# Módulo 08 — Aplicação integrada ao ELK

## Objetivo

Tornar os logs da aplicação **úteis no Kibana**: JSON estruturado, **request id** correlacionável e labels Kubernetes para filtros.

Ao final deste módulo:

| Item | Resultado |
| --- | --- |
| Logs JSON | stdout da app em formato Logstash/JSON |
| `X-Request-Id` | gerado ou propagado; ecoado na resposta e no MDC |
| Labels K8s | `app.kubernetes.io/component=api` + annotations de observabilidade |
| Kibana | busca por `requestId` / namespace `app` / mensagem de hello |
| Imagem | `labjavacicd:0.2.0` |

> Pré-requisitos: [Módulo 07](07-stack-elk.md) (ELK + Fluent Bit) e app no cluster ([Módulo 04](04-projeto-java-maven.md) / [06](06-argocd-gitops.md)).

---

## O que mudou no código

```text
app/
├── pom.xml                          # + logstash-logback-encoder
├── src/main/resources/
│   ├── application.yml
│   └── logback-spring.xml           # appender JSON no console
└── src/main/java/.../web/
    ├── RequestIdFilter.java         # X-Request-Id + MDC
    └── HelloController.java         # log.info no /api/hello
```

Fluxo de um request:

```text
Cliente
  │  (opcional) Header X-Request-Id
  ▼
RequestIdFilter → MDC(requestId, httpMethod, httpPath)
  ▼
HelloController → log JSON com requestId
  ▼
stdout do Pod → Fluent Bit → Elasticsearch → Kibana
```

---

## 1. Dependência e log JSON

No `pom.xml`:

```xml
<dependency>
  <groupId>net.logstash.logback</groupId>
  <artifactId>logstash-logback-encoder</artifactId>
  <version>8.1</version>
</dependency>
```

`logback-spring.xml` envia logs para o **console em JSON**, com campos `service`, `lab` e chaves MDC (`requestId`, `httpMethod`, `httpPath`).

Exemplo de linha (simplificada):

```json
{
  "@timestamp": "2026-07-23T16:00:00.000Z",
  "message": "Handling hello request",
  "service": "labjavacicd",
  "requestId": "a1b2c3d4-...",
  "httpMethod": "GET",
  "httpPath": "/api/hello",
  "logger_name": "com.bittclouds.labjavacicd.web.HelloController"
}
```

---

## 2. Correlation id (`X-Request-Id`)

`RequestIdFilter`:

1. Lê o header `X-Request-Id` (se vier vazio, gera um UUID)
2. Coloca no **MDC** (`requestId`)
3. Devolve o mesmo valor no header da resposta

Teste local:

```bash
cd app
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
export PATH="$JAVA_HOME/bin:$PATH"

mvn -B test
mvn -B spring-boot:run
```

Em outro terminal:

```bash
curl -sS -D - http://localhost:8080/api/hello -o /tmp/hello.json
# veja X-Request-Id na resposta

curl -sS -D - \
  -H 'X-Request-Id: demo-123' \
  http://localhost:8080/api/hello -o /tmp/hello2.json
# X-Request-Id: demo-123
```

Os logs no terminal do Spring devem ser **uma linha JSON por evento**, contendo `requestId`.

---

## 3. Labels e annotations no Kubernetes

O `Deployment` agora inclui:

| Campo | Valor | Uso no Kibana |
| --- | --- | --- |
| label `app.kubernetes.io/component` | `api` | filtrar tipo de workload |
| annotation `lab.observability/logging` | `json` | documentação operacional |
| annotation `lab.observability/request-id-header` | `X-Request-Id` | contrato do correlation id |

A imagem passa a `labjavacicd:0.2.0` (overlay `k8s/overlays/dev`).

---

## 4. Build, load e deploy

```bash
colima status
docker context use colima
kubectl config use-context kind-lab

cd app
docker build -t labjavacicd:0.2.0 .
kind load docker-image labjavacicd:0.2.0 --name lab
cd ..

kubectl apply -k k8s/overlays/dev
kubectl rollout status deploy/labjavacicd -n app --timeout=180s
```

Se o Argo CD estiver sincronizando o overlay do Git, faça **commit + push** da tag `0.2.0` e aguarde o sync (ou `argocd app sync labjavacicd --core` com namespace `argocd`). Sem push, o self-heal pode reverter um apply manual que divirja do Git.

---

## 5. Validação ponta a ponta (request → Kibana)

### 5.1 Gerar tráfego com request id conhecido

```bash
REQ_ID="lab-$(date +%s)"
echo "REQ_ID=$REQ_ID"

curl -sS -D - \
  -H "X-Request-Id: ${REQ_ID}" \
  http://localhost:8080/api/hello -o /dev/null

# confirme nos logs do pod
kubectl logs -n app deploy/labjavacicd --tail=20
```

**Esperado:** linha JSON com `"message":"Handling hello request"` e `"requestId":"<seu-id>"`.

### 5.2 Conferir no Elasticsearch (opcional)

```bash
kubectl port-forward -n logging svc/elasticsearch 9200:9200 --address 127.0.0.1
```

```bash
curl -sS -X POST 'http://127.0.0.1:9200/k8s-logs/_search?pretty' \
  -H 'Content-Type: application/json' \
  -d "{\"query\":{\"query_string\":{\"query\":\"${REQ_ID}\"}}}"
```

> O Fluent Bit pode demorar alguns segundos. Se o campo vier aninhado no `log`/`message`, busque pelo texto do UUID mesmo assim.

### 5.3 Kibana Discover

1. Port-forward do Kibana (se ainda não estiver):

```bash
kubectl port-forward -n logging svc/kibana 5601:5601 --address 127.0.0.1
```

2. **Discover** → data view `k8s-logs*`
3. Intervalo: Last 15 minutes
4. Filtros sugeridos (KQL):

```text
kubernetes.namespace_name : "app"
```

```text
kubernetes.labels.app_kubernetes_io/name : "labjavacicd"
```

```text
message : "Handling hello request"
```

```text
requestId : "lab-........" 
```

(Se `requestId` não aparecer como campo próprio, use busca livre pelo valor do `REQ_ID` — o JSON pode estar dentro do campo `log` / `message` até você extrair com ingest pipeline; para o lab, a busca textual já valida o fluxo.)

### 5.4 Busca salva (recomendado)

1. Aplique o filtro de namespace `app` + mensagem hello  
2. **Save** → nome: `lab-app-hello`  
3. Opcional: crie outra busca `lab-app-errors` com:

```text
kubernetes.namespace_name : "app" and (log.level : "ERROR" or level : "ERROR")
```

---

## 6. Checklist de verificação

- [ ] `mvn test` passa (inclui teste do header `X-Request-Id`)  
- [ ] Logs locais/pod em JSON  
- [ ] `curl` devolve `X-Request-Id`  
- [ ] Deploy `0.2.0` Available no namespace `app`  
- [ ] Request com `REQ_ID` aparece no `kubectl logs` e no Kibana/ES  

```bash
mvn -B -f app/pom.xml test
kubectl get deploy,pods -n app
curl -sS -D - -H "X-Request-Id: check-1" http://localhost:8080/api/hello -o /dev/null
kubectl logs -n app deploy/labjavacicd --tail=5
```

---

## 7. Problemas comuns

### Logs ainda em texto plano

- Confirme que a imagem `0.2.0` está no node (`kind load`)  
- O pod pode estar na tag antiga: `kubectl describe pod -n app -l app.kubernetes.io/name=labjavacicd | rg Image`

### `requestId` não vira campo no Kibana

Normal se o Fluent Bit indexa a linha inteira como string. Use busca full-text pelo UUID. Extrair campos JSON com pipeline/ingest fica como evolução (desafio).

### Argo CD reverte a tag

Publique a mudança do overlay no Git ou pause o sync enquanto testa apply local.

### Testes falham por logback

Garanta a dependência `logstash-logback-encoder` no `pom.xml` e rode `mvn -B test` de novo.

---

## 8. Desafio opcional

1. Adicione um endpoint `GET /api/boom` que loga `ERROR` e retorna 500 — filtre no Kibana.  
2. Configure um **Ingest Pipeline** no ES para parsear o JSON do campo `log` e mapear `requestId` como keyword.  
3. Crie um dashboard com contagem de `Handling hello request` ao longo do tempo.

---

## Próximo passo

Com observabilidade da app no ar, siga para o **Módulo 09 — Entrega de funcionalidades** ([09-entrega-funcionalidades.md](09-entrega-funcionalidades.md)).
