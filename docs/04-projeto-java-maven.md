# Módulo 04 — Projeto Java com Maven

## Objetivo

Criar a aplicação base do laboratório: **Spring Boot 3 + Java 21 + Maven**, com endpoint de negócio, health check, **Dockerfile multi-stage** e manifests Kubernetes iniciais.

Ao final deste módulo:

| Item | Resultado |
| --- | --- |
| Projeto Maven | `app/` com Spring Boot |
| API | `GET /api/hello` |
| Health | `GET /actuator/health` |
| Testes | `mvn test` verdes |
| Container | imagem `labjavacicd:0.1.0` |
| Kubernetes | Deployment, Service, Ingress em `k8s/base` |

> Pré-requisitos: [Módulo 01](01-setup-ambiente.md) (JDK 21, Maven, Colima/Docker) e [Módulo 02](02-setup-cluster-kubernetes.md) (cluster `kind-lab`). O [Módulo 03](03-configuracao-github.md) é recomendado antes de versionar.

Cheatsheets: [Java](cheatsheets/java.md) · [Maven](cheatsheets/maven.md) · [Docker](cheatsheets/docker.md) · [kubectl](cheatsheets/kubectl.md)

---

## Visão da estrutura

```text
app/
├── pom.xml
├── Dockerfile
└── src/
    ├── main/
    │   ├── java/com/bittclouds/labjavacicd/
    │   │   ├── Application.java
    │   │   └── web/HelloController.java
    │   └── resources/application.yml
    └── test/java/com/bittclouds/labjavacicd/...

k8s/
├── base/
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── ingress.yaml
│   └── kustomization.yaml
└── overlays/dev/
    └── kustomization.yaml
```

Neste repositório os arquivos **já estão criados**. O módulo explica o que cada parte faz e como validar.

---

## 1. Conferir o toolchain

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"   # Intel: /usr/local/opt/openjdk@21
export PATH="$JAVA_HOME/bin:$PATH"

java -version
mvn -version
```

**Esperado:** Java **21** e Maven 3.9+ usando esse JDK.

---

## 2. O que o `pom.xml` define

Arquivo: `app/pom.xml`

Pontos importantes:

| Trecho | Função |
| --- | --- |
| `spring-boot-starter-parent` **3.5.x** | BOM e plugins do Spring Boot |
| `java.version` = **21** | bytecode e compiler |
| `spring-boot-starter-web` | API REST |
| `spring-boot-starter-actuator` | `/actuator/health` |
| `spring-boot-starter-test` | JUnit + MockMvc |
| `spring-boot-maven-plugin` | gera o JAR executável |

Coordenadas do artefato:

- `groupId`: `com.bittclouds`
- `artifactId`: `labjavacicd`
- `version`: `0.1.0-SNAPSHOT`

---

## 3. Código da aplicação

### 3.1 Bootstrap

`com.bittclouds.labjavacicd.Application` — classe com `@SpringBootApplication` e `main`.

### 3.2 Endpoint de negócio

`com.bittclouds.labjavacicd.web.HelloController`:

- `GET /api/hello`
- resposta JSON:

```json
{
  "message": "Hello from labjavacicd",
  "status": "ok"
}
```

### 3.3 Configuração

`src/main/resources/application.yml`:

- porta `8080`
- Actuator expondo `health` e `info`

### 3.4 Testes

- `ApplicationTest` — sobe o contexto Spring
- `HelloControllerTest` — valida `/api/hello` com MockMvc

---

## 4. Rodar localmente (sem Docker)

Na pasta `app/`:

```bash
cd app
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
export PATH="$JAVA_HOME/bin:$PATH"

mvn -B test
mvn -B spring-boot:run
```

Em outro terminal:

```bash
curl -sS http://localhost:8080/api/hello
curl -sS http://localhost:8080/actuator/health
```

**Esperado:**

- JSON com `message` / `status`
- health com `"status":"UP"`

Encerre a app com `Ctrl+C`.

---

## 5. Empacotar o JAR

```bash
cd app
mvn -B -DskipTests package
ls -la target/*.jar
```

O JAR executável fica em `target/labjavacicd-0.1.0-SNAPSHOT.jar` (nome derivado do `artifactId` + `version`).

Teste rápido:

```bash
java -jar target/labjavacicd-0.1.0-SNAPSHOT.jar
```

---

## 6. Dockerfile multi-stage

Arquivo: `app/Dockerfile`

| Stage | Imagem | Função |
| --- | --- | --- |
| `build` | `maven:3.9.11-eclipse-temurin-21-alpine` | `mvn package` |
| final | `eclipse-temurin:21-jre-alpine` | só o JRE + JAR |

Benefícios:

- imagem final menor (sem JDK/Maven)
- usuário não-root (`app`)
- mesma versão Java do lab (21)

### Build da imagem

Colima precisa estar no ar.

> **Atenção — tag obrigatória:** use sempre `-t labjavacicd:0.1.0`.  
> Sem `-t`, o Docker gera só um ID (ex.: `Successfully built 681d600bcdd8`) e **não** cria o nome `labjavacicd:0.1.0`.  
> O comando `kind load docker-image labjavacicd:0.1.0` falha com:
> `ERROR: image: "labjavacicd:0.1.0" not present locally`.

```bash
colima status
docker context use colima

cd app

# CORRETO — cria a imagem com nome e tag
docker build -t labjavacicd:0.1.0 .

# ERRADO — não use isto neste laboratório
# docker build .
```

Confirme que a tag existe **antes** de ir para o kind:

```bash
docker images | grep labjavacicd
```

**Esperado:** uma linha com `labjavacicd` e tag `0.1.0`. Se `grep` não retornar nada, a tag não foi aplicada — rode de novo o `docker build -t ...`.

### Rodar o container

```bash
docker run --rm -p 8080:8080 labjavacicd:0.1.0
```

Em outro terminal:

```bash
curl -sS http://localhost:8080/api/hello
curl -sS http://localhost:8080/actuator/health
```

---

## 7. Manifests Kubernetes

### 7.1 Base (`k8s/base`)

| Arquivo | Recurso |
| --- | --- |
| `deployment.yaml` | 1 réplica, probes em `/actuator/health`, resources |
| `service.yaml` | ClusterIP porta 80 → 8080 |
| `ingress.yaml` | paths `/api` e `/actuator` via Ingress NGINX |
| `kustomization.yaml` | agrupa os recursos no namespace `app` |

A imagem no Deployment é `labjavacicd:0.1.0` com `imagePullPolicy: IfNotPresent` — adequada para imagem carregada localmente no kind (sem registry ainda).

### 7.2 Overlay dev

`k8s/overlays/dev/kustomization.yaml` referencia a base e fixa a tag da imagem (preparação para GitOps no Módulo 06).

---

## 8. Deploy no kind (smoke no cluster)

### 8.1 Carregar a imagem no cluster

Pré-requisito: a imagem **já deve existir localmente com a tag** `labjavacicd:0.1.0` (passo do `docker build -t` acima).

```bash
# 1) Confirme a tag no Docker (mesmo contexto do Colima)
docker context use colima
docker images | grep labjavacicd

# 2) Só então carregue no kind
kubectl config use-context kind-lab
kind load docker-image labjavacicd:0.1.0 --name lab
```

O kind **não** enxerga automaticamente o que você buildou no host: o `kind load` copia a imagem (pelo **nome:tag**) para dentro do node do cluster.

Se aparecer `ERROR: image: "labjavacicd:0.1.0" not present locally`, volte ao build com `-t` — a imagem ainda não está tagueada.

### 8.2 Aplicar manifests

```bash
# na raiz do repositório
kubectl apply -k k8s/overlays/dev

kubectl get deploy,pod,svc,ingress -n app
kubectl wait --namespace app \
  --for=condition=available deploy/labjavacicd \
  --timeout=180s
```

### 8.3 Validar o HTTP

Com o Ingress NGINX do Módulo 02 e o mapeamento `hostPort: 8080` do kind, o `curl` em `http://localhost:8080` costuma funcionar **sem** port-forward:

```bash
curl -sS http://localhost:8080/api/hello
curl -sS http://localhost:8080/actuator/health
```

**Esperado:** JSON do hello e health `UP`.

Isso funciona porque o tráfego vai:

`localhost:8080` → node kind (porta 80) → Ingress NGINX → Service `labjavacicd` → Pod

#### Se o Ingress não responder — use port-forward

```bash
kubectl port-forward -n app svc/labjavacicd 18080:80
```

Em outro terminal:

```bash
curl -sS http://localhost:18080/api/hello
curl -sS http://localhost:18080/actuator/health
```

> Use `18080` no forward para não conflitar com a porta `8080` já usada pelo Ingress.

### 8.4 Logs

`kubectl logs` também **não** precisa de port-forward (lê a saída do container via API do cluster):

```bash
kubectl logs -n app deploy/labjavacicd -f
```

---

## 9. Como recriar o projeto do zero *(referência)*

Se estiver em outro diretório vazio, o equivalente via [Spring Initializr](https://start.spring.io):

```bash
curl -sL "https://start.spring.io/starter.zip" \
  -d type=maven-project \
  -d language=java \
  -d bootVersion=3.5.16 \
  -d baseDir=app \
  -d groupId=com.bittclouds \
  -d artifactId=labjavacicd \
  -d name=labjavacicd \
  -d packageName=com.bittclouds.labjavacicd \
  -d javaVersion=21 \
  -d dependencies=web,actuator \
  -o app.zip

unzip app.zip
```

Depois adicione o controller, testes, Dockerfile e manifests como neste repositório.

---

## 10. Checklist de verificação (obrigatório)

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
export PATH="$JAVA_HOME/bin:$PATH"

echo "=== Maven ==="
cd app && mvn -B test

echo "=== Estrutura ==="
cd ..
ls app/pom.xml app/Dockerfile
ls k8s/base/*.yaml k8s/overlays/dev/kustomization.yaml

echo "=== Cluster (se kind estiver no ar) ==="
kubectl config current-context
kubectl get deploy,pods -n app
```

### Critérios de “deu certo”

- [ ] `mvn test` passa
- [ ] `curl` local em `/api/hello` e `/actuator/health` funciona
- [ ] Imagem Docker `labjavacicd:0.1.0` existe
- [ ] Deployment no namespace `app` fica `Available`
- [ ] `curl` via port-forward (ou Ingress) retorna o JSON de hello

---

## 11. Problemas comuns

### `mvn` usa Java 11/25 em vez de 21

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
export PATH="$JAVA_HOME/bin:$PATH"
mvn -version
```

### Pod em `ImagePullBackOff` / `ErrImageNeverPull`

A imagem não está no node kind:

```bash
docker images | grep labjavacicd
kind load docker-image labjavacicd:0.1.0 --name lab
kubectl rollout restart deploy/labjavacicd -n app
```

### `kind load` → `ERROR: image: "labjavacicd:0.1.0" not present locally`

Significa que o Docker **não tem** uma imagem chamada exatamente `labjavacicd:0.1.0`.

Causa mais comum neste lab: ter rodado `docker build .` **sem** `-t`.

Diagnóstico:

```bash
docker context use colima
docker images | grep labjavacicd
```

- Se não listar `labjavacicd:0.1.0` → rebuild **com** a tag:

```bash
cd app
docker build -t labjavacicd:0.1.0 .
docker images | grep labjavacicd
kind load docker-image labjavacicd:0.1.0 --name lab
```

- Se a imagem aparecer no `docker images`, mas o kind ainda falhar → confira se `docker` e `kind` usam o mesmo engine (`docker context use colima`).

### Pod `CrashLoopBackOff`

```bash
kubectl describe pod -n app -l app.kubernetes.io/name=labjavacicd
kubectl logs -n app -l app.kubernetes.io/name=labjavacicd
```

### Build Docker lento na primeira vez

Normal: o stage Maven baixa dependências. O Dockerfile atual copia o `pom.xml` antes do `src/` (`dependency:go-offline`) para melhorar o cache de layers no build local e no GitHub Actions (Módulo 05).

---

## 12. Desafio opcional

1. Adicione `GET /api/hello/{name}` que devolva `"message": "Hello, {name}"`.
2. Escreva um teste MockMvc para o novo endpoint.
3. Suba a versão da imagem para `0.1.1`, faça rebuild, `kind load` e atualize `k8s/overlays/dev/kustomization.yaml`.

---

## Próximo passo

Com a app rodando no cluster, siga para o **Módulo 05 — GitHub Actions (CI)** ([05-github-actions.md](05-github-actions.md)).
