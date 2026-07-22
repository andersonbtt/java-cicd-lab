# Cheatsheet — JDK / Java

Runtime e compilador (Java 21 no laboratório). Voltar ao [índice](README.md).

Documentação: https://docs.oracle.com/en/java/

```bash
java -version
javac -version
echo "$JAVA_HOME"
which java
/usr/libexec/java_home -V      # JDKs registrados no macOS
/usr/libexec/java_home -v 21   # caminho do Java 21
```

## Definir Java 21 na sessão atual (Homebrew)

```bash
# Apple Silicon
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
export PATH="$JAVA_HOME/bin:$PATH"

# Intel
export JAVA_HOME="/usr/local/opt/openjdk@21"
export PATH="$JAVA_HOME/bin:$PATH"
```

```bash
java -jar app.jar
java -Xms256m -Xmx512m -jar app.jar
```
