# Módulo 03 — Configuração do projeto no GitHub

## Objetivo

Transformar o laboratório em um **repositório GitHub** organizado, com estrutura de pastas, branch principal, proteção básica e permissões para CI (Actions) e registro de imagens (GHCR).

Ao final deste módulo:

| Item | Resultado |
| --- | --- |
| Git local | repositório inicializado |
| Remoto | repositório no GitHub ligado ao `origin` |
| Estrutura | `docs/`, `app/`, `k8s/`, `observability/`, `.github/` |
| Branch | `main` como padrão + fluxo via Pull Request |
| Proteção | regras básicas em `main` |
| CI/CD prep | Actions e packages habilitados para os próximos módulos |

> Pré-requisitos: [Módulo 01](01-setup-ambiente.md) (Git + `gh`) e preferencialmente [Módulo 02](02-setup-cluster-kubernetes.md). Conta GitHub autenticada (`gh auth status`).

Cheatsheets: [Git](cheatsheets/git.md) · [GitHub CLI](cheatsheets/gh.md)

---

## Visão da estrutura alvo

```text
java-cicd-lab/
├── README.md                 # porta de entrada do curso/lab
├── .gitignore
├── docs/                     # módulos e cheatsheets
├── app/                      # código Java (Módulo 04)
├── k8s/
│   ├── cluster/              # kind, namespaces, smoke (já existe)
│   ├── base/                 # manifests da app (próximos módulos)
│   └── overlays/
├── observability/            # ELK / Fluent Bit (Módulo 07)
└── .github/
    └── workflows/            # GitHub Actions (Módulo 05)
```

O GitHub será a **fonte da verdade** do código e dos manifests (GitOps com Argo CD no Módulo 06).

---

## 1. Conferir autenticação no GitHub

```bash
gh auth status
git --version
```

**Esperado:** login ativo (ex.: `Logged in to github.com account <seu-user>`).

Se precisar autenticar:

```bash
gh auth login
```

Recomendações deste curso:

- GitHub.com
- SSH **ou** HTTPS (use o mesmo protocolo que você já configurou)
- Login via browser
- Escopos com acesso a `repo` (e, quando o fluxo oferecer, permissão para packages)

Confira o usuário:

```bash
gh api user --jq .login
```

Anote o login — ele aparece nas URLs (`https://github.com/<login>/java-cicd-lab`) e depois no GHCR (`ghcr.io/<login>/...`).

---

## 2. Inicializar o Git no diretório do lab

Na raiz do projeto (onde estão `docs/` e `k8s/`):

```bash
cd /caminho/para/java-cicd-lab

git init -b main
git status
```

**Esperado:** branch `main` e arquivos listados como untracked.

Se o Git já estiver inicializado, pule o `git init` e só confira:

```bash
git rev-parse --is-inside-work-tree
git branch --show-current
```

---

## 3. Garantir a estrutura de pastas

Crie o que ainda não existir:

```bash
mkdir -p app \
  k8s/base \
  k8s/overlays/dev \
  observability \
  .github/workflows

# placeholders para o Git versionar pastas vazias
touch app/.gitkeep \
  k8s/base/.gitkeep \
  k8s/overlays/dev/.gitkeep \
  observability/.gitkeep \
  .github/workflows/.gitkeep
```

Confira:

```bash
find . -type d \( -name .git -prune \) -o -type d -print | sort
```

---

## 4. Adicionar `.gitignore`

Arquivo na raiz: `.gitignore`

```gitignore
# Java / Maven
target/
*.class
*.jar
*.war
*.ear
.mvn/wrapper/maven-wrapper.jar
!.mvn/wrapper/maven-wrapper.jar

# IDE
.idea/
*.iml
.vscode/
*.swp
.DS_Store

# Ambiente / segredos
.env
.env.*
!.env.example
*.pem
credentials.json

# Logs e temporários
*.log
tmp/
.temp/
```

Isso evita commit de build, IDE e segredos.

---

## 5. Criar o `README.md` raiz

Arquivo na raiz: `README.md`

O README é a porta de entrada do repositório. Conteúdo mínimo sugerido:

```markdown
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
```

Ajuste caminhos se a pasta do lab tiver outro nome.

---

## 6. Primeiro commit local

```bash
git add .
git status
git commit -m "$(cat <<'EOF'
docs: bootstrap do laboratório Java CI/CD

Ementa, setup macOS/Kubernetes, cheatsheets e estrutura inicial do repositório.
EOF
)"

git log --oneline -n 3
```

**Esperado:** pelo menos um commit em `main`.

---

## 7. Criar o repositório no GitHub e publicar

### Opção A — via GitHub CLI (recomendado)

Ainda na raiz do lab:

```bash
# público (bom para material de curso)
gh repo create java-cicd-lab --public --source=. --remote=origin --push

# ou privado:
# gh repo create java-cicd-lab --private --source=. --remote=origin --push
```

Se o nome `java-cicd-lab` já existir na sua conta, escolha outro (ex.: `java-cicd-lab-curso`) e mantenha consistência nos próximos módulos.

### Opção B — criar no site e ligar o remoto

1. Em https://github.com/new, crie `java-cicd-lab` **sem** README/gitignore/licença (o local já tem conteúdo).
2. No terminal:

```bash
# SSH
git remote add origin git@github.com:<seu-user>/java-cicd-lab.git

# ou HTTPS
# git remote add origin https://github.com/<seu-user>/java-cicd-lab.git

git push -u origin main
```

Valide:

```bash
git remote -v
gh repo view --web
```

**Esperado:** repositório aberto no browser com o `README.md` renderizado.

---

## 8. Fluxo de contribuição (`main` + Pull Requests)

Neste curso:

1. `main` só recebe mudanças via PR (após a proteção da seção 9)
2. Trabalho diário em branches curtas
3. CI (Módulo 05) valida o PR antes do merge

### Criar uma branch de exemplo

```bash
git switch -c docs/ajuste-readme
# edite algo pequeno no README, se quiser
git add README.md
git commit -m "docs: pequeno ajuste no README"
git push -u origin HEAD
gh pr create --title "docs: ajuste no README" --body "PR de validação do fluxo do Módulo 03."
```

Abra o PR no browser, revise e faça merge (Merge commit ou Squash — escolha um padrão e mantenha).

```bash
gh pr list
gh pr merge <numero> --squash --delete-branch
git switch main
git pull
```

---

## 9. Proteção básica da branch `main`

Na UI do GitHub:

1. **Settings → Rules → Rulesets** (ou **Settings → Branches → Add branch protection rule**, conforme a UI da sua conta)
2. Crie regra para `main` com, no mínimo:
   - Require a pull request before merging
   - Require approvals: **0** ou **1** (0 é aceitável em lab solo; 1 é melhor para simular time)
   - Do not allow force pushes
   - Do not allow deletions

Via CLI (API), exemplo com ruleset moderno (ajuste se a API/conta exigir outro formato):

```bash
OWNER=$(gh api user --jq .login)
REPO=java-cicd-lab

gh api \
  --method POST \
  -H "Accept: application/vnd.github+json" \
  "/repos/${OWNER}/${REPO}/rulesets" \
  -f name='protect-main' \
  -f target='branch' \
  -f enforcement='active' \
  --raw-field 'conditions={"ref_name":{"include":["refs/heads/main"],"exclude":[]}}' \
  --raw-field 'rules=[{"type":"pull_request","parameters":{"required_approving_review_count":0,"dismiss_stale_reviews":true,"require_code_owner_review":false,"require_last_push_approval":false,"required_review_thread_resolution":false}},{"type":"non_fast_forward"}]'
```

> Em contas free de repositório **privado**, algumas proteções avançadas são limitadas. Em repositório **público**, o essencial costuma estar disponível. Se o comando da API falhar, configure pela UI.

### Como validar

Tente push direto em `main` após a regra (deve ser bloqueado ou exigir PR, conforme a configuração):

```bash
git switch main
# faça um commit local de teste e:
# git push origin main
```

O fluxo correto permanece: branch → PR → merge.

---

## 10. Permissões para GitHub Actions e GHCR

No Módulo 05 o CI vai buildar e publicar imagens em `ghcr.io`. Prepare agora:

### 10.1 Actions — leitura/escrita

1. **Settings → Actions → General**
2. Em **Workflow permissions**:
   - selecione **Read and write permissions**
   - marque **Allow GitHub Actions to create and approve pull requests** (opcional; útil depois)
3. Salve

### 10.2 Packages (GHCR)

1. **Settings → General → Features**: confirme que packages/ações relacionadas estão disponíveis na conta
2. Para imagens públicas no GHCR, no Módulo 05 usaremos `packages: write` no workflow
3. Visibilidade do package pode ser ajustada depois em **https://github.com/users/\<login\>/packages**

### 10.3 Secrets

Neste módulo **ainda não é obrigatório** criar secrets. Guarde estes nomes para o Módulo 05+:

| Secret | Quando | Uso |
| --- | --- | --- |
| `GITHUB_TOKEN` | automático | Actions já fornece; com permissão `packages: write` publica no GHCR |
| *(opcional)* token PAT | só se o `GITHUB_TOKEN` não bastar no seu setup | `write:packages` + `read:packages` |

Não commite tokens. Use apenas **Settings → Secrets and variables → Actions**.

---

## 11. Checklist de verificação (obrigatório)

```bash
echo "=== Git local ==="
git rev-parse --is-inside-work-tree
git branch --show-current
git log --oneline -n 3

echo "=== Remoto ==="
git remote -v
gh repo view --json nameWithOwner,url,visibility,defaultBranchRef --jq .

echo "=== Estrutura ==="
ls -la
ls -la app k8s observability .github/workflows docs
```

### Critérios de “deu certo”

- [ ] `git status` limpo após o push (ou só com mudanças locais conscientes)
- [ ] Repositório visível em `gh repo view --web`
- [ ] `README.md` aparece na home do GitHub
- [ ] Pastas `app/`, `k8s/`, `docs/`, `observability/`, `.github/workflows/` versionadas
- [ ] Branch padrão = `main`
- [ ] Proteção básica de `main` configurada (UI ou API)
- [ ] Workflow permissions com escrita habilitada para o Módulo 05

---

## 12. Problemas comuns

### `gh repo create` → name already exists

Escolha outro nome ou delete o repositório vazio antigo (cuidado: irreversível):

```bash
gh repo create java-cicd-lab-curso --public --source=. --remote=origin --push
```

### `remote origin already exists`

```bash
git remote -v
git remote remove origin
git remote add origin git@github.com:<seu-user>/java-cicd-lab.git
git push -u origin main
```

### Push rejeitado (auth)

```bash
gh auth status
# SSH:
ssh -T git@github.com
# HTTPS: prefira gh como credential helper
gh auth setup-git
```

### Arquivos sensíveis quase commitados

```bash
git status
# se ainda não commitou:
git reset HEAD <arquivo>
# garanta que está no .gitignore
```

Se **já** foi para o remoto, rotacione o segredo imediatamente — remover do histórico exige procedimento adicional (não coberto neste módulo).

---

## 13. Desafio opcional

1. Abra um PR que só adicione uma linha no `README.md` citando o seu usuário GitHub.
2. Faça merge via `gh pr merge`.
3. Em **Insights → Network** (ou histórico de commits), confirme que o fluxo PR → `main` ficou registrado.

---

## Próximo passo

Com o repositório no GitHub, siga para o **Módulo 04 — Criação do projeto Java com Maven** ([04-projeto-java-maven.md](04-projeto-java-maven.md)).
