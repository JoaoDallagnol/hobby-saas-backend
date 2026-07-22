# AGENTS.md

## Produto
App multi-hobby de tracking e gerenciamento (tipo Strava/Letterboxd, mas não nichado em um hobby só). Usuário registra sessões de qualquer hobby e acompanha evolução num só lugar.

Fase atual: **MVP / Fase 0 do roadmap**.
Objetivo do MVP: validar hábito de registro antes de qualquer efeito de rede ou monetização avançada.

Escopo funcional do MVP:
- Perfil do hobbista (`users`, `user_hobbies`, username público único, bio, nível/experiência por hobby), com leitura autenticada de outro perfil por username.
- Tracker de sessão (`sessions` + `session_photos`), incluindo título, data, duração, notas, satisfação, no máximo uma foto, visibilidade `everyone`/`only_me` e vínculo opcional com lugar.
- Atributos dinâmicos por hobby via template + JSONB.
- Biblioteca de equipamentos (`equipment` + `session_equipment`) para cadastro e reutilização por sessão.
- Backlog/Kanban por hobby (`backlog_items`) com vínculo opcional da sessão a um item/projeto.
- Streak de constância.

Fora do MVP por padrão:
- Feed social, seguidores, Hobby Buddy e Heatmap são **Fase 2** e não devem ser implementados/expostos sem massa crítica real de usuários.
- Gamificação pessoal e fundação Plus possuem implementação antecipada autorizada, mas rollout continua posterior à validação end-to-end do MVP; detalhes em `docs/gamificacao-e-planos.md`.
- Cobrança, IA, múltiplas mídias, família, desafios sociais e demais itens posteriores continuam fora de escopo até decisão explícita e pré-requisitos próprios.

## Stack
Java 25, Spring Boot 4.1.x, Maven, Postgres, Flyway, Firebase Authentication (auth), Cloudflare R2 (fotos), Docker Compose numa VPS única na Hostinger. Detalhe completo: `docs/stack.md`.

## Docs de referência (ler sob demanda, não de uma vez)
- `docs/roadmap.md` — o que estamos construindo e em que ordem (fases, o que cada feature depende).
- `docs/funcionalidades.md` — descrição funcional mais detalhada de cada fase/feature; usar para entender intenção de produto, comportamento esperado e exemplos de uso.
- `docs/diretrizes-tecnicas.md` — modelo de sessão, atributos dinâmicos, equipamento, localização, fotos, taxonomia de hobbies.
- `docs/modelagem-banco-dados.md` — schema completo (diagramas + dicionário de dados).
- `docs/infraestrutura-e-seguranca.md` — auth, hospedagem, segurança, serviços de suporte.
- `docs/stack.md` — dependências e libs.
- `docs/gamificacao-e-planos.md` — regras Free/Plus, metas, XP, badges, recordes, retrospectiva, planejamento e manutenção.
- `docs/gamification-plus-execution-checklist.md` — status executável da gamificação e fundação Plus; não confundir com o checklist do MVP.
- `docs/api-acceptance-testing.md` — tutorial, cobertura e diagnóstico da collection Postman/Postman CLI.
- `docs/estrategia-de-cache.md` — matriz de cache, invalidação, regras de Places e entrega CDN/R2.
- `docs/cadastros-e-configuracoes-externas.md` — runbook das contas, credenciais e validações manuais fora do repositório.

## Leitura mínima antes de mudar algo
- Sempre alinhar a mudança com `docs/roadmap.md`.
- Se mexer em funcionalidade, fluxo do usuário, comportamento esperado da feature, contrato de API orientado ao produto ou priorização de escopo, conferir `docs/funcionalidades.md`.
- Se mexer em entidade ou fluxo de negócio, conferir `docs/diretrizes-tecnicas.md`.
- Se mexer em gamificação, estatísticas, entitlement ou benefício Plus, conferir `docs/gamificacao-e-planos.md` e seu checklist próprio.
- Se mexer em persistência, conferir `docs/modelagem-banco-dados.md`.
- Se mexer em auth, deploy, storage, backup, e-mail, monitoramento ou pagamento, conferir `docs/infraestrutura-e-seguranca.md`; se afetar cadastro/validação manual em plataforma, conferir também `docs/cadastros-e-configuracoes-externas.md`.
- Se mexer em cache, CDN, retenção de dados de provedor ou invalidação, conferir `docs/estrategia-de-cache.md`.
- Se criar ou alterar endpoint/contrato, atualizar a collection Postman e conferir `docs/api-acceptance-testing.md`; para executar ou diagnosticar a suíte, ler `skills/run-api-acceptance/SKILL.md`.
- Nenhuma mudança de produto, schema, fluxo, fase do roadmap ou decisão técnica pode encerrar com documentação divergente. Se algo mudar, atualizar os arquivos afetados no mesmo trabalho.
- Toda mudança de funcionalidade, schema, contrato, fluxo ou priorização precisa passar por análise de impacto antes de fechar o trabalho: verificar se algo dependia disso em outra parte do produto, da implementação, do roadmap ou da documentação.
- Se uma funcionalidade for removida, promovida, rebaixada ou alterada, revisar explicitamente vínculos com APIs, banco, validações, checklist, roadmap e features que dependem dela.
- Se aparecer inconsistência entre documentos, reportar antes de editar e só consolidar a mudança depois da decisão explícita.
- Em estágio inicial, é aceitável implementar primeiro domínio, persistência, contratos e regras centrais da API antes de fechar integrações externas reais (auth, storage, Places, e-mail), desde que o desenho final dessas integrações permaneça preservado na arquitetura e na documentação.

## Regras não óbvias (ler sempre)
- `users.id` = `sub`/`uid` do Firebase Authentication, armazenado como string. Nunca gerar id próprio, nunca criar coluna de senha em `users`.
- Provisionamento de `users` é just-in-time na primeira request autenticada; dados-base vêm do token validado do Firebase.
- Durante desenvolvimento inicial, fluxos de negócio podem ser implementados e testados com autenticação controlada de ambiente local/teste antes da integração real com Firebase estar fechada. Isso não autoriza enfraquecer contrato de segurança nem remover a integração real do escopo do MVP.
- Atributo dinâmico por hobby sempre vai em `sessions.attributes` (JSONB), validado contra `hobby_attribute_template`. Nunca criar coluna nova pra atributo específico de hobby.
- `equipment.category` e `equipment.name` são colunas independentes do mesmo registro — não é chave/valor.
- Nunca confiar em campo de plano/permissão vindo do client (ex: `isPremium`) — checar sempre contra o banco.
- Status de pagamento só muda via webhook assinado do provedor, nunca por request do client.
- Feature flag de gamificação/Plus controla rollout, não entitlement. Ausência de assinatura ativa no banco equivale a Free; não criar endpoint público de auto-upgrade.
- XP é projeção de sessões com fórmula server-side por categoria; badges são concedidos pelo servidor; o client nunca declara XP/conquista.
- Exportação bruta dos próprios dados permanece Free e nunca inclui secrets, tokens, storage keys internas ou dados privados de terceiros.
- Localização: client manda `place_id` e um `label` de exibição tratado como conteúdo do usuário; backend valida apenas o ID via Google Place Details e persiste `place_id` + `validated_at`, nunca nome/endereço/coordenadas retornados pelo Google como cache permanente e nunca lat/lng vindo do client.
- Fotos: upload via presigned URL direto pro R2, nunca binário passando pelo backend.
- Fotos persistem só como storage key/URL no banco; processamento é assíncrono, com thumbnail/compressão e remoção de EXIF.
- Sessão aceita no máximo uma foto, mantendo `photos` como lista para preservar evolução futura do contrato.
- Visibilidade de sessão no MVP é enum `everyone` ou `only_me`, com default `only_me`; não reduzir a boolean. `followers` fica reservado para a Fase 2 e não pode ser aceito antes de existir grafo e autorização de seguidores.
- Perfil público é identificado por `users.username`, nunca pelo UID do Firebase. DTO público nunca expõe e-mail, UID, `place_id`, coordenadas, IDs de equipamento/projeto ou storage keys internas.
- Mídia processada de sessão `everyone` vai para escopo público/CDN; mídia `only_me` fica privada e é lida por URL GET pré-assinada. Upload temporário e original cru ficam sempre privados.
- Cache nunca concede autorização ou entitlement. Cache local deve ser limitado/expirável, conter só dado reconstruível e ter invalidação documentada após commit; ao escalar para múltiplas instâncias, revisar invalidação distribuída antes do rollout.
- Sessão usa `notes` como campo único de notas/reflexão; não recriar diário separado no MVP.
- Se `equipmentIds` estiverem ativos em algum fluxo, eles sempre referenciam biblioteca do usuário; nunca texto livre dentro da sessão.
- Modelos marcados como "conceitual" ou pertencentes a fases futuras na documentação não devem ser implementados sem revisão específica.
- Perfis de ambiente e configuração sensível devem nascer separados desde cedo (`local` e `prod` no mínimo), com valores vindos de env vars/secrets e nunca hardcoded no código.
- Nenhum secret, token, senha, private key, webhook secret ou credencial de provedor pode ir para o repositório, examples commitados ou documentação com valor real.
- Sempre que uma integração exigir conta, projeto, billing, credencial, domínio, app registration, chave ou configuração manual em plataforma externa, avisar explicitamente o usuário no momento em que isso virar pré-requisito para dev ou prod funcionar.
- Toda integração externa deve ser implementada assumindo revisão de segurança: validação de input, autorização por recurso, menor privilégio, rotação de secret possível, logs sem vazamento e falha segura.
- Ao expor endpoint novo, considerar documentação OpenAPI, autenticação/autorização, validação de payload, tratamento de erro e risco de vazamento de dados.
- Feature flag é configuração de rollout/operação, nunca autorização. Usar principalmente em integrações externas ou processamento assíncrono; não colocar flag em regra central do MVP sem decisão de produto.
- Ao criar feature flag, documentar default por ambiente, comportamento quando desligada, variável de ambiente e critério objetivo para ativação/remoção.
- Implementação deve evitar padrões que um pentest básico apontaria: segredo hardcoded, endpoint sem auth esperada, IDOR/BOLA, confiança em dado do client, log de credencial, CORS aberto sem motivo, stacktrace/sensitive data exposta e ausência de validação de input.

## Comandos
- Build: `mvn clean install`
- Testes: `mvn test`
- Local: `docker compose up -d` (sobe Spring Boot + Postgres; auth real usa Firebase via env e o profile `local` também permite bearer estático controlado por env)
- Aceite HTTP isolado: `./scripts/run-api-acceptance.sh`

## Não fazer
- Não tratar presença no schema como autorização para buildar feature de fase futura.
- Não implementar sessões em grupo ou onboarding de descoberta sem pedido explícito — são Fase 1, não MVP. Estatísticas pessoais e gamificação tiveram implementação antecipada autorizada, mas não alteram a definição de pronto do MVP.
- Não implementar/expor feed, Hobby Buddy, indicações locais ou Heatmap (Fase 2) sem pedido explícito e sem massa crítica real.
- Não usar MongoDB ou qualquer banco além do Postgres — domínio é relacional, flexibilidade já é resolvida via JSONB.
- Não implementar load balancer/API Gateway — fora de escopo até o monolito numa VPS não bastar mais.
- Não adicionar SDK/dependência de pagamento sem confirmar o provedor primeiro (ainda não decidido: Stripe/Asaas/Mercado Pago).
- Não seguir padrões da linha Spring Boot 3.x (EOL) — baseline é Jakarta EE 11 + Jackson 3.
- Não persistir/aceitar lat/lng vindos do client.
- Não receber/uploadar binário de foto pelo backend.
- Não criar sincronização própria de senha/credencial fora do Firebase Authentication.

## Pendências (não travam trabalho, mas não estão fechadas)
- Nome do app / domínio.
- Provedor de pagamento.
- Enumeração de categorias de equipamento.
