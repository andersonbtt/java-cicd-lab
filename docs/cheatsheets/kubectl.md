# Cheatsheet — kubectl

Cliente oficial do Kubernetes. Voltar ao [índice](README.md).

Documentação: https://kubernetes.io/docs/reference/kubectl/

```bash
kubectl version --client
kubectl cluster-info
kubectl config get-contexts
kubectl config current-context
kubectl config use-context <nome>
kubectl config view --minify

# Namespaces e recursos
kubectl get ns
kubectl get nodes
kubectl get pods -A
kubectl get pods -n <ns>
kubectl get deploy,svc,ingress -n <ns>
kubectl describe pod <pod> -n <ns>
kubectl logs <pod> -n <ns>
kubectl logs -f <pod> -n <ns>
kubectl logs -f deploy/<nome> -n <ns>

# Aplicar / apagar
kubectl apply -f manifesto.yaml
kubectl apply -k overlays/dev        # Kustomize
kubectl delete -f manifesto.yaml
kubectl delete pod <pod> -n <ns>

# Debug
kubectl get events -n <ns> --sort-by=.lastTimestamp
kubectl top nodes                    # se metrics-server existir
kubectl top pods -n <ns>
kubectl port-forward svc/<svc> 8080:80 -n <ns>
kubectl exec -it <pod> -n <ns> -- sh
```

## Atalhos comuns

```bash
alias k=kubectl
export do='--dry-run=client -o yaml'
kubectl create deployment demo --image=nginx $do
```
