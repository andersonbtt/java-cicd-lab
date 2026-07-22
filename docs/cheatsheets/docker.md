# Cheatsheet — Docker CLI

Imagens e containers (engine via Colima). Voltar ao [índice](README.md).

Documentação: https://docs.docker.com

```bash
docker version
docker info
docker context ls
docker context use colima

# Imagens
docker images
docker pull <imagem>:<tag>
docker build -t <nome>:<tag> .
docker tag <local> <registry>/<nome>:<tag>
docker push <registry>/<nome>:<tag>
docker rmi <imagem>

# Containers
docker ps
docker ps -a
docker run --rm -it <imagem> sh
docker run --rm -p 8080:8080 <imagem>
docker logs <container>
docker logs -f <container>
docker exec -it <container> sh
docker stop <container>
docker rm <container>

# Limpeza
docker system df
docker system prune            # remove não usados
docker system prune -a         # também imagens sem tag (cuidado)
```

## Compose

Quando houver `compose.yaml` / `docker-compose.yml`:

```bash
docker compose up -d
docker compose ps
docker compose logs -f
docker compose down
```

## Smoke test

```bash
docker run --rm hello-world
```
