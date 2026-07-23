# Java CI/CD Lab

[![CI](https://github.com/<seu-usuario>/java-cicd-lab/actions/workflows/ci.yml/badge.svg)](https://github.com/<seu-usuario>/java-cicd-lab/actions/workflows/ci.yml)

Laboratório hands-on: aplicação Java, pipeline CI/CD e observabilidade (ELK) em Kubernetes.

## Documentação do curso

| Módulo | Documento |
| --- | --- |
| Ementa | [docs/00-ementa.md](docs/00-ementa.md) |
| 01 — Setup do ambiente (macOS) | [docs/01-setup-ambiente.md](docs/01-setup-ambiente.md) |
| 02 — Cluster Kubernetes | [docs/02-setup-cluster-kubernetes.md](docs/02-setup-cluster-kubernetes.md) |
| 03 — Projeto no GitHub | [docs/03-configuracao-github.md](docs/03-configuracao-github.md) |
| 04 — Projeto Java com Maven | [docs/04-projeto-java-maven.md](docs/04-projeto-java-maven.md) |
| 05 — GitHub Actions (CI) | [docs/05-github-actions.md](docs/05-github-actions.md) |
| 06 — Argo CD (GitOps) | [docs/06-argocd-gitops.md](docs/06-argocd-gitops.md) |
| 07 — Stack ELK | [docs/07-stack-elk.md](docs/07-stack-elk.md) |
| Fluxo CI/CD (conceitual) | [docs/fluxo-cicd-github.md](docs/fluxo-cicd-github.md) |
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
4. Crie/valide a app com o [Módulo 04](docs/04-projeto-java-maven.md)
5. Configure o CI com o [Módulo 05](docs/05-github-actions.md)
6. Configure o CD com o [Módulo 06](docs/06-argocd-gitops.md)
7. Suba o ELK com o [Módulo 07](docs/07-stack-elk.md)
