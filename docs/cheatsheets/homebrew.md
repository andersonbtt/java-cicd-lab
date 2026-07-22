# Cheatsheet — Homebrew

Gerenciador de pacotes no macOS. Voltar ao [índice](README.md).

Documentação: https://docs.brew.sh

```bash
brew --version                 # versão
brew update                    # atualiza índices
brew upgrade                   # atualiza pacotes instalados
brew install <formula>         # instala CLI/lib
brew install --cask <app>      # instala aplicativo GUI
brew uninstall <formula>       # remove
brew reinstall <formula>       # reinstala
brew list                      # listar instalados
brew search <termo>            # buscar
brew info <formula>            # detalhes
brew outdated                  # o que tem update
brew cleanup                   # limpar versões antigas
brew doctor                    # diagnóstico
brew services list             # serviços gerenciados pelo brew
```

## Apple Silicon vs Intel (prefixo)

```bash
# Apple Silicon
eval "$(/opt/homebrew/bin/brew shellenv)"

# Intel
eval "$(/usr/local/bin/brew shellenv)"
```
