# Cheatsheet — Git

Versionamento. Voltar ao [índice](README.md).

Documentação: https://git-scm.com/doc

```bash
git --version
git config --global user.name "Seu Nome"
git config --global user.email "seu-email@exemplo.com"
git config --global init.defaultBranch main
git config --global --list

git init
git clone <url>
git status
git add <arquivo|pasta|.>
git commit -m "mensagem"
git log --oneline -n 10
git diff
git diff --staged

git branch
git branch <nome>
git checkout <nome>
git switch <nome>
git switch -c <nome>           # cria e troca
git merge <branch>

git remote -v
git fetch
git pull
git push
git push -u origin HEAD        # publica branch atual

git stash
git stash pop
git stash list
```

## Fluxo curto do dia a dia

```bash
git switch -c feature/minha-tarefa
# ... editar arquivos ...
git add .
git commit -m "feat: descreve a mudança"
git push -u origin HEAD
```
