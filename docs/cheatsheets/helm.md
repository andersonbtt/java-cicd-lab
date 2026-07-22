# Cheatsheet — Helm

Gerenciador de pacotes para Kubernetes. Voltar ao [índice](README.md).

Documentação: https://helm.sh/docs/

```bash
helm version
helm repo add <nome> <url>
helm repo update
helm repo list
helm search repo <termo>

helm show values <chart>
helm install <release> <chart> -n <ns> --create-namespace
helm install <release> <chart> -n <ns> -f values.yaml
helm upgrade <release> <chart> -n <ns> -f values.yaml
helm upgrade --install <release> <chart> -n <ns> -f values.yaml
helm list -n <ns>
helm list -A
helm status <release> -n <ns>
helm rollback <release> <revisao> -n <ns>
helm uninstall <release> -n <ns>

helm template <release> <chart> -f values.yaml   # renderiza YAML
helm lint <chart>
helm history <release> -n <ns>
```

## Exemplo típico do curso

```bash
helm repo add argo https://argoproj.github.io/argo-helm
helm repo update
helm upgrade --install argocd argo/argo-cd -n argocd --create-namespace
```
