# toEvent

Aplicação web para organizar eventos pessoais. O projeto contém uma API REST em Spring Boot, persistência em MongoDB e uma interface React/Vite.

## Requisitos

- Java 17 ou mais recente. O projeto gera bytecode compatível com Java 17.
- Docker Desktop para iniciar a API em container.
- Node.js e npm para executar o frontend.
- Uma instância MongoDB acessível (MongoDB Atlas é a configuração documentada abaixo).

## Configurar MongoDB Atlas

1. Crie um usuário de banco no Atlas e permita conexões do endereço IP da máquina/servidor que executará a aplicação.
2. Copie `.env-example` para `.env`.
   - Windows CMD: `copy .env-example .env`
   - Bash: `cp .env-example .env`
3. Preencha no `.env`:

   | Variável | Descrição |
   | --- | --- |
   | `MONGODB_URI` | URI de conexão do cluster Atlas (`mongodb+srv://...`). |
   | `MONGODB_DATABASE` | Nome do banco usado pela aplicação. |
   | `AUTH_TOKEN_SECRET` | Segredo aleatório forte usado para assinar JWTs. |
   | `CORS_ALLOWED_ORIGINS` | Origens permitidas separadas por vírgula (ex: `https://seu-app.vercel.app`). |
   | `SWAGGER_ENABLED` | Ativa Swagger UI e OpenAPI docs (`true` em dev, `false` em prod). |

   Gere um segredo com `openssl rand -base64 48`, se o OpenSSL estiver instalado. Se a senha do banco tiver caracteres especiais, faça URL-encode antes de colocá-la na URI.

4. Não versione nem compartilhe `.env`. A aplicação cria um índice único para o email; se o banco de teste já tiver emails duplicados, remova ou corrija esses registros antes de iniciar a API.

## Executar a API

### Com Docker Compose

O Compose inicia somente a API e conecta ao MongoDB externo configurado no `.env`; não inicia um MongoDB local.

```bash
docker compose config
docker compose up --build
```

A API ficará disponível em `http://localhost:8080`. Para parar:

```bash
docker compose down
```

### Localmente

Com Java 17 instalado e `.env` preenchido:

- Windows PowerShell: `./mvnw.cmd spring-boot:run`
- Bash/macOS/Linux: `./mvnw spring-boot:run`

## Executar o frontend

O frontend é iniciado separadamente do Compose:

```bash
cd frontend
npm ci
npm run dev
```

Vite informa o endereço local no terminal (normalmente `http://localhost:5173`). Por padrão, o frontend chama a API em `http://localhost:8080`. Para outro endereço, configure `VITE_API_URL` no ambiente do frontend.

## Endpoints

O cadastro e o login são públicos. Os demais endpoints abaixo exigem o cabeçalho `Authorization: Bearer seu-jwt-aqui`, usando o JWT retornado pelo login.

| Método | Endpoint | Ação |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Cadastra uma conta (`name`, `email`, `password`). |
| `POST` | `/api/auth/login` | Autentica (`email`, `password`) e retorna um JWT. |
| `GET` | `/api/users` | Retorna o usuário autenticado. |
| `DELETE` | `/api/users` | Exclui o usuário autenticado. |
| `GET` | `/api/events` | Lista os eventos do usuário autenticado. |
| `GET` | `/api/events/{eventId}` | Consulta um evento pertencente ao usuário. |
| `POST` | `/api/events` | Cria um evento (`eventName`, `eventDate`, `description` opcional). Use `YYYY-MM-DD` para a data. |
| `PUT` | `/api/events/{eventId}` | Atualiza um evento pertencente ao usuário (`eventName`, `eventDate`, `description`). |
| `DELETE` | `/api/events/{eventId}` | Exclui um evento pertencente ao usuário. |

A documentação interativa da API fica em `http://localhost:8080/swagger-ui/index.html`.

## Testes e validações

Na raiz do projeto:

```bash
./mvnw test
```

No Windows PowerShell, use `./mvnw.cmd test`. Para o frontend:

```bash
cd frontend
npm run lint
npm run build
```

## Antes de publicar 

Restrinja as origens CORS em `SecurityConfig`; atualmente a configuração aceita origens amplas. Use um segredo JWT forte, mantenha as credenciais do Atlas fora do Git e revise as regras de acesso à rede do cluster.
