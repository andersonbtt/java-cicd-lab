# Cheatsheet — Colima

VM leve que hospeda o Docker Engine neste laboratório. Voltar ao [índice](README.md).

Documentação: https://github.com/abiosoft/colima

```bash
colima version
colima status
colima start
colima start --cpu 4 --memory 8 --disk 40
colima stop
colima restart
colima delete                  # apaga a VM (dados do disco da VM)
colima list
colima ssh                     # shell dentro da VM
```

## Dicas

```bash
# Recriar com mais recursos
colima delete
colima start --cpu 4 --memory 8 --disk 40

# Depois de start, conferir contexto Docker
docker context use colima
```

| Situação | Comando |
| --- | --- |
| Mac reiniciou | `colima start` |
| Liberar RAM | `colima stop` |
| Docker não conecta | `colima status` → `colima start` → `docker context use colima` |
