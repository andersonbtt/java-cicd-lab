# Cheatsheet — GitHub CLI (`gh`)

GitHub no terminal. Voltar ao [índice](README.md).

Documentação: https://cli.github.com/manual/

```bash
gh --version
gh auth login
gh auth status
gh auth logout

# Repositório
gh repo create <nome> --private --source=. --remote=origin --push
gh repo view --web
gh repo clone <owner>/<repo>

# Pull requests
gh pr create --title "titulo" --body "descricao"
gh pr list
gh pr view <numero>
gh pr view <numero> --web
gh pr checkout <numero>
gh pr status
gh pr merge <numero>

# Actions / checks
gh run list
gh run view <id>
gh run watch
gh workflow list

# Issues (se usar)
gh issue list
gh issue create --title "titulo" --body "descricao"
```

## Autenticação de registry (GHCR)

Quando necessário:

```bash
echo "$GITHUB_TOKEN" | docker login ghcr.io -u USERNAME --password-stdin
# ou via gh:
gh auth token
```
