# Módulo 06 — Argo CD (CD / GitOps)

## Objetivo

Implantar e atualizar a aplicação no Kubernetes a partir do **Git**, usando **Argo CD** (GitOps).

Ao final deste módulo:

| Item | Resultado |
| --- | --- |
| Argo CD | instalado no namespace `argocd` |
| UI / CLI | acesso ao servidor Argo CD |
| Application | `labjavacicd` sincronizando `k8s/overlays/dev` |
| Sync | automático (com self-heal) |
| Rollback | via `git revert` / commit anterior |

> Pré-requisitos: [Módulo 02](02-setup-cluster-kubernetes.md) (cluster `kind-lab`), [Módulo 03](03-configuracao-github.md) (repo no GitHub), [Módulo 04](04-projeto-java-maven.md) (manifests) e preferencialmente [Módulo 05](05-github-actions.md) (imagem no GHCR).

Conceito do fluxo: [fluxo-cicd-github.md](fluxo-cicd-github.md).

---

## Visão GitOps neste lab

```text
GitHub (main)
  └── k8s/overlays/dev   ← estado desejado (Deployment, tag da imagem, …)
           │
           ▼
       Argo CD (ns argocd)
           │  sync contínuo
           ▼
       Cluster kind (ns app)
           └── Pods labjavacicd
```

Diferença em relação a “Actions faz `kubectl apply`”:

| Modelo | Quem aplica no cluster |
| --- | --- |
| Imperativo | o job do CI/CD |
| **GitOps (este módulo)** | o Argo CD, olhando o Git |

O Actions continua responsável por **testar** e **publicar a imagem**. O Argo CD cuida de **fazer o cluster convergir** para o que está no Git.

---

## Arquivos deste módulo

```text
k8s/argocd/
├── values-lab.yaml      # Helm values (lab local)
└── application.yaml     # Application apontando para o overlay dev
```

---

## 1. Pré-checagens

```bash
kubectl config use-context kind-lab
kubectl get nodes
kubectl get ns app argocd
helm version
```

Instale a CLI do Argo CD (opcional, mas útil):

```bash
brew install argocd
argocd version --client
```

Garanta que o repositório **já está no GitHub** com a pasta `k8s/overlays/dev` na branch padrão (`main`). O Argo CD precisa clonar esse repo.

> Repo **público** simplifica o lab (sem credencial de Git).  
> Repo **privado** exige cadastrar credenciais no Argo CD (seção 10).

---

## 2. Instalar o Argo CD (Helm)

```bash
helm repo add argo https://argoproj.github.io/argo-helm
helm repo update

helm upgrade --install argocd argo/argo-cd \
  --namespace argocd \
  --create-namespace \
  --values k8s/argocd/values-lab.yaml \
  --wait
```

O `values-lab.yaml` habilita `server.insecure` **somente para laboratório** (facilita o port-forward sem TLS local). Não use isso em produção.

Aguarde os pods:

```bash
kubectl get pods -n argocd
kubectl wait --namespace argocd \
  --for=condition=available deploy/argocd-server \
  --timeout=300s
```

**Esperado:** pods do Argo CD `Running` / deployments disponíveis.

---

## 3. Acessar a UI

### 3.1 Senha inicial do admin

```bash
kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath="{.data.password}" | base64 -d
echo
```

Usuário: `admin`  
Senha: a string impressa acima.

### 3.2 Port-forward (UI no navegador)

No kind + Colima, o port-forward para o Argo CD é estável para **HTTP no browser**, mas costuma **quebrar** se a CLI `argocd login` tentar gRPC na mesma porta (`connection reset by peer` / `lost connection to pod`).

Use o port-forward **só para a UI**, apontando direto na porta do container (`8080`) e só em IPv4:

```bash
kubectl port-forward -n argocd deploy/argocd-server 8081:8080 --address 127.0.0.1
```

Abra: [http://127.0.0.1:8081](http://127.0.0.1:8081)

Login com `admin` + senha do secret.

> Deixe esse terminal aberto. **Não** rode `argocd login` contra `localhost:8081` neste lab — use a CLI em modo `--core` (abaixo).

### 3.3 CLI sem port-forward (`--core`) — recomendado neste lab

A CLI pode falar **direto com a API do Kubernetes** (sem passar pelo `argocd-server`):

```bash
kubectl config set-context --current --namespace=argocd
argocd app list --core
argocd app get labjavacicd --core
```

Para voltar o namespace padrão do kubectl:

```bash
kubectl config set-context --current --namespace=default
```

**Esperado:** app `labjavacicd` com `STATUS Synced` e `HEALTH Healthy`.

---

## 4. Criar a Application

Edite `k8s/argocd/application.yaml` e troque `<seu-usuario>` pelo owner real do GitHub **ou** aplique com `sed`:

```bash
export GITHUB_USER="$(gh api user --jq .login)"   # ou: export GITHUB_USER=seu-user

sed "s|<seu-usuario>|${GITHUB_USER}|g" k8s/argocd/application.yaml \
  | kubectl apply -f -
```

Confira:

```bash
kubectl get applications -n argocd
# NAME          SYNC STATUS   HEALTH STATUS
# labjavacicd   Synced        Healthy
```

Para usar a CLI `argocd app get ...` **neste lab**, prefira `--core` (sem port-forward):

```bash
kubectl config set-context --current --namespace=argocd
argocd app get labjavacicd --core
kubectl config set-context --current --namespace=default
```

Evite `argocd login localhost:8081` no kind/Colima: o gRPC da CLI costuma derrubar o port-forward (`connection reset by peer`). A UI no browser continua ok com:

```bash
kubectl port-forward -n argocd deploy/argocd-server 8081:8080 --address 127.0.0.1
```

Sem a CLI, `kubectl get applications -n argocd` também basta.

### O que a Application declara

| Campo | Valor neste lab |
| --- | --- |
| `source.repoURL` | `https://github.com/<seu-usuario>/java-cicd-lab.git` |
| `source.path` | `k8s/overlays/dev` |
| `source.targetRevision` | `HEAD` (branch padrão) |
| `destination.namespace` | `app` |
| `syncPolicy.automated` | sync + prune + selfHeal |

Com sync automático, o Argo CD aplica (e corrige) o cluster para bater com o Git.

---

## 5. Imagem: local (kind) vs GHCR

O overlay atual (`k8s/overlays/dev/kustomization.yaml`) usa:

```yaml
images:
  - name: labjavacicd
    newName: labjavacicd
    newTag: 0.1.0
```

Isso funciona com `imagePullPolicy: IfNotPresent` **se** a imagem foi carregada no kind (`kind load docker-image ...`) — fluxo do Módulo 04.

### Opção A — continuar com imagem local (rápido no lab)

1. Mantenha `labjavacicd:0.1.0` no overlay  
2. Sempre que rebuildar: `kind load docker-image labjavacicd:0.1.0 --name lab`  
3. Force restart se necessário: `kubectl rollout restart deploy/labjavacicd -n app`

O Argo CD sincroniza os manifests; a imagem continua vinda do node local.

### Opção B — apontar para o GHCR (fluxo CI → CD completo)

Depois do Módulo 05 ter publicado a imagem:

```yaml
images:
  - name: labjavacicd
    newName: ghcr.io/<seu-usuario-minusculo>/labjavacicd
    newTag: latest   # ou sha-xxxxxxx (preferível)
```

Commit + push desse overlay. O Argo CD sincroniza e o kubelet faz pull do GHCR.

> Package **público** no GHCR evita `imagePullSecrets`.  
> Package privado: crie secret e referencie no Deployment (seção 10).

---

## 6. Validar o sync

```bash
kubectl get applications -n argocd
# STATUS deve ir para Synced / Healthy

kubectl get deploy,pod,svc,ingress -n app
curl -sS http://localhost:8080/api/hello
```

Na UI do Argo CD, o app deve aparecer **Synced** e **Healthy** (ícone verde).

Se ficar **Unknown** / **ComparisonError**:

- URL do repo errada ou repo privado sem credencial  
- path `k8s/overlays/dev` inexistente na branch  
- veja: `argocd app get labjavacicd` ou eventos na UI

---

## 7. Ciclo completo: mudar → Git → cluster

### 7.1 Alterar o estado desejado

Exemplo: mudar réplicas no base ou via patch no overlay.

Crie `k8s/overlays/dev/replicas.yaml` (opcional):

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: labjavacicd
spec:
  replicas: 2
```

E no `kustomization.yaml` do overlay:

```yaml
resources:
  - ../../base
  - replicas.yaml   # se usar o arquivo acima

images:
  - name: labjavacicd
    newName: labjavacicd
    newTag: 0.1.0
```

### 7.2 Publicar no Git

```bash
git switch -c gitops/scale-replicas
git add k8s/overlays/dev
git commit -m "gitops: sobe labjavacicd para 2 replicas"
git push -u origin HEAD
# merge via PR (recomendado) ou push em main conforme o fluxo do curso
```

### 7.3 Observar o Argo CD

Com sync automático, em segundos/minutos:

```bash
kubectl get pods -n app -w
```

Ou clique **Refresh** na UI. Self-heal: se alguém der `kubectl scale` manual, o Argo CD **reverte** para o que está no Git.

---

## 8. Sync manual vs automático

Este lab usa **automático** (`automated` + `selfHeal`).

| Modo | Comportamento |
| --- | --- |
| Manual | você clica Sync / `argocd app sync labjavacicd` |
| Automático | qualquer drift ou novo commit dispara sync |
| Self-heal | corrige mudanças manuais no cluster |

Para forçar um sync:

```bash
argocd app sync labjavacicd
# ou
kubectl -n argocd patch application labjavacicd \
  --type merge \
  -p '{"operation":{"initiatedBy":{"username":"admin"},"sync":{"revision":"HEAD"}}}'
```

---

## 9. Rollback via Git

1. Identifique o commit bom: `git log --oneline k8s/overlays/dev`  
2. Reverta ou volte o arquivo da tag/réplicas  
3. Push em `main`  
4. Argo CD sincroniza o estado anterior  

```bash
git revert <commit-sha> --no-edit
git push origin main
```

Alternativa na UI: **History → Rollback** (também válido; o ideal didático do curso é o rollback pelo Git).

---

## 10. Problemas comuns

### Application `ComparisonError` / não clona o repo

- Confira `repoURL` e se o repo é acessível  
- Público? Teste no browser o HTTPS do GitHub  
- Privado? **Settings** no Argo CD → **Repositories** → Add repo (HTTPS + PAT, ou SSH)

Exemplo PAT (repo privado), via CLI:

```bash
argocd repo add https://github.com/<seu-usuario>/java-cicd-lab.git \
  --username <seu-usuario> \
  --password <GITHUB_PAT>
```

O PAT precisa de escopo `repo` (clássico) ou permissões equivalentes fine-grained.

### Pod `ImagePullBackOff` após apontar para GHCR

- Tag/nome da imagem errados  
- Package privado sem `imagePullSecret`  
- Confirme: `docker pull ghcr.io/<owner>/labjavacicd:<tag>` na sua máquina

### Sync OK, mas app antiga

- Overlay ainda com tag antiga  
- Imagem local: esqueceu o `kind load` após rebuild  
- `imagePullPolicy: IfNotPresent` não puxa de novo a mesma tag — mude a tag ou delete o pod

### UI / port-forward cai com `connection reset by peer`

No kind + Colima isso é comum quando a **CLI** usa gRPC no mesmo port-forward.

- **UI:** `kubectl port-forward -n argocd deploy/argocd-server 8081:8080 --address 127.0.0.1` e abra `http://127.0.0.1:8081` (só browser)
- **CLI:** use `argocd app ... --core` com namespace `argocd` (não use `argocd login` local)
- **Sem CLI:** `kubectl get applications -n argocd`

### Namespace `argocd` já existia vazio

O Helm com `--create-namespace` e `--namespace argocd` reutiliza o namespace criado no Módulo 02 — ok.

---

## 11. Atualização de imagem: estratégias

| Estratégia | Como | Quando usar no curso |
| --- | --- | --- |
| **Commit no overlay** | alterar `newTag` no `kustomization.yaml` e push | padrão (simples e rastreável) |
| Argo CD Image Updater | controller atualiza a tag sozinho | opcional / avançado |
| Editar só no cluster | `kubectl set image` | **evitar** (self-heal desfaz) |

Fluxo recomendado pós-Módulo 05:

1. Actions publica `ghcr.io/.../labjavacicd:sha-abc1234`  
2. PR atualiza `newTag: sha-abc1234` no overlay  
3. Merge → Argo CD sync → novos Pods  

---

## 12. Checklist de verificação (obrigatório)

- [ ] `helm upgrade --install argocd ...` concluiu com sucesso  
- [ ] UI abre em `http://localhost:8081` com login `admin`  
- [ ] Application `labjavacicd` existe e está **Synced/Healthy**  
- [ ] `kubectl get pods -n app` mostra a aplicação  
- [ ] `curl http://localhost:8080/api/hello` responde  
- [ ] Uma mudança no Git (ex.: réplicas) refletiu no cluster sem `kubectl apply` manual  

```bash
kubectl get applications -n argocd
kubectl get pods -n argocd
kubectl get pods -n app
curl -sS http://localhost:8080/api/hello
```

---

## 13. Desafio opcional

1. Desligue o sync automático, faça uma mudança no Git e sync **manual** pela UI.  
2. Altere um label no cluster com `kubectl`; observe o self-heal reverter.  
3. Aponte o overlay para uma tag `sha-...` do GHCR e documente o commit de GitOps.

---

## Próximo passo

Com o CD GitOps funcionando, siga para o **Módulo 07 — Stack ELK** ([07-stack-elk.md](07-stack-elk.md)).
