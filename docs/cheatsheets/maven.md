# Cheatsheet — Maven

Build de projetos Java. Voltar ao [índice](README.md).

Documentação: https://maven.apache.org/guides/

```bash
mvn -version
mvn -B clean                   # limpa target/
mvn -B compile                 # compila
mvn -B test                    # testes
mvn -B package                 # gera JAR/WAR em target/
mvn -B verify                  # package + checks
mvn -B install                 # instala no .m2 local
mvn -B clean package -DskipTests

mvn -B dependency:tree         # árvore de dependências
mvn -B dependency:resolve
mvn -B spring-boot:run         # sobe app Spring Boot (se plugin existir)

mvn -B archetype:generate \
  -DgroupId=lab.demo \
  -DartifactId=demo \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DarchetypeVersion=1.5 \
  -DinteractiveMode=false
```

## Flags úteis

| Flag | Efeito |
| --- | --- |
| `-B` | batch (menos prompts) |
| `-q` | quiet |
| `-X` | debug |
| `-DskipTests` | não roda testes |
| `-o` | offline (só cache local) |

Cache local: `~/.m2/repository`
