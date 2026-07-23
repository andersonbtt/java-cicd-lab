# Cheatsheet — ELK + Fluent Bit

Observabilidade no lab. Voltar ao [índice](README.md).

Módulo: [07-stack-elk.md](../07-stack-elk.md)

```bash
# sysctl no node kind (necessário para ES)
docker exec lab-control-plane sysctl -w vm.max_map_count=262144

# aplicar / remover stack
kubectl apply -k observability/
kubectl delete -k observability/

# status
kubectl get pods -n logging
kubectl logs -n logging statefulset/elasticsearch --tail=50
kubectl logs -n logging daemonset/fluent-bit --tail=50

# Elasticsearch
kubectl port-forward -n logging svc/elasticsearch 9200:9200 --address 127.0.0.1
curl -sS 'http://127.0.0.1:9200/_cluster/health?pretty'
curl -sS 'http://127.0.0.1:9200/_cat/indices?v'

# Kibana
kubectl port-forward -n logging svc/kibana 5601:5601 --address 127.0.0.1
# http://127.0.0.1:5601
```
