# Cheatsheet — k3d

Alternativa de cluster Kubernetes local (baseado em k3s). Voltar ao [índice](README.md).

Documentação: https://k3d.io

```bash
k3d version
k3d cluster create lab
k3d cluster create lab --agents 1 -p "8080:80@loadbalancer"
k3d cluster list
k3d cluster stop lab
k3d cluster start lab
k3d cluster delete lab

k3d image import <imagem>:<tag> -c lab
k3d kubeconfig merge lab --kubeconfig-merge-default
```

## Contexto kubectl

```bash
kubectl config use-context k3d-lab
kubectl get nodes
```
