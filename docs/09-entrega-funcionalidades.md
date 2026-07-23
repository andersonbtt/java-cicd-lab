# Módulo 09 — Entrega de funcionalidades

## Objetivo

Fechar o ciclo completo: **feature → testes → CI → imagem → GitOps → cluster → Kibana**.

Neste módulo a aplicação ganha uma API de **tarefas** (CRUD mínimo) com validação, erros padronizados e logs de negócio. Você pratica o fluxo de entrega como em um time real.

| Entrega | Capacidade |
| --- | --- |
| 1 | `GET /api/tasks` — listagem |
| 2 | `POST /api/tasks` — criação com validação |
| 3 | Erros padronizados (`validation_failed`, `task_not_found`) |
| 4 | Métricas/logs de negócio (`task_created`, `task_listed`, `/api/tasks/stats`) |
| 5 | Exercício: bug proposital + investigação no Kibana + PR de correção |

> Pré-requisitos: módulos 04–08 (app, Actions, Argo CD, ELK, logs JSON).

---

## API entregue

### Recursos

| Método | Path | Descrição |
| --- | --- | --- |
| `GET` | `/api/tasks` | Lista tarefas (`?status=TODO\|DONE` opcional) |
| `POST` | `/api/tasks` | Cria tarefa `{ "title": "..." }` → **201** |
| `GET` | `/api/tasks/{id}` | Busca por id → **404** se não existir |
| `GET` | `/api/tasks/stats` | Contadores in-memory de operações |

### Validação (`POST`)

- `title` obrigatório
- tamanho entre **3** e **120** caracteres

Erro **400** (exemplo):

```json
{
  "timestamp": "...",
  "status": 400,
  "code": "validation_failed",
  "message": "Request validation failed",
  "details": { "title": "title is required" }
}
```

### Logs de negócio (JSON)

| Evento | Quando |
| --- | --- |
| `event=task_created` | após criar |
| `event=task_listed` | após listar |
| `event=validation_failed` | body inválido |
| `event=task_not_found` | id inexistente |

---

## Fluxo padrão de cada entrega (use sempre)

```text
branch → commit → push → PR
   → GitHub Actions (CI verde)
   → merge em main
   → Publish (imagem)  [se app/** mudou]
   → atualizar tag no overlay (se necessário) + push
   → Argo CD sync
   → curl no cluster
   → filtro no Kibana
```

Checklist curto por feature:

1. `git switch -c feature/<nome>`
2. Código + testes (`mvn -B test`)
3. PR e Actions verdes
4. Merge
5. Build/tag local **ou** imagem do GHCR; atualizar `k8s/overlays/dev` se a tag mudou
6. Confirmar sync Argo / pods Ready
7. Validar HTTP + logs no Kibana (`kubernetes.namespace_name : "app"`)

---

## 1. Validar localmente (antes do PR)

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
export PATH="$JAVA_HOME/bin:$PATH"
cd app
mvn -B test
mvn -B spring-boot:run
```

Em outro terminal:

```bash
# criar
curl -sS -X POST http://localhost:8080/api/tasks \
  -H 'Content-Type: application/json' \
  -H 'X-Request-Id: feat-create-1' \
  -d '{"title":"Estudar GitOps"}'

# listar
curl -sS http://localhost:8080/api/tasks

# validação
curl -sS -X POST http://localhost:8080/api/tasks \
  -H 'Content-Type: application/json' \
  -d '{"title":"ab"}'

# stats
curl -sS http://localhost:8080/api/tasks/stats
```

**Esperado:** create 201, list com item, validação 400, stats com contadores > 0.

### Script de smoke (todos os endpoints)

Com a app no ar (`spring-boot:run` ou cluster/Ingress):

```bash
# Se a porta 8080 estiver com o Ingress do kind, use outra porta local.

# Dentro de app/ (seu caso atual):
SERVER_PORT=18080 mvn -B spring-boot:run

# Ou na raiz do repositório:
# SERVER_PORT=18080 mvn -f app/pom.xml spring-boot:run

# em outro terminal (na raiz do repo)
BASE_URL=http://127.0.0.1:18080 ./scripts/smoke-api.sh

# app já no cluster/Ingress (imagem 0.3.0+):
BASE_URL=http://127.0.0.1:8080 ./scripts/smoke-api.sh
```

O script valida health, hello, `X-Request-Id`, CRUD de tasks, validação, 404 e stats.

---

## 2. Publicar no cluster (imagem 0.3.0)

Neste módulo a tag sobe para **`labjavacicd:0.3.0`**.

### Opção A — kind local (rápido)

```bash
cd app
docker build -t labjavacicd:0.3.0 .
kind load docker-image labjavacicd:0.3.0 --name lab
cd ..
kubectl apply -k k8s/overlays/dev
kubectl rollout status deploy/labjavacicd -n app --timeout=180s
```

### Opção B — GitOps completo

1. Commit de `app/` + `k8s/overlays/dev` (tag `0.3.0`)
2. PR → CI → merge
3. Publish no GHCR (se configurado) **ou** continue com kind load no lab
4. Argo CD sincroniza o overlay

> Se o Argo apontar para imagem local `labjavacicd:0.3.0`, o `kind load` continua necessário após cada rebuild.

Smoke no Ingress:

```bash
curl -sS -X POST http://localhost:8080/api/tasks \
  -H 'Content-Type: application/json' \
  -H 'X-Request-Id: k8s-create-1' \
  -d '{"title":"Deploy no kind"}'

curl -sS http://localhost:8080/api/tasks
curl -sS http://localhost:8080/api/tasks/stats
```

---

## 3. Observabilidade da feature

```bash
kubectl logs -n app deploy/labjavacicd --tail=50 | rg 'task_created|task_listed|validation_failed'
```

No Kibana (Discover, data view `k8s-logs*`):

```text
kubernetes.namespace_name : "app" and message : "task_created"
```

```text
kubernetes.namespace_name : "app" and message : "validation_failed"
```

Salve buscas: `lab-tasks-created` e `lab-tasks-validation`.

---

## 4. Entregas guiadas (roteiro do curso)

### Entrega A — listagem e criação

Já implementadas em `TaskController` / `TaskService`.  
Exercício do aluno: abrir PR descrevendo os endpoints e evidências de `mvn test` + curl.

### Entrega B — validação e erros padronizados

`CreateTaskRequest` + `ApiExceptionHandler`.  
Exercício: adicionar um teste que envia `title` com 121 caracteres e espera 400.

### Entrega C — métrica de negócio

Logs `createdOperations` / `listedOperations` + `GET /api/tasks/stats`.  
Exercício: gerar 5 creates, 3 lists e conferir stats + Kibana.

### Entrega D — bug lab (investigação)

**Bug proposital (introduza só na branch do exercício):**

No método `TaskService.list`, comente o filtro e force retorno de todas as tarefas mesmo com `status` informado:

```java
// BUG LAB: filtro ignorado
// .filter(task -> statusFilter == null || task.status() == statusFilter)
.filter(task -> true)
```

Passos:

1. Commit na branch `bugfix/task-status-filter`
2. Deploy da imagem com o bug
3. Crie tarefas e chame `GET /api/tasks?status=DONE`
4. No Kibana, observe `event=task_listed` com `statusFilter=DONE` mas `totalReturned` incluindo TODOs
5. Corrija o filtro, PR, CI verde, redeploy
6. Confirme que `?status=DONE` só retorna DONE

> Não faça merge do bug em `main` sem a correção — o exercício é o ciclo investigar → corrigir → entregar.

---

## 5. Estrutura de código

```text
app/src/main/java/com/bittclouds/labjavacicd/
├── task/
│   ├── Task.java
│   ├── TaskStatus.java
│   ├── CreateTaskRequest.java
│   ├── TaskNotFoundException.java
│   ├── TaskService.java
│   └── TaskController.java
└── web/
    ├── ApiExceptionHandler.java
    ├── HelloController.java
    └── RequestIdFilter.java
```

Armazenamento: **in-memory** (reinicia com o Pod). Suficiente para o lab; persistência fica fora do escopo.

---

## 6. Checklist de conclusão do módulo

- [ ] `mvn test` verde (hello + tasks)  
- [ ] `POST/GET /api/tasks` funcionando no cluster  
- [ ] Validação retorna `validation_failed`  
- [ ] `task_not_found` para id inexistente  
- [ ] `/api/tasks/stats` reflete operações  
- [ ] Eventos visíveis no Kibana  
- [ ] (Opcional) Exercício do bug de filtro concluído com PR  

```bash
mvn -B -f app/pom.xml test
curl -sS http://localhost:8080/api/tasks/stats
kubectl logs -n app deploy/labjavacicd --tail=20
```

---

## 7. Problemas comuns

### `GET /api/tasks/stats` retorna `task_not_found`

Imagem antiga sem a rota `/stats`, ou conflito de rota. Confirme tag `0.3.0` e rebuild/load.

### Stats zeram após restart do Pod

Esperado (storage in-memory).

### Argo reverte a tag

Alinhe o Git (`k8s/overlays/dev`) com a tag que você carregou no kind.

---

## 8. Próximo passo

Módulo opcional de comparativo com Jenkins: [10-opcional-jenkins.md](10-opcional-jenkins.md).

Ou revise a [ementa](00-ementa.md) e o [fluxo CI/CD](fluxo-cicd-github.md) como fechamento do curso.
