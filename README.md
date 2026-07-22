# Java CI/CD Lab

Laboratório hands-on: aplicação Java, pipeline CI/CD e observabilidade (ELK) em Kubernetes.

## Documentação do curso

| Módulo | Documento |
| --- | --- |
| Ementa | [docs/00-ementa.md](docs/00-ementa.md) |
| 01 — Setup do ambiente (macOS) | [docs/01-setup-ambiente.md](docs/01-setup-ambiente.md) |
| 02 — Cluster Kubernetes | [docs/02-setup-cluster-kubernetes.md](docs/02-setup-cluster-kubernetes.md) |
| 03 — Projeto no GitHub | [docs/03-configuracao-github.md](docs/03-configuracao-github.md) |
| Cheatsheets | [docs/cheatsheets/README.md](docs/cheatsheets/README.md) |

## Stack

- Java 21 + Maven + Spring Boot
- Kubernetes local (kind) + Colima
- GitHub Actions (CI) + Argo CD (CD / GitOps)
- Elasticsearch + Kibana + Fluent Bit

## Como começar

1. Siga o [Módulo 01](docs/01-setup-ambiente.md)
2. Suba o cluster com o [Módulo 02](docs/02-setup-cluster-kubernetes.md)
3. Configure este repositório com o [Módulo 03](docs/03-configuracao-github.md)
