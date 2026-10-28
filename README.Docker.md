# Docker

O Docker Compose inicia somente a API e conecta ao MongoDB Atlas configurado no `.env`; não inicia um banco local. Consulte o `README.md` para instruções de configuração, variáveis, frontend e endpoints.

## Compilar e executar

Execute:

```bash
docker compose config
docker compose up --build
```

A API ficará disponível em `http://localhost:8080`. Para encerrá-la, use `docker compose down`.

## Publicação

Para instruções específicas do ambiente de hospedagem, consulte a documentação do provedor; a imagem usa Java 17 e recebe as variáveis MongoDB/JWT via ambiente.