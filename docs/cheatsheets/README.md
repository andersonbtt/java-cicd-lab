# Cheatsheets — Ferramentas do laboratório

Referência rápida por ferramenta. Complementa o [Módulo 01 — Setup do ambiente](../01-setup-ambiente.md).

| Ferramenta | Arquivo |
| --- | --- |
| Homebrew | [homebrew.md](homebrew.md) |
| Git | [git.md](git.md) |
| JDK / Java | [java.md](java.md) |
| Maven | [maven.md](maven.md) |
| Colima | [colima.md](colima.md) |
| Docker CLI | [docker.md](docker.md) |
| kubectl | [kubectl.md](kubectl.md) |
| kind | [kind.md](kind.md) |
| k3d | [k3d.md](k3d.md) |
| Helm | [helm.md](helm.md) |
| GitHub CLI (`gh`) | [gh.md](gh.md) |
| Argo CD | [argocd.md](argocd.md) |
| ELK + Fluent Bit | [elk.md](elk.md) |

## Fluxo mínimo “lab acordou”

Use após reiniciar o Mac:

```bash
colima start
docker context use colima
docker info >/dev/null && echo "Docker OK"

# se o cluster kind já existir:
kubectl config use-context kind-lab   # ajuste o nome
kubectl get nodes
```
