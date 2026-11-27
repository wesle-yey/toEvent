# toEvent

Aplicação web para organizar eventos pessoais. O projeto reúne uma interface React e uma API REST em Spring Boot, com MongoDB para persistência e autenticação por JWT.

> **Status:** MVP em desenvolvimento. Cadastro com confirmação de e-mail, login e gerenciamento de eventos estão implementados. A página Premium é apenas demonstrativa: ainda não há compra, upgrade ou cobrança.

## Funcionalidades

- Cadastro com confirmação de e-mail (link válido por 24 horas, com reenvio) e login automático ao confirmar.
- Bloqueio de cadastro com e-mails temporários/descartáveis.
- Login permitido apenas para contas com e-mail confirmado.
- Criação, consulta, edição e exclusão de eventos com data e descrição.
- Eventos associados à conta autenticada.
- Limite de cinco eventos para contas não Premium.
- Interface responsiva em português.

## Tecnologias

- Java 17 e Spring Boot
- Spring Security, JWT e validação de dados
- MongoDB
- Brevo (API HTTP) para envio de e-mails
- React, Vite e Axios
- Docker Compose (opcional, para executar a API)

## Requisitos

- Java 17 ou mais recente para executar e testar a API localmente. O Maven Wrapper usa o Java de `JAVA_HOME` (ou do `PATH`); com Java 8 a compilação e os testes falham.
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
BREVO_API_KEY=<chave-da-api-brevo>
BREVO_SENDER_EMAIL=<remetente-verificado-no-brevo>
BREVO_SENDER_NAME=toEvent
FRONTEND_URL=http://localhost:5173
MAIL_DAILY_LIMIT=250
```

`FRONTEND_URL` é usada para montar o link de confirmação enviado por e-mail. `MAIL_DAILY_LIMIT` é o máximo de e-mails por dia (UTC); mantenha-o abaixo da cota do plano Brevo (300/dia no gratuito). Se `BREVO_API_KEY` ou `BREVO_SENDER_EMAIL` estiverem vazias, nenhum e-mail é enviado: o link de confirmação aparece no log da API, o que facilita o desenvolvimento local.

No PowerShell:

```powershell
Copy-Item .env-example .env
```

No Bash:

```bash
cp .env-example .env
```

Gere um segredo JWT forte (por exemplo, com `openssl rand -base64 48`) e URL-encode caracteres especiais na senha incluída na URI do MongoDB. Restrinja no MongoDB Atlas os endereços IP que podem conectar. **Não versione nem compartilhe `.env` ou credenciais.**

### Deploy (frontend na Vercel, API no Render)

O frontend chama a API pelo **mesmo domínio** em que é servido: o `frontend/vercel.json` encaminha `/api/*` para a API no Render. Assim o cookie de sessão é first-party, funciona em navegadores que bloqueiam cookies de terceiros e o token nunca fica acessível ao JavaScript.

1. Em `frontend/vercel.json`, o destino do proxy é a URL do serviço da API no Render (`https://toevent.onrender.com`); ajuste-a se o serviço mudar.
2. Na Vercel, **não** defina `VITE_API_URL` (sem ela, o build de produção usa caminhos relativos e passa pelo proxy). Defina `PROXY_SHARED_SECRET` com um valor aleatório longo: o proxy envia esse valor no cabeçalho `X-Origin-Secret` em toda chamada a `/api/*`.
3. No serviço da API no Render, configure:
   - `CORS_ALLOWED_ORIGINS`: URL exata do frontend, incluindo `https://` (ex.: `https://to-event.vercel.app`). Ela também é usada para validar a origem de requisições autenticadas por cookie.
   - `FRONTEND_URL`: a mesma URL do frontend, para os links de confirmação de e-mail.
   - `AUTH_COOKIE_SECURE=true` e `AUTH_COOKIE_SAME_SITE=Lax`.
   - `PROXY_SHARED_SECRET`: o mesmo valor configurado na Vercel. Com ele, `/api/*` recusa com `403` qualquer chamada que não veio pelo proxy, então ninguém consegue chamar o Render direto e forjar o `X-Forwarded-For`. Vazio, o bloqueio fica desligado (use só em desenvolvimento).
   - `TRUST_PROXY_HEADERS=true` para usar o IP do cliente (primeiro valor de `X-Forwarded-For`) no rate limit. Ative apenas junto com `PROXY_SHARED_SECRET`, para que a API só aceite chamadas vindas do proxy da Vercel.
   - Health check em `GET /health`; `/` também responde como rota pública de saúde.

Não use `*` em `CORS_ALLOWED_ORIGINS`.

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

O Vite informa o endereço local no terminal (normalmente `http://localhost:5173`). Em desenvolvimento, o frontend usa a API em `http://localhost:8080` por padrão. Para apontar a uma API em outro endereço, defina `VITE_API_URL` no ambiente do Vite ou em `frontend/.env`, por exemplo:

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

Cadastro, login e confirmação de e-mail são públicos. Após o login (ou a confirmação do e-mail), a API envia um cookie JWT `HttpOnly`, válido por duas horas; o frontend o envia automaticamente. As demais rotas exigem uma sessão válida. O logout limpa o cookie.

O token também é devolvido no corpo da resposta e aceito no cabeçalho `Authorization: Bearer <token>`, para clientes da API que não usam cookies (scripts, Swagger). O frontend não o armazena.

| Método | Endpoint | Descrição |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Cria uma conta (`name`, `email`, `password`) e envia o e-mail de confirmação. Se o e-mail já tiver uma conta **não confirmada**, substitui nome e senha e envia um novo link. |
| `POST` | `/api/auth/verify-email` | Confirma o e-mail (`token` do link). Só entra automaticamente (`autoLogin: true`) no navegador que pediu o link; em outro, é preciso fazer login. |
| `POST` | `/api/auth/resend-verification` | Reenvia o link de confirmação (`email`). Responde igual exista ou não a conta. |
| `POST` | `/api/auth/login` | Autentica (`email`, `password`) e define o cookie de sessão. Retorna `403` com `EMAIL_NOT_VERIFIED` se o e-mail não foi confirmado. |
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
- **Envio de e-mails:** cada conta recebe no máximo 1 link de confirmação por minuto e 5 a cada 24 horas, e o total diário é limitado por `MAIL_DAILY_LIMIT` (contador no MongoDB). Contas não confirmadas são apagadas após 7 dias por um índice TTL do MongoDB. Ainda não há CAPTCHA no cadastro.
- **Proteção contra abuso:** o rate limit é mantido em memória e não compartilha estado entre instâncias. Por padrão, o IP vem da conexão direta; configure `TRUST_PROXY_HEADERS=true` somente atrás de um proxy confiável que define os cabeçalhos encaminhados. No Render, configure também rate limit compartilhado se escalar para várias instâncias.
- **Armazenamento de sessão:** o frontend guarda a sessão apenas no cookie `HttpOnly`, não acessível por JavaScript. As alterações autenticadas por cookie validam a origem; ainda assim, evite injeção de conteúdo e adote proteção contra XSS.
- **Validação de eventos:** a API limita o nome a 120 caracteres e a descrição a 500, além das validações do formulário.
- **Operação:** use HTTPS, restrinja `CORS_ALLOWED_ORIGINS` às origens exatas da aplicação, proteja o segredo JWT e as credenciais do banco, e planeje backups e exclusão de dados.

## Build in public

Este repositório está em evolução. Sugestões, relatos de bugs e contribuições são bem-vindos.
