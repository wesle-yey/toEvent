# toEvent

Aplicação web para organizar eventos pessoais. O projeto reúne uma interface React e uma API REST em Spring Boot, com MongoDB para persistência e autenticação por JWT.

> **Status:** MVP em desenvolvimento. Cadastro, login e gerenciamento de eventos estão implementados. A página Premium é apenas demonstrativa: ainda não há compra, upgrade ou cobrança.

## Funcionalidades

- Cadastro e login de usuários.
- Criação, consulta, edição e exclusão de eventos com data e descrição.
- Eventos associados à conta autenticada.
- Limite de cinco eventos para contas não Premium.
- Interface responsiva em português.

## Tecnologias

- Java 17 e Spring Boot
- Spring Security, JWT e validação de dados
- MongoDB
- React, Vite e Axios
- Docker Compose (opcional, para executar a API)

## Requisitos

- Java 17 ou mais recente para executar e testar a API localmente.
- Node.js e npm para executar o frontend.
- Uma instância MongoDB acessível, local ou hospedada.
- Docker Desktop apenas se for usar Docker Compose.

## Configuração

Na raiz do projeto, copie `.env-example` para `.env` e preencha as configurações:

```text
MONGODB_URI=mongodb+srv://...
MONGODB_DATABASE=toevent
AUTH_TOKEN_SECRET=<segredo-aleatorio-forte>
CORS_ALLOWED_ORIGINS=http://localhost:5173
AUTH_COOKIE_SECURE=false
AUTH_COOKIE_SAME_SITE=Lax
TRUST_PROXY_HEADERS=false
SWAGGER_ENABLED=false
```

No PowerShell:

```powershell
Copy-Item .env-example .env
```

No Bash:

```bash
cp .env-example .env
```

Gere um segredo JWT forte (por exemplo, com `openssl rand -base64 48`) e URL-encode caracteres especiais na senha incluída na URI do MongoDB. Restrinja no MongoDB Atlas os endereços IP que podem conectar. **Não versione nem compartilhe `.env` ou credenciais.**

### Deploy no Render

Render é uma opção adequada para hospedar a aplicação. Se frontend e API forem serviços separados em domínios `onrender.com`, configure no serviço da API:

- `CORS_ALLOWED_ORIGINS`: URL exata do frontend, incluindo `https://`.
- `AUTH_COOKIE_SECURE=true` e `AUTH_COOKIE_SAME_SITE=None` para permitir o cookie de sessão entre os domínios.
- `TRUST_PROXY_HEADERS=true` para usar o IP encaminhado pelo proxy do Render no rate limit. Ative apenas quando a API só puder ser acessada pelo proxy confiável da plataforma.
- Configure o health check do serviço para `GET /health`; `/` também responde como rota pública de saúde.

Não use `*` em `CORS_ALLOWED_ORIGINS`. Se usar domínio próprio compartilhado entre frontend e API, ajuste `AUTH_COOKIE_SAME_SITE` de acordo com a origem e mantenha HTTPS.

## Executar localmente

### API

Com Java 17 e MongoDB configurados:

```powershell
.\mvnw.cmd spring-boot:run
```

No macOS/Linux:

```bash
./mvnw spring-boot:run
```

A API fica disponível em `http://localhost:8080`. O Swagger está desativado por padrão; para habilitá-lo localmente, defina `SWAGGER_ENABLED=true` no `.env`. Quando habilitado, acesse `http://localhost:8080/swagger-ui/index.html`.

### Frontend

Em outro terminal:

```powershell
cd frontend
npm ci
npm run dev
```

O Vite informa o endereço local no terminal (normalmente `http://localhost:5173`). Para apontar o frontend a uma API em outro endereço, defina `VITE_API_URL` no ambiente do Vite ou em `frontend/.env`, por exemplo:

```text
VITE_API_URL=http://localhost:8080
```

O arquivo `.env` da raiz configura a API; `frontend/.env` configura o Vite. Mantenha segredos apenas no ambiente do servidor — variáveis `VITE_*` são incluídas no frontend público.

## Executar a API com Docker Compose

O Compose inicia somente a API; é necessário fornecer um MongoDB externo configurado em `.env`.

```bash
docker compose config
docker compose up --build
```

A API ficará disponível em `http://localhost:8080`. Para parar:

```bash
docker compose down
```

## API

Cadastro e login são públicos. Após o login, a API envia um cookie JWT `HttpOnly`, válido por duas horas; o frontend o envia automaticamente. As demais rotas exigem uma sessão válida. O logout limpa o cookie.

| Método | Endpoint | Descrição |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Cria uma conta (`name`, `email`, `password`). |
| `POST` | `/api/auth/login` | Autentica (`email`, `password`) e define o cookie de sessão. |
| `POST` | `/api/auth/logout` | Encerra a sessão limpando o cookie. |
| `GET` | `/api/users` | Retorna os dados da conta autenticada. |
| `DELETE` | `/api/users` | Exclui a conta autenticada. |
| `GET` | `/api/events` | Lista os eventos da conta autenticada. |
| `GET` | `/api/events/{eventId}` | Consulta um evento da conta autenticada. |
| `POST` | `/api/events` | Cria um evento (`eventName`, `eventDate`, `description` opcional). |
| `PUT` | `/api/events/{eventId}` | Atualiza um evento da conta autenticada. |
| `DELETE` | `/api/events/{eventId}` | Exclui um evento da conta autenticada. |

Use `YYYY-MM-DD` para `eventDate`. As operações de evento verificam a propriedade do recurso para impedir acesso cruzado entre contas.

## Testes e validações

Na raiz:

```powershell
.\mvnw.cmd test
```

Para validar o frontend:

```powershell
cd frontend
npm run lint
npm run build
```

## Limitações e cuidados antes de produção

- **Premium não está implementado:** a página é ilustrativa, e não há fluxo de assinatura ou forma de alterar o status Premium.
- **Exclusão de conta:** excluir a conta também exclui os eventos associados ao usuário.
- **Proteção contra abuso:** o rate limit é mantido em memória e não compartilha estado entre instâncias. Por padrão, o IP vem da conexão direta; configure `TRUST_PROXY_HEADERS=true` somente atrás de um proxy confiável que define os cabeçalhos encaminhados. No Render, configure também rate limit compartilhado se escalar para várias instâncias.
- **Armazenamento de sessão:** o JWT fica em cookie `HttpOnly`, não acessível diretamente por JavaScript. As alterações autenticadas por cookie validam a origem; ainda assim, evite injeção de conteúdo e adote proteção contra XSS.
- **Validação de eventos:** a API limita o nome a 120 caracteres e a descrição a 500, além das validações do formulário.
- **Operação:** use HTTPS, restrinja `CORS_ALLOWED_ORIGINS` às origens exatas da aplicação, proteja o segredo JWT e as credenciais do banco, e planeje backups e exclusão de dados.

## Build in public

Este repositório está em evolução. Sugestões, relatos de bugs e contribuições são bem-vindos.
