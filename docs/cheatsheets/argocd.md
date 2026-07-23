# Cheatsheet — Argo CD

GitOps continuous delivery. Voltar ao [índice](README.md).

Documentação: https://argo-cd.readthedocs.io  
Módulo do lab: [06-argocd-gitops.md](../06-argocd-gitops.md)

## UI (navegador)

```bash
kubectl port-forward -n argocd deploy/argocd-server 8081:8080 --address 127.0.0.1
# abra http://127.0.0.1:8081
```

Senha inicial:

```bash
kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath="{.data.password}" | base64 -d; echo
```

> No kind/Colima, **não** use `argocd login` contra esse port-forward (gRPC costuma derrubá-lo).

## CLI recomendada neste lab (`--core`)

Fala direto com o Kubernetes (sem API do argocd-server):

```bash
kubectl config set-context --current --namespace=argocd
argocd app list --core
argocd app get labjavacicd --core
argocd app sync labjavacicd --core
argocd app diff labjavacicd --core
kubectl config set-context --current --namespace=default
```

## Via kubectl (sempre funciona)

```bash
kubectl get applications -n argocd
kubectl get pods -n argocd
```
