# Relatório de status e operação do MVP

Data da revisão: **2026-07-20**.

Este documento separa o que está implementado no repositório do que ainda depende de conta, credencial, infraestrutura ou decisão do responsável pelo produto. Nenhum valor real de secret deve ser registrado aqui.

## Estado atual

O backend central da Fase 0 está implementado: perfil próprio/público por username, hobbies, catálogo oficial, sessões paginadas com `everyone`/`only_me`, atributos dinâmicos em JSONB, equipamentos, backlog/Kanban, streak, localização por `place_id`, uma foto por sessão via presigned upload, processamento WebP/thumbnail sem EXIF e documentação OpenAPI.

A aplicação possui autorização por proprietário do recurso, provisionamento JIT de usuário, rate limit básico, configuração `local`/`prod`, Flyway, Postgres, Compose local e de produção, Nginx/TLS, scripts de backup/restauração e health/readiness das integrações habilitadas.

O MVP **ainda não está pronto para lançamento público** porque faltam validar Firebase, R2 e Google Places com contas reais, provisionar a VPS/domínio/TLS, ativar monitoramento e executar um teste real de backup/restauração.

## Entregas concluídas nesta revisão

- Feature flags operacionais para upload de fotos, processamento e localização, com `GET /api/features` autenticado e falha segura HTTP 503.
- Health de produção fica `DOWN` quando Firebase ou uma integração habilitada está sem configuração, sem expor valores.
- Catálogo oficial em `GET /api/hobbies` para o client descobrir IDs válidos.
- Paginação de sessões com ordenação estável e limite máximo de página.
- Processamento assíncrono de JPEG/PNG/WebP via `cwebp`: variante de até 2048 px, thumbnail de até 480 px e remoção de metadata/EXIF por re-encode.
- Fotos possuem estado `pending`, `ready` ou `failed`, no máximo três tentativas de processamento e erro técnico sem mensagem sensível.
- Chaves temporárias são namespaced pelo UID autenticado; o backend rejeita key alheia, repetida ou acima do limite de uma foto.
- Edição de sessão preserva fotos quando `photos` é omitido; lista vazia remove todas; IDs existentes e novas keys podem ser combinados sem aceitar IDOR/BOLA.
- Exclusão de foto/sessão enfileira a remoção dos objetos R2 na mesma transação do banco; worker idempotente executa cleanup com retry/backoff.
- Mídia `only_me` fica no bucket privado e usa GET presigned; mídia `everyone` usa bucket público/CDN. Mudança de visibilidade move variantes e purga cache ao tornar privada.
- O Compose local usa Adobe S3Mock 5.1.0 com volume persistente, sem depender de conta R2.
- Token bearer validado não é retido como credencial no `SecurityContext`; erros internos de Firebase não são refletidos ao client.
- Contrato de backlog valida que projeto com hobby definido pertence ao mesmo hobby da sessão.

## Feature flags

| Variável | `local` padrão | `prod` padrão | Quando ativar |
|---|---:|---:|---|
| `FEATURE_PHOTO_UPLOADS_ENABLED` | `false` | `true` | somente após bucket e credenciais R2 funcionarem |
| `FEATURE_PHOTO_PROCESSING_ENABLED` | `false` | `true` | junto com upload; exige R2 e `cwebp` disponível |
| `FEATURE_SESSION_LOCATION_ENABLED` | `false` | `true` | após Places API e chave restrita funcionarem |

Perfil, hobbies, sessões básicas, equipamentos, backlog e streak não têm flag: são o núcleo coerente do MVP. Flags são configuração do servidor, nunca permissão enviada pelo client. Em produção, não habilitar upload com processamento desligado; o readiness detecta essa combinação inválida.

## Secrets e configurações necessárias

Os exemplos seguros estão em `.env.example` e `deploy/production.env.example`. Valores abaixo descrevem **o que preencher**, não valores que devem ser copiados literalmente.

| Variável | Secret? | Origem/formato esperado | Obrigatória |
|---|---:|---|---|
| `POSTGRES_DB` | não | nome do banco, por exemplo um identificador próprio do ambiente | local/prod |
| `POSTGRES_USER` | não | usuário dedicado da aplicação | local/prod |
| `POSTGRES_PASSWORD` | sim | senha aleatória forte e exclusiva | local/prod |
| `DB_URL` | não | JDBC `jdbc:postgresql://host:5432/database`; no Compose prod é montada internamente | execução fora do Compose |
| `DB_USERNAME` | não | mesmo usuário dedicado do banco | execução fora do Compose |
| `DB_PASSWORD` | sim | mesma credencial injetada para a aplicação | execução fora do Compose |
| `FIREBASE_PROJECT_ID` | não | project ID do Firebase do ambiente | Firebase real/prod |
| `FIREBASE_SERVICE_ACCOUNT_JSON_BASE64` | sim | JSON da service account codificado em Base64, em uma única linha | Firebase real/prod, alternativa ao path |
| `FIREBASE_SERVICE_ACCOUNT_PATH` | sensível | path de arquivo montado fora do repositório, com permissão restrita | alternativa ao Base64 |
| `LOCAL_AUTH_TOKEN` | sim | token aleatório forte, apenas para profile `local` | somente se `LOCAL_AUTH_ENABLED=true` |
| `R2_ENDPOINT` | não | endpoint S3 do account Cloudflare R2 | fotos/backup |
| `R2_PRIVATE_BUCKET` | não | bucket privado isolado por ambiente | uploads temporários e fotos `only_me` |
| `R2_PUBLIC_BUCKET` | não | bucket público isolado por ambiente | variantes processadas `everyone` |
| `R2_PUBLIC_BASE_URL` | não | domínio customizado/CDN do bucket público | URLs estáveis de mídia pública |
| `R2_BUCKET` | não | bucket privado separado dos buckets de fotos | script de backup PostgreSQL |
| `R2_ACCESS_KEY` | sim | access key de token R2 com menor privilégio | fotos/backup |
| `R2_SECRET_KEY` | sim | secret da credencial R2 | fotos/backup |
| `CLOUDFLARE_ZONE_ID` | não | id da zona do domínio de mídia | purge ao tornar foto privada |
| `CLOUDFLARE_API_TOKEN` | sim | token limitado a Cache Purge da zona | purge ao tornar foto privada |
| `GOOGLE_PLACES_API_KEY` | sim | chave server-side restrita à Places API e ao ambiente | localização ativa |
| `SENTRY_DSN` | sensível | DSN do projeto backend | quando o SDK de monitoramento for ativado |
| `CORS_ALLOWED_ORIGINS` | não | origens exatas separadas por vírgula, nunca `*` em prod | prod |
| `APP_DOMAIN` | não | hostname da API, sem protocolo | prod |
| `NGINX_CERTS_DIR` | sensível operacional | diretório host fora do git com certificado e chave | prod |
| `SSL_CERTIFICATE_PATH` | não | path do certificado dentro do container Nginx | prod |
| `SSL_CERTIFICATE_KEY_PATH` | sensível operacional | path da private key dentro do container Nginx | prod |

Também existem ajustes não secretos de capacidade: `RATE_LIMIT_*`, `PHOTO_PROCESSING_POLL_DELAY_MS`, `PHOTO_DELETION_POLL_DELAY_MS`, `NGINX_RATE_LIMIT_*` e retenção `BACKUP_*`.

Não usar os placeholders `change-me`/`example.com` em produção. O arquivo real de produção deve ficar fora do repositório, idealmente `/opt/hobby-saas/prod.env`, com permissão `600`.

## Cadastros e validações externas pendentes

### Firebase Authentication

1. Criar projetos separados de desenvolvimento e produção no Firebase Console.
2. Habilitar os provedores de login escolhidos no produto; e-mail/senha pode ser o primeiro.
3. Registrar cada app cliente com package/bundle ID definitivo.
4. Criar uma service account exclusiva do backend por ambiente e aplicar menor privilégio.
5. Injetar project ID e credencial fora do git.
6. Obter um ID token pelo client/Emulator e validar autenticação, token expirado/inválido e provisionamento JIT contra o backend.

Até isso acontecer, `LOCAL_AUTH_*` serve somente ao desenvolvimento. Ele não é carregado no profile `prod`.

### Cloudflare R2

1. Criar buckets público e privado separados em cada ambiente; nunca usar o bucket público para upload cru ou backup.
2. Conectar domínio customizado/CDN somente ao bucket público e criar token restrito a Cache Purge da zona.
3. Criar credencial S3 com acesso somente aos dois buckets/prefixos necessários.
4. Preencher endpoint, buckets, base URL, access key, secret, zone id e token de purge no ambiente.
5. Validar presigned `PUT` com `Content-Type` e `Content-Length` assinados.
6. Validar upload privado, processamento, leitura privada/CDN, alternância de visibilidade com purge e exclusão.
7. Configurar lifecycle para uploads temporários abandonados, como proteção adicional.

### Google Places

1. Criar projeto Google Cloud do ambiente e habilitar billing/Places API.
2. Criar chave server-side restrita à API necessária e revisar quotas.
3. Validar Place Details com FieldMask Essentials, cache local e comportamento de falha.

### Hostinger, domínio e Cloudflare

1. Fechar nome/domínio e provisionar a VPS Hostinger.
2. Instalar Docker/Compose e ferramentas de backup; configurar SSH por chave e firewall.
3. Apontar DNS pelo Cloudflare, emitir/montar certificados TLS e preencher `APP_DOMAIN`/paths.
4. Subir `docker-compose.prod.yml` com env file externo; expor apenas Nginx, não Spring/Postgres.
5. Configurar cron diário para backup e testar restauração em banco isolado.

## Pendências antes do lançamento

- [ ] Validar Firebase real em dev e prod, inclusive JIT e tokens inválidos/expirados.
- [ ] Validar R2 real end-to-end, inclusive processamento e fila de exclusão.
- [ ] Validar Google Places real e custo/quota configurados.
- [ ] Escolher nome/domínio e concluir DNS/TLS/Cloudflare/Hostinger.
- [ ] Trocar todos os placeholders por secrets fortes fora do git.
- [ ] Executar backup real, restaurar em banco isolado e registrar evidência/data.
- [ ] Adicionar o SDK real do Sentry ou escolher outro monitor; hoje existe configuração/placeholder, não envio de eventos.
- [ ] Criar alerta externo de uptime/readiness.
- [ ] Executar smoke test completo no ambiente de produção antes de abrir tráfego.
- [ ] Definir a enumeração inicial de categorias de equipamento.

Não bloqueiam este backend MVP: provedor de pagamento, lojas mobile e e-mail transacional próprio. Pagamento continua fora do código até a escolha explícita do provedor.

## Decisões ainda abertas

- Nome do app e domínio.
- Enumeração curada das categorias de equipamento.
- Provedor de pagamento para fase posterior.

Já fechadas: Firebase Authentication padrão, VPS Hostinger, Postgres, Cloudflare R2, Google Places, JSONB nativo do Hibernate e processamento com `cwebp/libwebp`.

## Validação técnica desta revisão

- `./mvnw clean install`: **74 testes**, zero falhas/erros, incluindo domínio, contrato, segurança e integração.
- Testes de integração exercitam PostgreSQL real via Testcontainers e migrations Flyway.
- Build do pacote Maven executado sem testes após a suíte.
- Imagem Docker construída e iniciada contra PostgreSQL 17 local.
- Health respondeu `UP`; rota protegida sem bearer respondeu HTTP 401; `cwebp` foi encontrado na imagem.
- Varredura de padrões óbvios de secret disponível em `scripts/check-no-secrets.sh`.

Comandos de aceite final:

```bash
./mvnw clean install
./scripts/check-no-secrets.sh
docker compose up -d --build
docker compose ps
curl -fsS http://localhost:8080/actuator/health
```

No momento desta revisão, o daemon Docker local manteve o container antigo `hobby-saas-app` e os containers temporários `hobby-saas-app-validation*`; as tentativas de parada/remoção não tiveram efeito e uma delas respondeu `permission denied`. A migration V5 e o smoke test foram validados no temporário da porta `18081`; a imagem final também foi reconstruída depois do último ajuste de segurança. Para o Compose principal adotar essa imagem e limpar os temporários, reiniciar/corrigir o daemon Docker e executar novamente `docker compose up -d --build`. Isso é um problema do daemon local, não da aplicação nem do volume Postgres.
