# Fluxo CI/CD com GitHub — visão conceitual

Este documento explica **como funciona** um pipeline CI/CD centrado no GitHub, etapa por etapa. O exemplo concreto deste laboratório usa:

| Papel | Ferramenta |
| --- | --- |
| Código-fonte | Repositório GitHub |
| CI (build/test) | GitHub Actions |
| Artefato de runtime | Imagem Docker no GHCR |
| CD (implantação) | GitOps com Argo CD no Kubernetes *(Módulo 06)* |

> Hands-on correspondente: [05-github-actions.md](05-github-actions.md) (CI) e, em seguida, Argo CD (CD).

---

## CI vs CD em uma frase

| Sigla | Significado | Pergunta que responde |
| --- | --- | --- |
| **CI** | Continuous Integration | “Este commit está íntegro? Compila e passa nos testes?” |
| **CD** | Continuous Delivery / Deployment | “Como esse artefato aprovado chega ao ambiente (cluster)?” |

No GitHub, o CI costuma ser **Actions**. O CD pode ser:

- o próprio Actions fazendo `kubectl apply` / Helm, **ou**
- (neste lab) o Actions só **publica a imagem** e o **Argo CD** aplica o estado declarado no Git (GitOps).

---

## Panorama do fluxo completo

```text
  Desenvolvedor
       │  push / Pull Request
       ▼
  GitHub (código + workflows)
       │
       ├─────────────── CI (Actions) ────────────────┐
       │  1. Checkout do código                      │
       │  2. Download de dependências (Maven)        │
       │  3. Compilação                              │
       │  4. Testes                                  │
       │  5. Empacotamento (JAR)                     │
       │  6. Geração da imagem Docker                │
       │  7. Publicação no GHCR                      │
       └─────────────────────────────────────────────┘
                         │
                         ▼
              Registro de imagens (GHCR)
                         │
       ┌───────────────── CD (GitOps) ───────────────┐
       │  8. Manifests no Git apontam a nova tag     │
       │  9. Argo CD sincroniza o cluster            │
       │ 10. Kubernetes sobe/atualiza os Pods        │
       └─────────────────────────────────────────────┘
                         │
                         ▼
              Aplicação rodando no cluster
```

Cada etapa abaixo detalha **o quê**, **por quê** e **onde aparece neste lab**.

---

## 1. Disparo — o que inicia o pipeline

Tudo começa com um evento no GitHub:

| Evento | Uso típico |
| --- | --- |
| `pull_request` | validar qualidade **antes** de entrar em `main` |
| `push` em `main` | publicar artefato “oficial” |
| `workflow_dispatch` | rodar na mão (debug / republish) |

O arquivo YAML em `.github/workflows/` declara esses gatilhos (`on:`).

**Importante:** não há botão “criar CI”. Bastam os YAMLs **commitados e enviados (`git push`) ao GitHub**. O GitHub descobre os workflows sozinho; eles só **executam** quando o evento do `on:` ocorre (detalhes no [Módulo 05](05-github-actions.md#o-que-você-precisa-fazer-para-criar-o-ci)).

Neste lab:

- **CI** (`ci.yml`) → PR e push em `main` quando muda `app/**`
- **Publish** (`publish.yml`) → push em `main` (e execução manual)

---

## 2. Checkout — obter o código

O runner do Actions (máquina virtual temporária) precisa do código do commit que disparou o workflow.

```text
actions/checkout → pasta de trabalho com o repositório na revisão correta
```

Sem checkout, não há `pom.xml`, `src/` nem `Dockerfile`.

---

## 3. Download das dependências

Projetos Java quase nunca são autossuficientes: frameworks (Spring), bibliotecas de teste, plugins Maven etc. vêm do **Maven Central** (ou de outros repositórios).

### O que acontece

1. O Maven lê o `pom.xml`
2. Resolve a árvore de dependências
3. Baixa JARs (e POMs) para o cache local (`~/.m2/repository` no runner)
4. Reutiliza cache entre builds quando possível (`actions/setup-java` com `cache: maven`, ou layers Docker)

### Por que importa

- É a etapa mais **lenta** na primeira execução
- Falhas de rede/repositório quebram o pipeline cedo (melhor do que falhar depois do deploy)
- Cache bom = feedback mais rápido no PR

### Neste lab

| Onde | Como |
| --- | --- |
| Job CI | `setup-java` + `mvn verify` (Maven baixa deps sob demanda) |
| Build da imagem | `mvn dependency:go-offline` no Dockerfile, **antes** de copiar `src/` |

---

## 4. Compilação

O compilador Java (`javac`, via plugin Maven) transforma `.java` em `.class` (bytecode), na versão definida no projeto (**Java 21**).

```text
src/main/java/**/*.java  →  target/classes/**/*.class
```

### Por que importa

- Erros de sintaxe, tipos e APIs incompatíveis são pegos **antes** dos testes
- Garante que o código “fecha” com as dependências baixadas

### Neste lab

Faz parte de `mvn verify` / `mvn package` (fases `compile` / `test-compile`).

---

## 5. Testes

Com o código compilado, o Maven executa os testes automatizados (JUnit + Spring MockMvc neste lab).

```text
src/test/java/**  →  Surefire  →  sucesso ou falha do job
```

### Por que importa

- É a barreira de qualidade da **integração contínua**
- PR vermelho = não misturar código quebrado em `main`
- Testes rápidos e confiáveis tornam o CI útil no dia a dia

### Neste lab

```bash
mvn -B verify
```

Cobre, entre outros, `GET /api/hello` e o carregamento do contexto Spring.

> Se os testes falham, o pipeline **para**. Não se publica imagem “oficial” a partir de um fluxo saudável de CD (no lab, CI e publish são workflows separados; o desafio opcional do Módulo 05 é encadeá-los com `needs:`).

---

## 6. Empacotamento

Depois dos testes, o build gera o **artefato** da aplicação.

No Spring Boot + Maven:

```text
classes + dependências  →  labjavacicd-0.1.0-SNAPSHOT.jar  (JAR executável)
```

O `spring-boot-maven-plugin` “reempacota” o JAR para rodar com `java -jar`.

### Por que importa

- O JAR é a unidade que a **imagem Docker** (ou um servidor) de fato executa
- Versionar o artefato (ou a imagem que o contém) permite rastrear *o que* foi para produção

### Neste lab

- No CI: `verify` já passa por `package` (sem falhar nos testes)
- No Docker: `mvn -DskipTests package` no stage de build (testes já rodaram no job CI)

---

## 7. Geração da imagem Docker

Empacotar em container padroniza o runtime: mesma JRE, mesmo entrypoint, em qualquer ambiente.

### Multi-stage (ideia)

```text
Stage build (JDK + Maven)  →  gera o JAR
Stage final (só JRE)       →  copia o JAR, usuário não-root, EXPOSE 8080
```

Resultado: imagem menor e sem ferramentas de build em produção.

### Por que importa

- Kubernetes agenda **imagens**, não JARs soltos
- Tag da imagem (`sha-abc1234`, `0.1.0`, `latest`) é o elo entre CI e CD

### Neste lab

- Local: `docker build -t labjavacicd:0.1.0 ./app`
- Actions: `docker/build-push-action` com contexto `./app`

---

## 8. Publicação no registro (GHCR)

A imagem precisa ficar em um **registry** acessível pelo cluster (e por humanos).

```text
Runner Actions  →  docker push  →  ghcr.io/<owner>/labjavacicd:<tag>
```

Tags típicas:

| Tag | Uso |
| --- | --- |
| `sha-<commit>` | imutável, rastreável até o commit |
| `latest` | conveniência na branch principal (evitar como única referência em produção) |

### Por que importa

- Sem registry, cada ambiente teria que buildar a mesma coisa de novo
- O CD só “puxa” o que já foi construído e (em tese) testado

### Neste lab

Workflow `publish.yml` → GitHub Container Registry (`ghcr.io`).

---

## 9. Implantação (CD)

Aqui o artefato aprovado vira **carga de trabalho** no Kubernetes.

### Dois modelos comuns

**A) Imperativo (Actions aplica direto)**

```text
Actions → kubectl/helm apply → cluster
```

Simples, mas o “estado desejado” pode ficar só no job, não no Git.

**B) GitOps (usado neste laboratório)**

```text
Git (manifests k8s/ + tag da imagem)  ←  fonte da verdade
         │
         ▼
      Argo CD observa o Git e sincroniza o cluster
```

Fluxo mental:

1. CI valida o código
2. Publish gera `ghcr.io/.../labjavacicd:sha-...`
3. Alguém (ou um bot) atualiza `k8s/overlays/dev` com a nova tag
4. Argo CD detecta o drift e faz sync
5. O Deployment puxa a nova imagem; Pods sobem; probes (`/actuator/health`) confirmam saúde

### Por que importa

- Rollback = voltar o Git (ou a tag anterior)
- Auditoria: quem mudou o que fica no histórico
- O cluster converge para o que está declarado, não para um script esquecido

---

## 10. Como as peças deste lab se encaixam

| Etapa | Comando / peça | Onde vive |
| --- | --- | --- |
| Dependências | Maven / `dependency:go-offline` | `pom.xml`, Dockerfile |
| Compilação | `mvn compile` (via `verify`/`package`) | Actions CI + stage Docker |
| Testes | `mvn verify` | `.github/workflows/ci.yml` |
| Empacotamento | JAR Spring Boot | `target/*.jar` |
| Imagem | `docker build` / build-push-action | `app/Dockerfile`, `publish.yml` |
| Registro | GHCR | `ghcr.io/<owner>/labjavacicd` |
| Implantação | manifests + Argo CD | `k8s/`, Módulo 06 |

---

## 11. Critérios de “pipeline saudável”

Um fluxo CI/CD com GitHub está coerente quando:

1. **Todo PR relevante** roda testes automaticamente  
2. **Falha de teste** impede merge (proteção de branch + checks)  
3. **Todo artefato em `main`** tem imagem versionada no registry  
4. **O cluster** só recebe imagens que passaram pelo caminho acordado  
5. **É possível** responder: “este Pod veio de qual commit?”

---

## 12. Analogia rápida

Pense numa linha de montagem:

| Etapa industrial | Etapa CI/CD |
| --- | --- |
| Receber matéria-prima | Checkout + dependências |
| Usinar peças | Compilação |
| Controle de qualidade | Testes |
| Embalar o produto | JAR + imagem Docker |
| Estoque | GHCR |
| Entrega na loja | Deploy no Kubernetes (Argo CD) |

Se o controle de qualidade falha, a caixa **não** vai para a prateleira.

---

## Próximos documentos

- Prática de CI: [05-github-actions.md](05-github-actions.md)
- Prática de CD / GitOps: [06-argocd-gitops.md](06-argocd-gitops.md)
- Visão do curso: [00-ementa.md](00-ementa.md)
