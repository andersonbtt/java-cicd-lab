# Módulo 07 — Stack ELK no Kubernetes

## Objetivo

Subir a base de **observabilidade** no cluster: **Elasticsearch** (armazenamento/busca), **Kibana** (UI) e **Fluent Bit** (coleta de logs dos containers).

Ao final deste módulo:

| Item | Resultado |
| --- | --- |
| Namespace | `logging` (já criado no Módulo 02) |
| Elasticsearch | single-node respondendo em `:9200` |
| Kibana | UI acessível via port-forward |
| Fluent Bit | DaemonSet enviando logs para o ES |
| Smoke | logs do cluster visíveis no Discover |

> Neste lab o “L” do ELK é o **Fluent Bit** (mais leve que Logstash em Kubernetes).

> Pré-requisitos: [Módulo 02](02-setup-cluster-kubernetes.md) (cluster saudável). Recomendado: app do [Módulo 04](04-projeto-java-maven.md) rodando para gerar logs.

---

## Visão da arquitetura

```text
Pods (app, argocd, kube-system, …)
        │  stdout/stderr → arquivos em /var/log/containers
        ▼
Fluent Bit (DaemonSet, 1 por node)
        │  filtro kubernetes (namespace, pod, labels)
        ▼
Elasticsearch (StatefulSet, ns logging)
        ▲
Kibana ─┘  (consulta / visualização)
```

Arquivos:

```text
observability/
├── kustomization.yaml
├── elasticsearch.yaml
├── kibana.yaml
└── fluent-bit.yaml
```

---

## Aviso de recursos (importante)

ELK é o componente que mais consome memória. Com Colima em **8 GB**, o lab funciona, mas pode ficar apertado junto com Argo CD + app.

Sugestões:

```bash
# se ainda não subiu com 8 GB:
colima stop
colima start --cpu 4 --memory 8 --disk 40
```

Se pods do ES ficarem `OOMKilled` / `Pending`, aumente a memória do Colima ou pause workloads não essenciais.

---

## 1. Pré-checagens

```bash
kubectl config use-context kind-lab
kubectl get ns logging
kubectl get nodes
```

### `vm.max_map_count` (Elasticsearch)

O Elasticsearch exige `vm.max_map_count` alto no **node**. No kind:

```bash
docker exec lab-control-plane sysctl -w vm.max_map_count=262144
docker exec lab-control-plane sysctl vm.max_map_count
```

**Esperado:** `vm.max_map_count = 262144`.

O manifesto também traz um initContainer privilegiado tentando o mesmo; no kind o comando acima é a garantia.

> Após recriar o cluster kind, rode o `sysctl` de novo.

---

## 2. Aplicar a stack

Na raiz do repositório:

```bash
kubectl apply -k observability/

kubectl get all -n logging
```

Aguarde o Elasticsearch (pode levar 1–3 minutos na primeira vez — pull de imagem grande):

```bash
kubectl rollout status statefulset/elasticsearch -n logging --timeout=300s
kubectl rollout status deployment/kibana -n logging --timeout=300s
kubectl rollout status daemonset/fluent-bit -n logging --timeout=180s
```

**Esperado:**

```bash
kubectl get pods -n logging
# elasticsearch-0   1/1 Running
# kibana-...        1/1 Running
# fluent-bit-...    1/1 Running
```

---

## 3. Validar o Elasticsearch

```bash
kubectl port-forward -n logging svc/elasticsearch 9200:9200
```

Em outro terminal:

```bash
curl -sS http://127.0.0.1:9200
curl -sS 'http://127.0.0.1:9200/_cluster/health?pretty'
```

**Esperado:** JSON com `cluster_name` / `status` `green` ou `yellow` (yellow é normal com 1 nó e réplicas 0 em alguns índices internos).

> No **zsh**, URLs com `?` precisam de aspas — senão aparece `no matches found`.

Listar índices (depois que o Fluent Bit enviar dados):

```bash
curl -sS 'http://127.0.0.1:9200/_cat/indices?v'
```

Procure algo como `k8s-logs` (nome configurado no Fluent Bit).

---

## 4. Acessar o Kibana

```bash
kubectl port-forward -n logging svc/kibana 5601:5601 --address 127.0.0.1
```

Abra: [http://127.0.0.1:5601](http://127.0.0.1:5601)

### 4.1 Criar data view (índice)

1. Menu **Management → Stack Management → Data Views** (ou **Discover** pedirá criar)
2. Create data view  
   - Name: `k8s-logs`  
   - Index pattern: `k8s-logs*`  
3. Time field: `@timestamp` (se existir) ou o campo de tempo que o Fluent Bit enviar
4. Save

### 4.2 Discover — smoke test

1. Abra **Discover**
2. Selecione o data view `k8s-logs`
3. Ajuste o intervalo de tempo (ex.: Last 15 minutes)
4. Filtre, por exemplo:

```text
kubernetes.namespace_name : "app"
```

ou

```text
kubernetes.pod_name : *labjavacicd*
```

Gere tráfego na app (se estiver no ar):

```bash
curl -sS http://localhost:8080/api/hello
curl -sS http://localhost:8080/actuator/health
```

Em seguida atualize o Discover — devem aparecer linhas de log dos pods.

---

## 5. O que cada manifesto faz

| Arquivo | Função |
| --- | --- |
| `elasticsearch.yaml` | StatefulSet 1 réplica, security desabilitada (**só lab**), heap 512m, PVC 5Gi |
| `kibana.yaml` | UI apontando para o Service `elasticsearch` |
| `fluent-bit.yaml` | SA + RBAC + ConfigMap + DaemonSet; lê `/var/log/containers`, enriquece com metadados K8s, envia ao ES índice `k8s-logs` |

### Segurança no lab vs produção

Neste módulo `xpack.security` está **desligado** para reduzir atrito didático. Em produção: TLS, usuários, roles e secrets — fora do escopo deste passo.

---

## 6. Checklist de verificação (obrigatório)

- [ ] `vm.max_map_count=262144` no node kind  
- [ ] Pods `elasticsearch`, `kibana`, `fluent-bit` em `Running`  
- [ ] `curl` em `:9200` retorna informações do cluster  
- [ ] Índice `k8s-logs` (ou similar) aparece em `/_cat/indices`  
- [ ] Kibana abre e o Discover mostra logs  

```bash
kubectl get pods -n logging
kubectl logs -n logging daemonset/fluent-bit --tail=50
curl -sS 'http://127.0.0.1:9200/_cat/indices?v'   # com port-forward do ES
```

---

## 7. Problemas comuns

### Elasticsearch `CrashLoop` / `max virtual memory areas`

```bash
docker exec lab-control-plane sysctl -w vm.max_map_count=262144
kubectl delete pod elasticsearch-0 -n logging
```

### Pod `Pending` (Insufficient memory)

Aumente RAM do Colima ou reduza temporariamente outros workloads (ex.: pare smoke-web antigo).

### Fluent Bit Running, mas sem índice no ES

```bash
kubectl logs -n logging daemonset/fluent-bit --tail=100
```

Verifique erros de conexão com `elasticsearch.logging.svc`. Confirme o ES healthy. Aguarde 1–2 minutos após o ES ficar Ready.

### Kibana `Red` / não sobe

O Kibana espera o ES. Veja:

```bash
kubectl logs -n logging deploy/kibana --tail=100
kubectl get endpoints elasticsearch -n logging
```

### Port-forward cai

Reinicie o forward. Use `--address 127.0.0.1` se houver conflito IPv6.

### Disco do PVC

Se o kind reclamar de storage, confira o provisioner padrão:

```bash
kubectl get storageclass
kubectl get pvc -n logging
```

O kind costuma ter `standard` (local-path) como default.

---

## 8. Operação do dia a dia

| Tarefa | Comando |
| --- | --- |
| Status | `kubectl get pods -n logging` |
| Logs do coletor | `kubectl logs -n logging ds/fluent-bit -f` |
| UI Kibana | `kubectl port-forward -n logging svc/kibana 5601:5601 --address 127.0.0.1` |
| Remover a stack | `kubectl delete -k observability/` |
| Apagar dados do ES | deletar o PVC após remover o StatefulSet (cuidado) |

---

## 9. Desafio opcional

1. No Discover, salve uma busca filtrando só `namespace_name: app`.  
2. Crie um dashboard simples com contagem de documentos por namespace.  
3. Compare logs do `labjavacicd` antes e depois de algumas requests `curl`.

---

## Próximo passo

Com logs fluindo para o ELK, siga para o **Módulo 08 — Aplicação integrada ao ELK** ([08-app-integracao-elk.md](08-app-integracao-elk.md)): logging estruturado JSON e correlation id na app Java.
