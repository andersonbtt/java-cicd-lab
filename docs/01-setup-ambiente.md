# Módulo 01 — Setup do ambiente (macOS)

## Objetivo

Deixar o macOS pronto para todo o laboratório: desenvolvimento Java, containers, Kubernetes local, GitOps e observabilidade.

> Referência rápida depois da instalação: [Cheatsheets das ferramentas](cheatsheets/README.md).

Ao final deste módulo, as ferramentas abaixo devem estar instaladas e validadas:

| Ferramenta | Uso no laboratório |
| --- | --- |
| Git | versionamento e fluxo com GitHub |
| Homebrew | instalação das demais ferramentas |
| JDK 21 | compilação e execução Java |
| Maven | build do projeto |
| Docker CLI + Colima | imagens e runtime do cluster local |
| kubectl | operação do Kubernetes |
| kind **ou** k3d | cluster Kubernetes local |
| Helm | instalação de Argo CD e stack ELK |
| GitHub CLI (`gh`) *(opcional, recomendado)* | repositório, PRs e autenticação |

---

## Pré-requisitos deste módulo

- macOS Sequoia / Sonoma / Ventura (ou mais recente)
- Acesso de administrador (para instalar apps e o Homebrew)
- Conexão com a internet
- Conta no [GitHub](https://github.com)
- **Apple Silicon (M1/M2/M3/M4)** ou **Intel** — os comandos cobrem os dois; diferenças aparecem quando necessário

### Recursos de hardware recomendados

| Recurso | Mínimo | Recomendado |
| --- | --- | --- |
| RAM | 8 GB | 16 GB ou mais |
| Disco livre | 20 GB | 40 GB ou mais |
| CPU | 4 cores | 8 cores |

> O stack ELK (Elasticsearch + Kibana) é o componente que mais consome memória. Com 8 GB o laboratório funciona, mas pode ficar apertado.

---

## 1. Identificar a arquitetura do Mac

Abra o **Terminal** (`Aplicativos → Utilitários → Terminal` ou Spotlight: `Terminal`) e execute:

```bash
uname -m
sw_vers
```

Interpretação de `uname -m`:

| Saída | Arquitetura | Prefixo típico do Homebrew |
| --- | --- | --- |
| `arm64` | Apple Silicon | `/opt/homebrew` |
| `x86_64` | Intel | `/usr/local` |

Guarde essa informação: ela aparece em vários passos de PATH e instalação.

---

## 2. Instalar o Xcode Command Line Tools

O Git da Apple e compiladores básicos vêm com as Command Line Tools:

```bash
xcode-select --install
```

Se já estiver instalado, o sistema informa isso. Confirme:

```bash
xcode-select -p
git --version
```

**Esperado:** um caminho como `/Library/Developer/CommandLineTools` e uma versão do Git.

---

## 3. Instalar o Homebrew

Se o Homebrew ainda não existir:

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

Ao terminar, o instalador mostra comandos para colocar o `brew` no `PATH`. Em Apple Silicon isso costuma ser:

```bash
echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
eval "$(/opt/homebrew/bin/brew shellenv)"
```

Em Intel, o prefixo costuma ser `/usr/local`.

Valide:

```bash
brew --version
brew update
```

---

## 4. Instalar JDK 21

O laboratório usa **Java 21** (LTS).

```bash
brew install openjdk@21
```

Em seguida, registre o JDK para o macOS e configure o shell.

### Apple Silicon (`arm64`)

```bash
sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk \
  /Library/Java/JavaVirtualMachines/openjdk-21.jdk

echo 'export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"' >> ~/.zshrc
echo 'export JAVA_HOME="/opt/homebrew/opt/openjdk@21"' >> ~/.zshrc
source ~/.zshrc
```

### Intel (`x86_64`)

```bash
sudo ln -sfn /usr/local/opt/openjdk@21/libexec/openjdk.jdk \
  /Library/Java/JavaVirtualMachines/openjdk-21.jdk

echo 'export PATH="/usr/local/opt/openjdk@21/bin:$PATH"' >> ~/.zshrc
echo 'export JAVA_HOME="/usr/local/opt/openjdk@21"' >> ~/.zshrc
source ~/.zshrc
```

Valide:

```bash
java -version
echo "$JAVA_HOME"
/usr/libexec/java_home -V
```

**Esperado:** `openjdk version "21.x.x"`.

> Se `java -version` ainda mostrar Java 11 (ou outra versão), o `PATH` antigo está vencendo. Feche e reabra o Terminal, ou confira a ordem das linhas em `~/.zshrc`.

---

## 5. Instalar Maven

```bash
brew install maven
mvn -version
```

**Esperado:** Maven 3.9+ e a linha `Java version` apontando para **21**.

Se o Maven listar outro JDK, force o mesmo `JAVA_HOME` do passo anterior e abra um novo terminal.

---

## 6. Instalar Docker com Colima

O cluster local (kind/k3d) e o build de imagens dependem de um runtime de containers. Neste curso usamos **Colima** (VM leve) + **Docker CLI**, sem Docker Desktop.

### 6.1 Instalar pacotes

```bash
brew install docker docker-compose docker-credential-helper colima
```

### 6.2 Iniciar o Colima

Recursos sugeridos para o laboratório (ajuste se a máquina tiver menos RAM):

```bash
colima start --cpu 4 --memory 8 --disk 40
```

Na primeira execução o Colima baixa a imagem da VM — pode demorar alguns minutos.

Para ver o status:

```bash
colima status
```

**Esperado:** estado `Running`.

### 6.3 Garantir o contexto Docker

O Colima costuma criar e ativar o contexto `colima` automaticamente. Confirme:

```bash
docker context ls
docker context use colima
```

A coluna `CURRENT` deve marcar `colima` com `*`.

### 6.4 Validar o engine

```bash
docker version
docker info
docker run --rm hello-world
```

**Esperado:** client e server ativos; o container `hello-world` imprime mensagem de sucesso.

### 6.5 Comandos úteis no dia a dia

| Comando | Quando usar |
| --- | --- |
| `colima start` | após reiniciar o Mac ou parar o runtime |
| `colima stop` | liberar CPU/RAM quando não estiver no lab |
| `colima status` | conferir se a VM está no ar |
| `colima delete` | recriar do zero (apaga o disco da VM do Colima) |

> Se no futuro você instalar Docker Desktop na mesma máquina, não deixe os dois engines ativos no mesmo contexto. Para este curso, mantenha `docker context use colima`.

---

## 7. Instalar kubectl

```bash
brew install kubectl
kubectl version --client
```

**Esperado:** client em versão recente (1.28+). O server só aparecerá depois do Módulo 02, com o cluster no ar.

---

## 8. Instalar o runtime de cluster local

Escolha **uma** opção. Para o curso, a recomendação padrão é **kind** (Kubernetes IN Docker): documentação ampla e comportamento próximo de um cluster “vanilla”.

### Opção recomendada — kind

```bash
brew install kind
kind version
```

### Alternativa — k3d (Kubernetes leve baseado em k3s)

```bash
brew install k3d
k3d version
```

> Instale as duas ferramentas se quiser comparar depois, mas nos módulos seguintes usaremos **kind** nos exemplos, salvo indicação contrária.

---

## 9. Instalar Helm

```bash
brew install helm
helm version
```

**Esperado:** Helm v3.x.

---

## 10. Instalar GitHub CLI (opcional, recomendado)

Facilita login, criação de repositório e PRs nos módulos seguintes.

```bash
brew install gh
gh --version
gh auth login
```

No `gh auth login`, escolha:

- GitHub.com
- HTTPS
- autenticar via browser
- escopos padrão (e permissão de `write:packages` quando o fluxo pedir, para GHCR)

---

## 11. Configurar identidade do Git

Substitua pelos seus dados:

```bash
git config --global user.name "Seu Nome"
git config --global user.email "seu-email@exemplo.com"
git config --global init.defaultBranch main

git config --global --get user.name
git config --global --get user.email
```

Use o mesmo e-mail associado à conta GitHub (ou o e-mail `noreply` do GitHub).

---

## 12. Organização sugerida de pastas

```bash
mkdir -p ~/Code/labs
cd ~/Code/labs
```

Neste laboratório, o repositório do curso ficará em algo como:

```text
~/Code/labs/java-cicd-lab/
├── docs/            # documentação dos módulos
├── app/             # (ainda não existe) código Java
├── k8s/             # (ainda não existe) manifests
├── observability/   # (ainda não existe) ELK
└── .github/         # (ainda não existe) Actions
```

Se você já clonou ou está trabalhando neste repositório, use o diretório atual — não é obrigatório mover nada.

---

## 13. Checklist de verificação (obrigatório)

Execute o bloco abaixo. Todos os comandos devem retornar sucesso e versões coerentes com a tabela.

```bash
echo "=== macOS / arch ==="
sw_vers
uname -m

echo "=== Git ==="
git --version

echo "=== Homebrew ==="
brew --version

echo "=== Java ==="
java -version
echo "JAVA_HOME=$JAVA_HOME"

echo "=== Maven ==="
mvn -version

echo "=== Docker / Colima ==="
colima status
docker version
docker info >/dev/null && echo "Docker engine: OK"

echo "=== kubectl ==="
kubectl version --client

echo "=== kind (ou k3d) ==="
kind version 2>/dev/null || true
k3d version 2>/dev/null || true

echo "=== Helm ==="
helm version

echo "=== GitHub CLI (opcional) ==="
gh --version 2>/dev/null || echo "gh não instalado (ok se foi opção consciente)"
```

### Critérios de “deu certo”

- [ ] `java -version` mostra **21**
- [ ] `mvn -version` usa Java **21**
- [ ] `docker info` responde sem erro
- [ ] `kubectl version --client` funciona
- [ ] `kind version` **ou** `k3d version` funciona
- [ ] `helm version` mostra v3
- [ ] Git com `user.name` e `user.email` configurados

---

## 14. Problemas comuns

### `java -version` não é 21

Causa mais comum: outro JDK no início do `PATH`.

```bash
which -a java
echo "$PATH"
/usr/libexec/java_home -V
```

Garanta que `openjdk@21` venha antes de outros JDKs no `~/.zshrc` e reabra o terminal.

### `docker info` falha com “Cannot connect to the Docker daemon”

1. Confira se a VM está no ar: `colima status`
2. Se estiver parada: `colima start`
3. Ative o contexto: `docker context use colima`
4. Se o contexto estiver errado (`desktop-linux` ou similar), liste com `docker context ls` e selecione `colima`

### Colima inicia, mas `docker` ainda falha

Em alguns setups o socket não fica no PATH esperado. Verifique:

```bash
colima status
docker context inspect colima
```

Se necessário, reinicie o contexto:

```bash
colima stop
colima start --cpu 4 --memory 8 --disk 40
docker context use colima
```

### Homebrew lento ou falhando em rede corporativa

- Verifique proxy/VPN
- Tente `brew update` novamente fora da VPN

### Espaço em disco insuficiente

```bash
df -h /
docker system df
```

Remova imagens/containers antigos com cuidado:

```bash
docker system prune
```

### Apple Silicon e imagens `amd64`

Na maior parte deste curso as imagens oficiais já têm builds multi-arch. Se algum componente só existir para `amd64`, o Docker pode emular via Rosetta/QEMU — espere performance menor e documente o aviso quando aparecer.

---

## 15. Desafio opcional

1. Crie um diretório temporário e gere um projeto Maven mínimo só para validar o toolchain:

```bash
mkdir -p /tmp/java-toolchain-check && cd /tmp/java-toolchain-check
mvn -B archetype:generate \
  -DgroupId=lab.check \
  -DartifactId=toolchain-check \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DarchetypeVersion=1.5 \
  -DinteractiveMode=false
cd toolchain-check
mvn -B test
```

2. Anote as versões instaladas em um arquivo pessoal (não precisa versionar), por exemplo `~/lab-versions.txt`, para comparar com colegas ou com a máquina de um aluno.

---

## Próximo passo

Com o ambiente validado, consulte os [cheatsheets das ferramentas](cheatsheets/README.md) quando precisar de comandos rápidos e siga para o **Módulo 02 — Setup do cluster Kubernetes** ([02-setup-cluster-kubernetes.md](02-setup-cluster-kubernetes.md)).
