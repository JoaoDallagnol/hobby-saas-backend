# Cadastros e Configurações Externas

> Runbook operacional de contas, projetos, credenciais e passos manuais fora do repositório.

## Regra de uso

- Sempre que alguma integração abaixo virar pré-requisito real para `dev` ou `prod`, parar a implementação e orientar o usuário sobre o cadastro/configuração faltante.
- Nunca assumir que uma conta, projeto, bucket, domínio, certificado ou billing já existem.
- Nunca commitar credencial real, certificado real, token, chave privada ou arquivo de service account.

## 1. Firebase Authentication

Status no projeto:
- arquitetura definida;
- backend já preparado para `dev`/`prod`;
- integração real ainda depende de projeto/credencial válidos por ambiente.

### Ambiente `dev`

- criar projeto Firebase de desenvolvimento;
- habilitar método(s) de login necessários;
- gerar service account para o backend;
- guardar:
  - `FIREBASE_PROJECT_ID`
  - `FIREBASE_SERVICE_ACCOUNT_JSON_BASE64` ou `FIREBASE_SERVICE_ACCOUNT_PATH`

### Ambiente `prod`

- criar projeto Firebase separado de produção;
- repetir configuração de método(s) de login;
- gerar service account separada;
- nunca reutilizar projeto ou credencial de `dev`.

### Quando mobile entrar no fluxo

- registrar app Android;
- registrar app iOS;
- revisar fingerprints SHA do Android;
- revisar bundle id / app id do iOS;
- alinhar backend + app cliente com o mesmo projeto Firebase do ambiente correspondente.

## 2. Hostinger / VPS

Status no projeto:
- stack de produção já modelada em `docker-compose.prod.yml`;
- proxy/TLS já modelados em `deploy/nginx/default.conf.template`.

### Provisionamento mínimo

- contratar/provisionar a VPS final;
- configurar acesso SSH seguro;
- instalar:
  - Docker
  - Docker Compose plugin
  - `postgresql-client`
  - AWS CLI ou equivalente compatível com R2
- configurar firewall;
- preparar diretórios operacionais fora do repositório, por exemplo:
  - `/opt/hobby-saas/prod.env`
  - `/opt/hobby-saas/certs/`

### Deploy inicial esperado

- copiar o projeto para a VPS;
- copiar `deploy/production.env.example` para um arquivo real fora do git;
- ajustar permissões do arquivo de env (`chmod 600`);
- montar certificados TLS no path definido;
- subir com:
  - `docker compose --env-file /opt/hobby-saas/prod.env -f docker-compose.prod.yml up -d --build`

## 3. Cloudflare / DNS / R2

Status no projeto:
- Cloudflare está no desenho de borda;
- R2 já está no desenho de storage e backup;
- credenciais reais ainda não foram conectadas.

### Cloudflare / DNS

- criar conta Cloudflare;
- adicionar a zona do domínio;
- apontar nameservers do registrador;
- criar os registros DNS necessários;
- ativar proxy da borda conforme estratégia do domínio/API.

### R2

- criar dois buckets por ambiente: privado (upload temporário e `only_me`) e público (somente variantes processadas `everyone`);
- conectar domínio customizado/CDN apenas ao bucket público;
- criar token da zona limitado a Cache Purge para retirar mídia ao mudar para `only_me`;
- gerar credenciais com menor privilégio possível;
- guardar:
  - `R2_ENDPOINT`
  - `R2_PRIVATE_BUCKET`
  - `R2_PUBLIC_BUCKET`
  - `R2_PUBLIC_BASE_URL`
  - `R2_ACCESS_KEY`
  - `R2_SECRET_KEY`
  - `CLOUDFLARE_ZONE_ID`
  - `CLOUDFLARE_API_TOKEN`
- para o backup PostgreSQL, criar bucket privado separado e preencher `R2_BUCKET`; idealmente usar credencial própria com acesso somente a ele.

### Certificados

- obter certificado TLS válido para o domínio da API;
- montar `fullchain.pem` e `privkey.pem` no diretório configurado para o Nginx;
- nunca manter esses arquivos dentro do repositório.

## 4. Google Places

Status no projeto:
- backend já preparado para consumir Place Details;
- integração real depende de projeto, billing e chave válidos.

### Passos

- criar projeto Google Cloud do ambiente;
- habilitar billing;
- ativar Places API necessária;
- gerar chave;
- restringir por uso e escopo;
- guardar `GOOGLE_PLACES_API_KEY`.

## 5. Sentry

Status no projeto:
- integração preparada/configurável;
- ainda opcional enquanto não houver DSN real.

### Passos

- criar projeto no Sentry;
- obter DSN do ambiente;
- guardar `SENTRY_DSN`;
- revisar se logs/respostas não expõem payload sensível.

## 6. Brevo

Status no projeto:
- ainda não é pré-requisito do MVP atual;
- só configurar quando e-mail transacional próprio entrar de fato no escopo.

### Quando entrar

- criar conta;
- validar domínio;
- configurar SPF/DKIM/DMARC;
- gerar credenciais SMTP;
- guardar:
  - `BREVO_SMTP_USERNAME`
  - `BREVO_SMTP_PASSWORD`

## 7. Stores mobile

Status no projeto:
- fora de escopo operacional atual do backend;
- não marcar como concluído no checklist antes da fase de publicação.

### Quando entrar

- Google Play Console;
- Apple Developer Program;
- configuração de publicação;
- integração com apps móveis reais;
- revisão final de Firebase por app/ambiente.

## Resumo de variáveis por plataforma

| Plataforma | Variáveis / artefatos principais |
|---|---|
| Firebase | `FIREBASE_PROJECT_ID`, `FIREBASE_SERVICE_ACCOUNT_JSON_BASE64` ou `FIREBASE_SERVICE_ACCOUNT_PATH` |
| Hostinger/VPS | `prod.env`, certificados TLS, Docker/Compose instalados |
| Cloudflare/R2 | `APP_DOMAIN`, `R2_ENDPOINT`, `R2_PRIVATE_BUCKET`, `R2_PUBLIC_BUCKET`, `R2_PUBLIC_BASE_URL`, `R2_BUCKET` (backup), `R2_ACCESS_KEY`, `R2_SECRET_KEY`, `CLOUDFLARE_ZONE_ID`, `CLOUDFLARE_API_TOKEN` |
| Google Places | `GOOGLE_PLACES_API_KEY` |
| Sentry | `SENTRY_DSN` |
| Brevo | `BREVO_SMTP_USERNAME`, `BREVO_SMTP_PASSWORD` |
