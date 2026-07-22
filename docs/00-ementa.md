# Laboratório Java CI/CD + Observabilidade no Kubernetes

## Visão geral

Este curso é um laboratório hands-on para construir, do zero, um ambiente completo de desenvolvimento e entrega de uma aplicação Java:

- cluster Kubernetes local
- repositório e fluxo de trabalho no GitHub
- aplicação Java com Maven
- pipeline de CI com GitHub Actions
- entrega contínua com GitOps (Argo CD)
- observabilidade com stack ELK
- evolução da aplicação com entregas incrementais

Ao final, você terá um ambiente reproduzível e documentado, pronto para servir tanto como laboratório pessoal quanto como base de material didático.

---

## Objetivos de aprendizagem

Ao concluir o curso, você será capaz de:

1. Preparar uma máquina de desenvolvimento com as ferramentas necessárias para o laboratório
2. Subir e operar um cluster Kubernetes local adequado a experimentos de CI/CD
3. Organizar um repositório GitHub com branches, proteção e boas práticas de colaboração
4. Criar e estruturar um projeto Java com Maven
5. Automatizar build, testes e publicação de imagem com GitHub Actions
6. Implantar a aplicação no Kubernetes usando GitOps com Argo CD
7. Instalar e configurar Elasticsearch, componentes de ingestão e Kibana
8. Instrumentar a aplicação para enviar logs (e correlacionar eventos) ao ELK
9. Entregar funcionalidades de ponta a ponta, do código ao cluster, com rastreabilidade

---

## Stack recomendada

| Camada | Tecnologia | Motivo da escolha |
| --- | --- | --- |
| Linguagem / build | Java 21 + Maven | padrão amplamente usado em empresas |
| Container | Docker | empacotamento da aplicação |
| Cluster local | kind ou k3d | leve, rápido e próximo de um cluster real |
| CI | GitHub Actions | nativo do GitHub, simples de manter no curso |
| CD / GitOps | Argo CD | encaixa melhor em Kubernetes do que Jenkins tradicional |
| Registro de imagens | GitHub Container Registry (GHCR) | integração direta com Actions |
| Observabilidade | Elasticsearch + Kibana + Fluent Bit | ELK moderno e adequado a Kubernetes |
| App de exemplo | Spring Boot (API REST) | ecossistema maduro e fácil de evoluir |

### Por que Argo CD em vez de Jenkins?

Jenkins continua relevante em muitos ambientes corporativos, mas neste laboratório o foco é **Kubernetes**. Argo CD:

- é nativo de cluster
- modela o estado desejado em Git (GitOps)
- reduz a complexidade de agents, plugins e jobs imperativos
- facilita demonstrar promoção entre ambientes (ex.: `dev` → `staging`)

> **Nota didática:** um módulo opcional no final do curso pode apresentar um pipeline Jenkins equivalente, para comparar o modelo imperativo (Jenkins) com o declarativo/GitOps (Argo CD).

---

## Público-alvo

- Desenvolvedores Java que querem aprender CI/CD e Kubernetes na prática
- DevOps / Platform Engineers iniciantes que precisam de um lab reproduzível
- Instrutores que desejam um roteiro modular para ministrar o conteúdo

### Pré-requisitos

- Noções básicas de linha de comando (terminal)
- Conceitos introdutórios de Git
- Familiaridade mínima com Java (sintaxe e pacotes)
- Conta no GitHub

Não é necessário conhecimento prévio avançado de Kubernetes, CI/CD ou ELK.

---

## Estrutura do curso

Cada módulo terá seu próprio arquivo Markdown em `docs/`, com passos numerados, comandos copiáveis e critérios de verificação (“como saber que deu certo”).

| Módulo | Arquivo (planejado) | Tema |
| --- | --- | --- |
| 00 | `docs/00-ementa.md` | Ementa e visão do curso |
| 01 | `docs/01-setup-ambiente.md` | Setup do ambiente local |
| 02 | `docs/02-setup-cluster-kubernetes.md` | Setup do cluster Kubernetes |
| 03 | `docs/03-configuracao-github.md` | Configuração do projeto no GitHub |
| 04 | `docs/04-projeto-java-maven.md` | Criação do projeto Java com Maven |
| 05 | `docs/05-github-actions.md` | Configuração do GitHub Actions (CI) |
| 06 | `docs/06-argocd-gitops.md` | Pipeline de entrega com Argo CD (CD) |
| 07 | `docs/07-stack-elk.md` | Configuração do ELK no cluster |
| 08 | `docs/08-app-integracao-elk.md` | Aplicação enviando logs ao ELK |
| 09 | `docs/09-entrega-funcionalidades.md` | Entrega de funcionalidades na aplicação |
| 10 | `docs/10-opcional-jenkins.md` | *(Opcional)* Comparativo com Jenkins |

---

## Ementa detalhada por módulo

### Módulo 01 — Setup do ambiente

**Objetivo:** deixar a máquina pronta para todo o laboratório.

**Conteúdo previsto:**

- Instalação e verificação de:
  - Git
  - JDK 21
  - Maven
  - Docker CLI + Colima (runtime de containers)
  - `kubectl`
  - kind **ou** k3d
  - Helm
- Configuração de variáveis e validação rápida (`java -version`, `docker info`, etc.)
- Organização sugerida de pastas do laboratório

**Resultado esperado:** todas as ferramentas instaladas e validadas com um checklist.

---

### Módulo 02 — Setup do cluster Kubernetes

**Objetivo:** criar um cluster local estável para CI/CD e observabilidade.

**Conteúdo previsto:**

- Criação do cluster (kind/k3d)
- Contextos do `kubectl`
- Namespaces do laboratório (`app`, `argocd`, `logging`, etc.)
- Ingress controller (quando necessário)
- Smoke test: deploy de um Pod/Service de exemplo

**Resultado esperado:** cluster saudável e namespaces prontos para os próximos módulos.

---

### Módulo 03 — Configuração do projeto no GitHub

**Objetivo:** preparar o repositório como fonte da verdade do curso e do GitOps.

**Conteúdo previsto:**

- Criação do repositório
- Estrutura inicial de pastas (`app/`, `k8s/`, `docs/`, `.github/`)
- Branch principal e fluxo de contribuição (`main` + PRs)
- Branch protection básica
- Secrets e permissões necessárias para Actions/GHCR
- README raiz apontando para a documentação

**Resultado esperado:** repositório organizado e pronto para receber código e manifests.

---

### Módulo 04 — Criação do projeto Java com Maven

**Objetivo:** gerar a aplicação base que será construída, publicada e observada.

**Conteúdo previsto:**

- Bootstrap do projeto Spring Boot + Maven
- Estrutura de pacotes
- Endpoint de health (`/actuator/health` ou equivalente)
- Endpoint de negócio inicial (ex.: `GET /api/hello`)
- Dockerfile multi-stage
- Manifests Kubernetes iniciais (Deployment, Service)

**Resultado esperado:** aplicação rodando localmente e como container, com manifests versionados.

---

### Módulo 05 — Configuração do GitHub Actions (CI)

**Objetivo:** automatizar qualidade e publicação de artefatos.

**Conteúdo previsto:**

- Workflow de CI em Pull Request:
  - checkout
  - setup JDK
  - `mvn test` / `mvn verify`
- Workflow de build e publish:
  - build da imagem Docker
  - push para GHCR
  - tag da imagem (commit SHA / semver simples)
- Badges e evidências de execução no GitHub

**Resultado esperado:** todo push/PR relevante dispara pipeline de CI e gera imagem versionada.

---

### Módulo 06 — Entrega contínua com Argo CD (CD / GitOps)

**Objetivo:** implantar e atualizar a aplicação no cluster a partir do Git.

**Conteúdo previsto:**

- Instalação do Argo CD no cluster
- Acesso à UI/CLI
- Application apontando para `k8s/` (ou pasta de overlays)
- Estratégia de atualização de imagem (commit no Git ou Image Updater)
- Sync automático vs manual
- Demonstração de rollback via Git

**Resultado esperado:** mudança aprovada no repositório reflete no cluster de forma rastreável.

---

### Módulo 07 — Configuração do ELK

**Objetivo:** disponibilizar a base de observabilidade no Kubernetes.

**Conteúdo previsto:**

- Namespace `logging`
- Elasticsearch (armazenamento e busca)
- Kibana (visualização)
- Fluent Bit como DaemonSet (coleta de logs de containers)
- Índices / data streams iniciais
- Acesso à UI do Kibana e consulta de smoke test

**Resultado esperado:** logs do cluster visíveis no Kibana.

> Neste laboratório usamos **Fluent Bit** no lugar de Logstash para coleta em nós Kubernetes (mais leve e idiomático). O “L” do ELK permanece representado pelo pipeline de ingestão/coleta.

---

### Módulo 08 — Aplicação integrada ao ELK

**Objetivo:** tornar logs da aplicação úteis para operação e troubleshooting.

**Conteúdo previsto:**

- Logging estruturado (JSON) na aplicação Java
- Correlation / request id em requests HTTP
- Labels e anotações Kubernetes para facilitar filtros no Kibana
- Dashboards e buscas salvas (erros 5xx, latência de endpoint, startup)
- Validação ponta a ponta: request → log → Kibana

**Resultado esperado:** é possível acompanhar o comportamento da app no ELK com filtros claros.

---

### Módulo 09 — Entrega de funcionalidades na aplicação

**Objetivo:** fechar o ciclo completo de engenharia: feature → CI → CD → observabilidade.

**Conteúdo previsto (sugestão de entregas):**

1. Endpoint de listagem (ex.: tarefas ou produtos)
2. Endpoint de criação com validação
3. Tratamento de erros padronizado
4. Métrica/log de negócio (ex.: contagem de operações)
5. Exercício final: corrigir um “bug” proposital usando Kibana + pipeline

Para cada funcionalidade:

- branch → PR → Actions verdes → sync Argo CD → verificação no cluster → verificação no Kibana

**Resultado esperado:** domínio prático do fluxo completo de entrega.

---

### Módulo 10 *(opcional)* — Comparativo com Jenkins

**Objetivo:** contrastar GitOps com pipeline imperativo clássico.

**Conteúdo previsto:**

- Jenkins no Kubernetes (controller + agent)
- Pipeline Declarative equivalente ao CI atual
- Pontos fortes/fracos vs GitHub Actions + Argo CD
- Quando faz sentido manter Jenkins em produção

---

## Dinâmica sugerida do curso

| Formato | Duração estimada |
| --- | --- |
| Autodidata (lab completo) | 16–24 horas |
| Turma presencial/online (com facilitador) | 3–4 encontros de 4h |
| Ritmo recomendado | 1 módulo por sessão + desafio curto |

Cada módulo deve terminar com:

1. **Checklist de verificação**
2. **Problemas comuns e soluções**
3. **Desafio opcional** para fixação

---

## Critérios de conclusão

O laboratório/curso é considerado concluído quando o aluno conseguir:

- [ ] Subir o cluster e listar nodes/pods com `kubectl`
- [ ] Ter o repositório GitHub com estrutura e proteção básica
- [ ] Rodar a API Java localmente e em container
- [ ] Ver o workflow de CI concluído com sucesso
- [ ] Ver a aplicação sincronizada pelo Argo CD no cluster
- [ ] Consultar logs da aplicação no Kibana
- [ ] Entregar ao menos duas funcionalidades seguindo o fluxo completo

---

## Organização dos artefatos (visão alvo do repositório)

```text
java-cicd-lab/
├── README.md
├── docs/
│   ├── 00-ementa.md
│   ├── 01-setup-ambiente.md
│   ├── 02-setup-cluster-kubernetes.md
│   ├── ...
│   └── 10-opcional-jenkins.md
├── app/                      # código Java (Maven)
├── k8s/
│   ├── base/                 # manifests base
│   └── overlays/             # ambientes (dev, etc.)
├── observability/            # valores/manifests ELK + Fluent Bit
└── .github/
    └── workflows/            # GitHub Actions
```

---

## Próximo passo

Siga para o **Módulo 01 — Setup do ambiente (macOS)** (`docs/01-setup-ambiente.md`).
```