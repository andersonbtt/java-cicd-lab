# Módulo 05 — GitHub Actions (CI)

## Objetivo

Automatizar **testes** (CI) e **publicação da imagem Docker** no GitHub Container Registry (GHCR) a cada mudança relevante na aplicação.

Ao final deste módulo:

| Item | Resultado |
| --- | --- |
| Workflow CI | `mvn verify` em PR e push em `main` |
| Workflow Publish | imagem em `ghcr.io/<owner>/labjavacicd` |
| Tags | `sha-<short>` e `latest` (na `main`) |
| Badge | status do CI no `README.md` |

> Pré-requisitos: [Módulo 03](03-configuracao-github.md) (repo no GitHub + permissões de Actions) e [Módulo 04](04-projeto-java-maven.md) (app Maven + Dockerfile).

> Hands-on deste módulo: abaixo. Visão conceitual do fluxo completo: [fluxo-cicd-github.md](fluxo-cicd-github.md).

Cheatsheet: [GitHub CLI](cheatsheets/gh.md)

---

## Visão do pipeline

```text
Pull Request (app/**) ──► CI: checkout → JDK 21 → mvn verify
                                              │
Push em main (app/**) ─┬──────────────────────┘
                       └──► Publish: build Docker → push GHCR
                              tags: sha-<commit> + latest
```

Arquivos neste repositório:

```text
.github/workflows/
├── ci.yml         # testes Maven
└── publish.yml    # build + push da imagem
```

---

## O que você precisa fazer para “criar” o CI?

**Não existe um passo separado do tipo “criar pipeline” na interface.**  
O GitHub Actions funciona assim:

1. Você coloca arquivos YAML em `.github/workflows/`
2. Faz **commit + push** para o GitHub (o que está só na sua máquina **não** cria o fluxo)
3. O GitHub **detecta sozinho** esses arquivos e passa a usá-los como workflows

A partir daí, o fluxo **roda automaticamente** quando acontece um evento que o YAML declara em `on:` (por exemplo push/PR que altera `app/**`).

| Situação | O CI existe? | O CI roda? |
| --- | --- | --- |
| Arquivos só no disco local | Não (GitHub ainda não vê) | Não |
| Push dos YAMLs para o repositório remoto | Sim — aparecem em **Actions** | Só se o evento/gatilho bater |
| Push/PR mudando `app/**` (com workflows já no remoto) | Sim | Sim (CI; publish na `main`) |
| Mudança só em `docs/**` | Sim | Não (path filter não inclui docs) |

### Checklist mínimo

1. `git push` dos arquivos `.github/workflows/*.yml` (e do restante do lab) para o remoto  
2. Abrir o repo no GitHub → aba **Actions** → deve listar **CI** e **Publish image**  
3. (Recomendado) **Settings → Actions → General** → workflow permissions com escrita (para o publish no GHCR)  
4. Disparar um evento: push/PR em `app/**`, ou **Run workflow** no publish (`workflow_dispatch`)

> Resumo: **atualizar o repositório remoto com `.github/workflows` já “cria” o fluxo.**  
> Você não cadastra o CI em outro lugar; no máximo ajusta permissões e espera (ou provoca) um gatilho para ver a primeira execução.

---

## 1. Permissões no GitHub (revisão rápida)

Em **Settings → Actions → General**:

1. **Workflow permissions** → **Read and write permissions**
2. Salve

Isso permite que o `GITHUB_TOKEN` publique pacotes no GHCR (`packages: write` no workflow).

Opcional, mas útil: em **Settings → Actions → General**, confirme que Actions estão habilitadas para o repositório.

---

## 2. Workflow de CI — `ci.yml`

Arquivo: `.github/workflows/ci.yml`

### O que ele faz

| Gatilho | Quando |
| --- | --- |
| `pull_request` | mudanças em `app/**` ou no próprio workflow |
| `push` em `main` | idem |

| Step | Função |
| --- | --- |
| `actions/checkout` | clona o repo |
| `actions/setup-java` | Temurin 21 + cache do Maven |
| `mvn -B verify` | compila + testes (+ checks do ciclo `verify`) |

O `working-directory: app` faz o Maven rodar dentro de `app/`.

### Trecho essencial

```yaml
- name: Set up JDK 21
  uses: actions/setup-java@v4
  with:
    distribution: temurin
    java-version: "21"
    cache: maven
    cache-dependency-path: app/pom.xml

- name: Run tests
  run: mvn -B verify
```

---

## 3. Workflow de publish — `publish.yml`

Arquivo: `.github/workflows/publish.yml`

### O que ele faz

| Gatilho | Quando |
| --- | --- |
| `push` em `main` | mudanças em `app/**` ou no workflow |
| `workflow_dispatch` | execução manual na UI do Actions |

| Step | Função |
| --- | --- |
| Buildx | builder moderno do Docker |
| Login GHCR | autentica com `GITHUB_TOKEN` |
| metadata-action | gera tags (`sha-...`, `latest`) |
| build-push-action | build do `app/Dockerfile` e push |

### Imagem publicada

```text
ghcr.io/<seu-user-em-minusculas>/labjavacicd:sha-<7chars>
ghcr.io/<seu-user-em-minusculas>/labjavacicd:latest
```

> O GHCR exige owner em **minúsculas**. O workflow normaliza com `${GITHUB_REPOSITORY_OWNER,,}`.

### Permissões do job

```yaml
permissions:
  contents: read
  packages: write
```

---

## 4. Dockerfile otimizado para CI

No Módulo 05 o `app/Dockerfile` passa a baixar dependências **antes** de copiar o código-fonte:

```dockerfile
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package ...
```

Assim, mudanças só em `src/` reaproveitam a layer de dependências (localmente e no cache do Actions via `cache-from/to: type=gha`).

---

## 5. Badge no README

No topo do `README.md` (ajuste o owner/repo se necessário):

```markdown
[![CI](https://github.com/<seu-usuario>/java-cicd-lab/actions/workflows/ci.yml/badge.svg)](https://github.com/<seu-usuario>/java-cicd-lab/actions/workflows/ci.yml)
```

Substitua `<seu-usuario>` pelo owner do repositório (ex.: o login do GitHub).

---

## 6. Como validar (passo a passo)

### 6.1 Commit e push dos workflows

Na raiz do repositório:

```bash
git status
git add .github/workflows/ci.yml .github/workflows/publish.yml app/Dockerfile README.md docs/05-github-actions.md
git commit -m "$(cat <<'EOF'
ci: adiciona GitHub Actions para testes e publish no GHCR

EOF
)"
git push origin main
```

> Se a branch `main` estiver protegida, use uma branch + PR (recomendado pelo Módulo 03).

### 6.2 Acompanhar execuções

```bash
gh run list --limit 10
gh run watch
```

Ou abra: **Actions** no GitHub.

**Esperado:**

- job **CI / Maven verify** verde
- job **Publish image** verde (no push em `main` que toque `app/**`)

### 6.3 Confirmar a imagem no GHCR

```bash
gh api user/packages?package_type=container --jq '.[].name'
```

Ou no browser: `https://github.com/<owner>?tab=packages`

Para puxar a imagem (se o package estiver público ou você estiver autenticado):

```bash
echo "$GITHUB_TOKEN" | docker login ghcr.io -u USERNAME --password-stdin
# ou:
gh auth token | docker login ghcr.io -u $(gh api user --jq .login) --password-stdin

docker pull ghcr.io/<owner-minusculo>/labjavacicd:latest
```

### 6.4 Testar o CI via Pull Request

```bash
git switch -c ci/smoke-pr
# pequena mudança só para disparar o path filter, ex.: comentário em um teste
git add -A && git commit -m "ci: smoke PR" && git push -u origin HEAD
gh pr create --title "ci: smoke" --body "Valida workflow CI no PR."
gh run list --branch ci/smoke-pr
```

---

## 7. Tornar o package público *(opcional)*

Por padrão o package no GHCR pode nascer **privado**.

1. Abra o package em GitHub → **Package settings**
2. **Change visibility** → Public (se fizer sentido para o curso)

Sem isso, `docker pull` anônimo falha; com login funciona.

---

## 8. Usar a imagem do GHCR no kind *(adianto do Módulo 06)*

Enquanto o Argo CD não estiver configurado, você já pode apontar o overlay para a imagem publicada:

```bash
# exemplo — troque owner e tag
cd k8s/overlays/dev
# edite kustomization.yaml:
# images:
#   - name: labjavacicd
#     newName: ghcr.io/<owner>/labjavacicd
#     newTag: sha-xxxxxxx
```

Se o package for privado, o cluster precisará de `imagePullSecrets` (detalharemos no GitOps). Para o lab, package **público** simplifica.

---

## 9. Checklist de verificação (obrigatório)

- [ ] `.github/workflows/ci.yml` e `publish.yml` versionados
- [ ] Actions com permissão de escrita habilitada no repo
- [ ] Run de CI verde (`mvn verify`)
- [ ] Run de Publish verde
- [ ] Package `labjavacicd` visível no GHCR
- [ ] Badge de CI no README (opcional, recomendado)

```bash
gh run list --workflow ci.yml --limit 3
gh run list --workflow publish.yml --limit 3
```

---

## 10. Problemas comuns

### CI não dispara

- O push/PR **não** alterou `app/**` nem o arquivo do workflow (path filters).
- Actions desabilitadas no repositório.

### Publish falha com `denied` / `403` no GHCR

- Workflow permissions ainda em “Read repository contents”.
- Falta `packages: write` no job.
- Owner da imagem com letras maiúsculas (o workflow já lowercasa).

### `dependency:go-offline` falha no Docker build

Em alguns POMs o plugin reclama de artefatos opcionais. Se acontecer no CI:

```dockerfile
RUN mvn -B -q -DskipTests dependency:resolve || true
```

No material deste lab o `go-offline` é o caminho padrão; ajuste só se necessário.

### Badge quebrado

Confira `owner/repo` e o nome do arquivo (`ci.yml`) na URL do badge.

### Duas execuções no mesmo push

Push em `main` que muda `app/` dispara **CI** e **Publish** — esperado. O CI valida testes; o Publish gera a imagem.

---

## 11. Desafio opcional

1. Adicione um job no `ci.yml` que rode `mvn -B -DskipTests package` e faça upload do JAR como artifact (`actions/upload-artifact`).
2. Restrinja o `publish.yml` para só rodar se o CI do mesmo commit passou (`workflow_run` ou um único workflow com `needs:`).

---

## Próximo passo

Com imagens versionadas no GHCR, siga para o **Módulo 06 — Argo CD / GitOps** ([06-argocd-gitops.md](06-argocd-gitops.md)).
