# Módulo 02 — Setup do cluster Kubernetes

## Objetivo

Criar e validar um cluster Kubernetes **local** com **kind**, rodando sobre o Docker Engine do **Colima**, com namespaces do laboratório, Ingress Controller e um smoke test.

Ao final deste módulo você terá:

| Item | Valor esperado |
| --- | --- |
| Cluster | `lab` (kind) |
| Contexto kubectl | `kind-lab` |
| Namespaces | `app`, `argocd`, `logging` |
| Ingress | `ingress-nginx` instalado |
| Smoke test | Deployment + Service acessíveis |

> Pré-requisito: [Módulo 01 — Setup do ambiente](01-setup-ambiente.md) concluído (Colima, Docker, kubectl, kind, Helm).

Cheatsheets úteis: [Colima](cheatsheets/colima.md) · [Docker](cheatsheets/docker.md) · [kind](cheatsheets/kind.md) · [kubectl](cheatsheets/kubectl.md) · [Helm](cheatsheets/helm.md)

---

## Visão do que vamos montar

```text
macOS
  └── Colima (VM)
        └── Docker Engine
              └── kind node (container)
                    └── Kubernetes
                          ├── ns app        → aplicação Java (módulos seguintes)
                          ├── ns argocd     → GitOps (módulo 06)
                          └── ns logging    → ELK + Fluent Bit (módulo 07)
```

O curso usa **kind** como padrão. No final há um apêndice opcional com **k3d**.

---

## 1. Garantir que o Colima e o Docker estão no ar

```bash
colima status
docker context use colima
docker info >/dev/null && echo "Docker OK"
kind version
kubectl version --client
```

Se o Colima estiver parado:

```bash
colima start --cpu 4 --memory 8 --disk 40
docker context use colima
```

**Esperado:** `colima status` = Running; `docker info` sem erro.

> Sem Colima/Docker ativos, o `kind create cluster` falha.

---

## 2. Arquivo de configuração do kind

Crie (ou use) o arquivo versionado no repositório:

`k8s/cluster/kind-config.yaml`

```yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
name: lab
nodes:
  - role: control-plane
    kubeadmConfigPatches:
      - |
        kind: InitConfiguration
        nodeRegistration:
          kubeletExtraArgs:
            node-labels: "ingress-ready=true"
    extraPortMappings:
      - containerPort: 80
        hostPort: 8080
        protocol: TCP
      - containerPort: 443
        hostPort: 8443
        protocol: TCP
```

### Por que essa configuração?

| Escolha | Motivo |
| --- | --- |
| Um único node (`control-plane`) | Menos RAM — importante com ELK depois |
| Label `ingress-ready=true` | Compatível com o Ingress NGINX do kind |
| `hostPort` 8080/8443 (não 80/443) | Evita conflito de privilégio/porta no macOS; acesso via `http://localhost:8080` |

Se a máquina tiver **16 GB+** de RAM e você quiser simular worker separado:

```yaml
nodes:
  - role: control-plane
    # ... mesmo kubeadmConfigPatches e extraPortMappings ...
  - role: worker
```

Para o curso, o cluster de **um node** é suficiente.

---

## 3. Criar o cluster

A partir da raiz do repositório:

```bash
cd /caminho/para/java-cicd-lab

kind create cluster --name lab --config k8s/cluster/kind-config.yaml
```

A primeira criação baixa imagens do node — pode levar alguns minutos.

Valide:

```bash
kind get clusters
kubectl config get-contexts
kubectl config use-context kind-lab
kubectl cluster-info
kubectl get nodes -o wide
```

**Esperado:**

- cluster `lab` listado
- contexto atual `kind-lab`
- node `Ready`
- `kubectl cluster-info` mostra o Kubernetes control plane

### Se o cluster `lab` já existir

```bash
kind get clusters
# para recriar do zero:
kind delete cluster --name lab
kind create cluster --name lab --config k8s/cluster/kind-config.yaml
```

---

## 4. Criar os namespaces do laboratório

Arquivo: `k8s/cluster/namespaces.yaml`

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: app
  labels:
    lab.project: java-cicd-lab
    lab.purpose: application
---
apiVersion: v1
kind: Namespace
metadata:
  name: argocd
  labels:
    lab.project: java-cicd-lab
    lab.purpose: gitops
---
apiVersion: v1
kind: Namespace
metadata:
  name: logging
  labels:
    lab.project: java-cicd-lab
    lab.purpose: observability
```

Aplique:

```bash
kubectl apply -f k8s/cluster/namespaces.yaml
kubectl get ns
```

**Esperado:** `app`, `argocd` e `logging` presentes (além de `default`, `kube-system`, etc.).

---

## 5. Instalar o Ingress Controller

Usamos o manifesto oficial do kind para Ingress NGINX:

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
```

Aguarde o controller ficar pronto:

```bash
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=180s

kubectl get pods -n ingress-nginx
```

**Esperado:** pod do controller `Running` / `Ready`.

> Em redes lentas o pull da imagem pode estourar o timeout. Rode o `kubectl wait` de novo ou acompanhe com `kubectl get pods -n ingress-nginx -w`.

---

## 6. Smoke test — aplicação de exemplo

Vamos publicar um NGINX simples no namespace `app`, expor via Service + Ingress e validar o acesso.

### 6.1 Manifesto de smoke test

Arquivo: `k8s/cluster/smoke-test.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: smoke-web
  namespace: app
  labels:
    app: smoke-web
spec:
  replicas: 1
  selector:
    matchLabels:
      app: smoke-web
  template:
    metadata:
      labels:
        app: smoke-web
    spec:
      containers:
        - name: nginx
          image: nginx:1.27-alpine
          ports:
            - containerPort: 80
---
apiVersion: v1
kind: Service
metadata:
  name: smoke-web
  namespace: app
spec:
  selector:
    app: smoke-web
  ports:
    - name: http
      port: 80
      targetPort: 80
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: smoke-web
  namespace: app
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  ingressClassName: nginx
  rules:
    - http:
        paths:
          - path: /smoke
            pathType: Prefix
            backend:
              service:
                name: smoke-web
                port:
                  number: 80
```

Aplique e acompanhe:

```bash
kubectl apply -f k8s/cluster/smoke-test.yaml
kubectl get deploy,pod,svc,ingress -n app
kubectl wait --namespace app \
  --for=condition=available deploy/smoke-web \
  --timeout=120s
```

### 6.2 Validar acesso

Com o mapeamento do kind (`hostPort: 8080`):

```bash
curl -sS -o /dev/null -w "%{http_code}\n" http://localhost:8080/smoke/
```

**Esperado:** código HTTP `200` (a página padrão do NGINX).

Se preferir ver o HTML:

```bash
curl -sS http://localhost:8080/smoke/ | head
```

### 6.3 Alternativa — port-forward

Se `localhost:8080` não responder (comum em alguns setups Colima/rede), use port-forward:

```bash
kubectl port-forward -n app svc/smoke-web 18080:80
```

Em outro terminal:

```bash
curl -sS -o /dev/null -w "%{http_code}\n" http://localhost:18080/
```

**Esperado:** `200`.

> Port-forward é suficiente para desenvolver. O Ingress continua importante para o modelo “como em produção”, que usaremos com a app Java depois.

---

## 7. Checklist de verificação (obrigatório)

```bash
echo "=== Colima / Docker ==="
colima status
docker context show

echo "=== Cluster ==="
kind get clusters
kubectl config current-context
kubectl get nodes

echo "=== Namespaces ==="
kubectl get ns app argocd logging

echo "=== Ingress ==="
kubectl get pods -n ingress-nginx

echo "=== Smoke ==="
kubectl get deploy,svc,ingress -n app
curl -sS -o /dev/null -w "HTTP %{http_code}\n" http://localhost:8080/smoke/ \
  || echo "localhost:8080 falhou — use port-forward (seção 6.3)"
```

### Critérios de “deu certo”

- [ ] Contexto atual é `kind-lab`
- [ ] Node(s) em `Ready`
- [ ] Namespaces `app`, `argocd`, `logging` existem
- [ ] Pod do Ingress NGINX está `Running`
- [ ] Smoke test responde `200` via Ingress **ou** via port-forward

---

## 8. Operação do dia a dia

| Tarefa | Comando |
| --- | --- |
| Ver clusters kind | `kind get clusters` |
| Trocar contexto | `kubectl config use-context kind-lab` |
| Ver tudo no `app` | `kubectl get all -n app` |
| Parar Colima (libera RAM) | `colima stop` — o cluster kind sobe de novo após `colima start` |
| Apagar só o smoke test | `kubectl delete -f k8s/cluster/smoke-test.yaml` |
| Apagar o cluster inteiro | `kind delete cluster --name lab` |

### Após reiniciar o Mac

```bash
colima start
docker context use colima
kubectl config use-context kind-lab
kubectl get nodes
```

Os nodes kind são containers Docker: com o Colima de volta, em geral o cluster `lab` reaparece sem recriar. Se os nodes não voltarem `Ready`, recrie o cluster (seção 3).

---

## 9. Problemas comuns

### `ERROR: failed to create cluster: ... Cannot connect to the Docker daemon`

Colima/Docker fora do ar ou contexto errado:

```bash
colima start
docker context use colima
docker info
```

### Node fica `NotReady` ou cria muito devagar

- Aguarde o pull de imagens
- Confira recursos do Colima (`colima status`); se preciso: `colima stop` e `colima start --cpu 4 --memory 8 --disk 40`
- Verifique: `kubectl describe node` e `kubectl get events -A --sort-by=.lastTimestamp`

### Ingress instalado, mas `curl localhost:8080` falha

1. Confirme o mapping: `docker ps` e procure portas `8080->80` / `8443->443` no node kind  
2. Confirme o controller: `kubectl get pods -n ingress-nginx`  
3. Use port-forward (seção 6.3) e siga o curso  
4. Em último caso, delete e recrie o cluster com o `kind-config.yaml` correto

### `kind create cluster` diz que o nome já existe

```bash
kind delete cluster --name lab
kind create cluster --name lab --config k8s/cluster/kind-config.yaml
```

### Contexto kubectl errado

```bash
kubectl config get-contexts
kubectl config use-context kind-lab
```

---

## 10. Limpeza do smoke test (opcional)

O NGINX de exemplo pode sair agora ou ficar até a app Java ocupar o namespace `app`:

```bash
kubectl delete -f k8s/cluster/smoke-test.yaml
```

**Não apague** ainda os namespaces `app`, `argocd` e `logging` — eles serão usados nos módulos seguintes.

---

## 11. Desafio opcional

1. Escalone o smoke test para 2 réplicas e observe os pods:

```bash
kubectl scale deploy/smoke-web -n app --replicas=2
kubectl get pods -n app -w
```

2. Crie um segundo Ingress path (`/smoke-alt`) apontando para o mesmo Service e valide com `curl`.
3. Compare `kubectl describe ingress smoke-web -n app` antes e depois da mudança.

---

## Apêndice — Alternativa com k3d

Só use se quiser comparar. O restante do curso assume **kind**.

```bash
k3d cluster create lab \
  --agents 0 \
  --port "8080:80@loadbalancer" \
  --port "8443:443@loadbalancer"

kubectl config use-context k3d-lab
kubectl apply -f k8s/cluster/namespaces.yaml
kubectl get nodes
```

Para remover:

```bash
k3d cluster delete lab
```

> k3d já embute Traefik como Ingress em muitos setups. Se misturar kind e k3d, preste atenção ao **contexto** do kubectl.

---

## Próximo passo

Com o cluster saudável, siga para o **Módulo 03 — Configuração do projeto no GitHub** ([03-configuracao-github.md](03-configuracao-github.md)).
