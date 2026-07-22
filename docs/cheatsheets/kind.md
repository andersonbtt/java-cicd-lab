# Cheatsheet — kind

Cluster Kubernetes local (padrão do curso). Voltar ao [índice](README.md).

Documentação: https://kind.sigs.k8s.io

```bash
kind version
kind create cluster
kind create cluster --name lab
kind create cluster --name lab --config kind-config.yaml
kind get clusters
kind delete cluster --name lab

kind get kubeconfig --name lab
kind export kubeconfig --name lab

# Carregar imagem local no cluster (sem registry)
kind load docker-image <imagem>:<tag> --name lab
```

## Contexto kubectl

O contexto gerado pelo kind costuma ser `kind-<nome>`:

```bash
kubectl config use-context kind-lab
kubectl get nodes
```
