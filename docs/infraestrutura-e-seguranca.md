# Infraestrutura e Segurança

> 🟢 MVP (bloqueia lançamento) · 🔵 Fase seguinte (implementa quando a dor aparecer) · ⚠️ pendente de decisão

## Autenticação 🟢

- **Firebase Authentication** (modo padrão, sem upgrade para Identity Platform neste momento).
- Firebase Authentication guarda a credencial e executa o fluxo de login; app **nunca** vê/armazena senha.
- `users.id` = `sub`/`uid` do token emitido pelo Firebase Authentication, armazenado como string (não gerar id próprio, não ter coluna de mapeamento separada).
- Provisionamento **just-in-time**: primeira request autenticada de um user novo cria a linha em `users` na hora, a partir do token validado.
- Separar projeto/credenciais de `dev` e `prod` no Firebase desde cedo. Não reutilizar configuração de produção em ambiente local.
- Quando a integração real virar pré-requisito, orientar o usuário sobre criação de projeto, apps, métodos de login, service account, restrições e secrets antes de fechar a entrega.
- Ordem pragmática permitida: backend pode evoluir primeiro em domínio, banco, contratos e regras de negócio com auth controlada de `local`/`test`; integração real com Firebase continua obrigatória antes de fechar MVP funcional end-to-end ou publicar clientes.

## Hospedagem 🟢

- **VPS única** (Hostinger, linha KVM, datacenter São Paulo) rodando Spring Boot + Postgres via **Docker Compose**. Não usar PaaS por serviço (Railway/Render) neste desenho atual.
- Descartados: Oracle Cloud Free Tier (instabilidade/limite reduzido), Hetzner (EUR + datacenter EU).
- App deve ser **stateless** (sem estado em memória local, config via env var) — pré-requisito barato pra escalar depois sem reescrever.
- 🔵 Load balancer/API Gateway: não implementar agora. Evolução: Postgres gerenciado → Nginx como LB simples → API Gateway só se virar multi-serviço de verdade.

## Segurança 🟢

- DDoS: Cloudflare (free) como proxy DNS.
- Rate limit: Cloudflare (free) + Nginx (`limit_req`) + Bucket4j (por usuário/endpoint no código).
- **Nunca confiar em campo de plano/permissão vindo do cliente** (ex: `isPremium`) — checar sempre contra o banco.
- Pagamento: status de assinatura só muda via **webhook assinado** do provedor, nunca por chamada do client dizendo "paguei".
- Autorização por recurso em todo endpoint (`session.user_id == usuário autenticado`, não só "está logado").
- Tokens do Firebase devem ser validados no backend; nunca confiar em `uid`, `email`, plano ou papel enviados pelo client fora do token validado + banco.
- SQL Injection: mitigado por padrão via Spring Data JPA (queries parametrizadas); risco só em query nativa concatenada.
- HTTPS obrigatório (Let's Encrypt). Segredos só em env var, nunca no código.
- Mobile: token em Keychain/Keystore nativo; API key do Google Places restrita por package/bundle ID + assinatura; nenhum segredo real embutido no app.
- 🔵 Certificate pinning e detecção de root/jailbreak: não agora.

## Configuração por ambiente 🟢

- Perfis mínimos obrigatórios: `local` e `prod`.
- `application.yaml` fica como base neutra, sem segredo real.
- `application-local.yaml` suporta desenvolvimento local, com defaults seguros e integrações opcionais temporariamente vazias.
- `application-prod.yaml` assume variáveis obrigatórias para produção e não deve depender de fallback inseguro.
- OpenAPI/Swagger:
  - `local`: habilitado por padrão (`SPRINGDOC_ENABLED=true`).
  - `prod`: desabilitado por padrão (`SPRINGDOC_ENABLED=false`) e só exposto quando houver necessidade explícita.
- Nunca usar a mesma credencial/projeto Firebase, R2 ou Google Places entre `dev` e `prod`.
- Nenhum secret, JSON de service account, token, private key ou senha deve ser commitado em `.env`, compose, código, docs ou examples com valor real.

### Variáveis de ambiente por tipo

#### Base local

- `SPRING_PROFILES_ACTIVE=local`
- `SPRINGDOC_ENABLED=true`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `CORS_ALLOWED_ORIGINS`

#### Integrações opcionais enquanto o backend central evolui

- `FIREBASE_PROJECT_ID`
- `FIREBASE_SERVICE_ACCOUNT_JSON_BASE64` ou `FIREBASE_SERVICE_ACCOUNT_PATH`
- `R2_ENDPOINT`
- `R2_BUCKET`
- `R2_ACCESS_KEY`
- `R2_SECRET_KEY`
- `GOOGLE_PLACES_API_KEY`
- `SENTRY_DSN`
- `BREVO_SMTP_USERNAME`
- `BREVO_SMTP_PASSWORD`

#### Produção mínima

- `SPRING_PROFILES_ACTIVE=prod`
- `SPRINGDOC_ENABLED=false`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `CORS_ALLOWED_ORIGINS`
- `FIREBASE_PROJECT_ID`
- `FIREBASE_SERVICE_ACCOUNT_JSON_BASE64` ou `FIREBASE_SERVICE_ACCOUNT_PATH`

#### Produção quando a feature correspondente estiver ativa

- Fotos R2: `R2_ENDPOINT`, `R2_BUCKET`, `R2_ACCESS_KEY`, `R2_SECRET_KEY`
- Localização Google Places: `GOOGLE_PLACES_API_KEY`
- Erros em produção: `SENTRY_DSN`
- E-mail transacional próprio: `BREVO_SMTP_USERNAME`, `BREVO_SMTP_PASSWORD`

## Gestão de secrets 🟢

- `local`:
  - usar shell local, `.env` não versionado ou mecanismo equivalente fora do git;
  - manter `.env.example` só com placeholders;
  - se precisar service account do Firebase, preferir arquivo local fora do repositório ou variável base64 injetada só no ambiente do dev.
- `prod`:
  - usar env vars do servidor/compose final ou secret file fora do repositório;
  - restringir acesso aos secrets ao menor conjunto de usuários/processos possível;
  - separar credenciais por ambiente e por serviço;
  - preparar rotação sem alteração de código.
- Logs e erros:
  - não logar bearer token, API key, DSN com credencial, senha SMTP, segredo R2 ou JSON completo de service account;
  - erros devem falhar de forma segura sem despejar config sensível na resposta HTTP.

## E-mail transacional 🟢

- **Brevo**: 300 e-mails/dia, free permanente, sem cartão.
- Não é pré-requisito para o login básico no Firebase Authentication.
- Usar quando o produto realmente precisar enviar e-mail transacional próprio (ex: comunicação operacional, alertas, notificações fora do auth).
- Não montar SMTP próprio: problema não é código, é reputação de IP (spam por padrão).
- Depende de domínio próprio + registros SPF/DKIM/DMARC.

## Notificação push 🔵 (depende do mobile existir)

- **FCM** (Firebase Cloud Messaging) — grátis, ilimitado, unifica iOS/Android.
- Fluxo: app registra token de dispositivo → backend salva (`user_devices`) → backend chama API do FCM quando notificar.
- Uso: lembrete de abandono (premium), streak.

## Backup do banco 🟢 (bloqueia — risco real de perda de dado)

- `pg_dump` via cron diário → upload pro Cloudflare R2. Retenção rotativa (7 diários + 4 semanais).
- Backup semanal de VM da Hostinger **não substitui isso** (snapshot pode capturar estado inconsistente do Postgres).
- Testar restauração pelo menos 1x antes de precisar de verdade.

## Monitoramento 🟢 (esforço baixo, ativar desde já)

- **Sentry** plano Developer: grátis, 5k erros/mês, 1 usuário, inclui 1 uptime monitor.
- 🔵 Métricas de negócio/tráfego: Spring Actuator + Micrometer (nativo) → visualização via Prometheus + Grafana só quando sentir falta. Log estruturado via Grafana Loki, só se `docker compose logs` via SSH incomodar. Atenção: 6 containers na mesma VPS pode exigir upgrade de plano (KVM1 → KVM2).

## CI/CD 🔵 (não bloqueia 1º deploy manual, mas fácil/grátis, ativar cedo)

- **GitHub Actions**: grátis ilimitado em repo público; 2.000 min/mês grátis em privado.
- Pipeline simples: push → testes → SSH na VPS → `git pull && docker compose up -d --build`. Sem registry de imagem externo.
- Hostinger não substitui isso (deploy 1-clique da Hostinger é pra apps prontas de terceiro, não pra atualizar código próprio continuamente).

## Cadastros e contas operacionais ⚠️

- Quando qualquer integração sair do papel e virar dependência real de dev/prod, parar e orientar explicitamente o usuário sobre o cadastro/configuração necessária.
- Cadastros que provavelmente serão necessários neste projeto:
  - Projeto Firebase `dev` e `prod`, com método de login habilitado e service account para backend.
  - Conta Hostinger/VPS para produção.
  - Conta Cloudflare + zona do domínio.
  - Bucket/credenciais do Cloudflare R2.
  - Projeto Google Places com billing habilitado e chaves restritas.
  - Contas de loja mobile (Google Play e Apple) quando publicação virar escopo.
- Não assumir que cadastro externo já existe só porque a integração está documentada.

### Checklist operacional de cadastro/configuração

#### Firebase Authentication

- criar projeto `dev` e projeto `prod`;
- habilitar método(s) de login necessários;
- gerar service account para o backend;
- registrar apps clientes quando mobile/web real entrarem no fluxo;
- guardar `project_id` e credencial fora do git.

#### Hostinger / VPS

- provisionar VPS final;
- configurar acesso SSH seguro;
- instalar Docker e Docker Compose plugin;
- configurar firewall e HTTPS reverso antes de expor produção publicamente;
- definir onde as env vars/secrets de produção ficarão armazenadas.

#### Cloudflare / R2

- criar conta e zona DNS do domínio quando o nome do app fechar;
- criar bucket R2 separado por ambiente ou política equivalente de isolamento;
- gerar credenciais com menor privilégio possível;
- configurar domínio público/CDN das imagens só quando o fluxo de exibição estiver pronto.

#### Google Places

- criar projeto com billing habilitado;
- ativar Places API necessária;
- restringir chave por uso e revisar escopo;
- manter chave separada por ambiente se necessário.

## Domínio/DNS ⚠️ pendente (nome do app não decidido)

Passo a passo: registrar domínio → apontar nameservers pro Cloudflare → criar registros (VPS, `api.dominio`, `app.dominio` se necessário, SPF/DKIM/DMARC do Brevo quando houver e-mail transacional próprio).

| Registrador | Preço/ano | WHOIS privado | |
|---|---|---|---|
| Registro.br (.com.br) | R$40–96 | Não | Exige CPF/CNPJ, mais confiança BR |
| Cloudflare Registrar (.com/.app) | ~US$9–10 (custo) | Sim, grátis | Mesmo painel do Cloudflare |
| Revendedoras BR | R$25–96 | Varia | Pix/boleto, suporte PT-BR |

## Analytics de produto 🔵 (não bloqueia MVP com poucos usuários conhecidos)

- **PostHog** — free tier generoso, self-hosted opcional (mesma VPS). Cobre funil/retenção/coorte — diferente de Sentry (erro) e Prometheus (infra).

## Pagamento ⚠️ pendente

| Provedor | Mensalidade | Taxa recorrência | |
|---|---|---|---|
| Stripe | Não | — | Aceita BR oficialmente, elegibilidade de conta tem nuances |
| Asaas | Não | ~2,99% + R$0,49 | Favorito levantado, API simples |
| Mercado Pago | Não | ~3,19–4,99% | Maior confiança de marca no BR |

Descartados: Iugu (mensalidade fixa mesmo sem venda), Pagar.me (over-spec pro caso atual).

## Resumo — bloqueia MVP

🟢 Auth · Hospedagem · Segurança · Backup · Monitoramento · Domínio (pendente) · Pagamento (pendente)
🔵 E-mail transacional próprio · Push · Métricas/Prometheus/Grafana/Loki · CI/CD · Analytics de produto
