# Gamificação, Retenção e Planos

> Especificação funcional e técnica das funcionalidades de retenção que não dependem de massa crítica. Este documento complementa `roadmap.md`, `funcionalidades.md`, `diretrizes-tecnicas.md` e `modelagem-banco-dados.md`.

## Objetivo e ordem de rollout

O código de gamificação e a fundação do plano Plus podem existir antes do lançamento, mas o rollout de produto só acontece depois de o fluxo central do MVP estar validado end-to-end com Firebase, R2 e Places reais. Implementação disponível não muda automaticamente a prioridade do roadmap.

Ordem:

1. fechar e validar o MVP de registro;
2. ativar metas, marcos, recordes, badges e XP para medir retenção;
3. ativar estatísticas avançadas e customização Plus;
4. conectar cobrança somente após escolher o provedor e implementar webhook assinado;
5. adicionar desafios sociais somente junto de seguidores/grupos;
6. adicionar IA ou mídia extra somente com orçamento e decisão próprios.

## Princípios

- Gamificação reforça a prática real; não recompensa apenas abrir o app.
- O loop `praticar → registrar → enxergar progresso → voltar` permanece gratuito.
- Não existe compra de XP, boost pago, badge de mérito comprado ou prioridade artificial em ranking/feed.
- Hobbies diferentes não disputam um ranking global pela mesma unidade bruta de tempo.
- Badges conquistados não são removidos se o histórico mudar; são marcos históricos.
- Metas são flexíveis e positivas. Falhar uma meta não remove XP, badge ou histórico.
- Toda leitura e alteração é autorizada pelo `user_id` extraído da autenticação.
- Cálculos usam UTC enquanto o usuário não possuir timezone configurável.

## Plano Free

- sessões, hobbies, equipamentos e backlog do núcleo atual;
- streak diário global;
- uma meta semanal ativa por hobby, medida por sessões ou minutos;
- progresso da meta atual;
- XP e nível por hobby;
- marcos e badges oficiais;
- recordes pessoais básicos;
- desafio mensal oficial;
- exportação bruta dos próprios dados em formato interoperável;
- participação em desafios sociais futuros.

## Plano Plus

- múltiplas metas simultâneas e metas mensais/customizadas;
- comparação entre períodos e tendências detalhadas;
- retrospectiva mensal/anual em dados estruturados para o client renderizar cards/infográficos;
- criação de desafios pessoais customizados;
- temas de perfil, badges em destaque e selo discreto de apoiador;
- planejamento avançado do backlog;
- regras e alertas de manutenção por equipamento;
- no futuro, criação/administração de desafios sociais, IA, múltiplas mídias e PDF visual.

O backend nunca aceita `isPremium`, `plan` ou equivalente no payload de negócio. Ausência de assinatura ativa equivale a `FREE`. Até existir provedor, contas Plus só podem ser preparadas por operação interna controlada no banco; não existe endpoint de auto-upgrade.

## Feature flags

| Variável | Local | Produção | Comportamento desligado | Remoção |
|---|---:|---:|---|---|
| `FEATURE_GAMIFICATION_ENABLED` | `true` | `false` | endpoints de metas, XP, badges, recordes e desafios respondem `503` | depois do rollout estável da Fase 1 |
| `FEATURE_PLUS_ENABLED` | `true` | `false` | endpoints Plus respondem `503`, mesmo para entitlement Plus | depois da cobrança e operação Plus estabilizarem |

Flags controlam disponibilidade; entitlement controla autorização comercial. Uma flag nunca transforma usuário Free em Plus.

## Metas

Métricas iniciais:

- `sessions`: quantidade de sessões no período;
- `minutes`: soma de `duration_minutes` no período.

Cadências:

- `weekly`: semana UTC de segunda a domingo;
- `monthly`: mês civil UTC;
- `custom`: intervalo explícito, exclusivo do Plus.

Regras Free:

- somente `weekly`;
- no máximo uma meta ativa por hobby;
- alvo inteiro positivo;
- hobby precisa pertencer ao usuário.

Regras Plus:

- múltiplas metas ativas;
- `weekly`, `monthly` ou `custom`;
- intervalo customizado precisa ter início, fim e duração limitada;
- arquivar preserva histórico; exclusão física não é necessária no fluxo normal.

O progresso é derivado das sessões existentes, não incrementado cegamente. Criar, editar ou excluir uma sessão reflete no progresso sem gerar contagem duplicada.

`completed` fica reservado no schema para uma evolução futura; na API atual a conquista é o boolean derivado `achieved` e a meta permanece `active` até ser atualizada ou arquivada. Isso impede que concluir cedo permita criar outra meta Free no mesmo período.

## XP e níveis por hobby

XP é uma projeção reconstruível a partir das sessões. Cada categoria define `xp_session_bonus` e `xp_minutes_per_point`; não existe fórmula escondida no client.

Fórmula por sessão:

```text
xp = xp_session_bonus + floor(duration_minutes / xp_minutes_per_point)
```

Configuração inicial:

| Categoria | Bônus | Minutos por XP |
|---|---:|---:|
| Sports & Movement | 10 | 5 |
| Arts & Creativity | 10 | 8 |
| Learning & Intellectual | 10 | 10 |
| Games & Strategy | 10 | 12 |

Níveis genéricos iniciais:

| XP mínimo | Nível | Label |
|---:|---:|---|
| 0 | 1 | Beginner |
| 100 | 2 | Engaged |
| 300 | 3 | Consistent |
| 750 | 4 | Dedicated |
| 1500 | 5 | Experienced |

Labels temáticos por hobby podem substituir os genéricos depois sem alterar XP acumulado. Alterar a fórmula exige recalcular toda a projeção e versionar a regra.

## Badges e marcos oficiais

Catálogo inicial:

- `first_session`: primeira sessão global;
- `sessions_5`, `sessions_25`, `sessions_100`: quantidade global;
- `hours_10`, `hours_50`: minutos globais acumulados;
- `explorer_3`: sessões em três hobbies distintos;
- `streak_7`, `streak_30`: melhor sequência histórica;
- `hobby_sessions_10`: dez sessões em um mesmo hobby.

O catálogo é definido pelo servidor. `user_badges` persiste `earned_at` e `hobby_id` quando o badge é específico. O client não declara conquista. No cache miss, o dashboard reconstrói a projeção a partir da fonte da verdade e a mantém por usuário por até cinco minutos. Create/update/delete de sessão invalida essa entrada somente depois do commit, de modo que a leitura seguinte reconstrói sem incremento duplicado. Essa reconstrução também funciona como reparo idempotente; detalhes em `estrategia-de-cache.md`.

## Recordes pessoais

Resposta básica gratuita:

- maior duração de uma sessão;
- maior número de sessões em uma semana UTC;
- maior número de minutos em um mês UTC;
- melhor streak histórico;
- hobby com mais sessões;
- hobby com mais minutos.

Plus acrescenta filtros de período, comparação entre períodos, séries temporais e distribuição por hobby.

## Desafio mensal oficial

O desafio oficial é pessoal e determinístico, sem IA. A primeira versão usa quantidade de sessões no mês, com alvo configurado pelo servidor. O progresso é derivado das sessões do mês. Concluir gera badge/conquista, nunca prêmio financeiro ou XP comprável.

Desafios customizados Plus reutilizam as métricas de metas. Desafios com amigos, ranking e administração de grupo permanecem na Fase 2 porque exigem relações sociais e regras antifraude próprias.

## Retrospectiva e comparação Plus

O backend entrega JSON estruturado; o app cliente renderiza gráficos e cards. Períodos suportados: mês e ano civil UTC.

Dados mínimos:

- sessões, minutos e dias ativos;
- distribuição por hobby;
- maior sessão;
- melhor streak no período;
- badges conquistados;
- evolução contra o período anterior equivalente.

Geração de imagem, compartilhamento em story e timelapse de fotos são trabalhos de client/mídia posteriores e não entram nesta implementação backend.

## Contrato HTTP implementado

Todos os endpoints abaixo exigem bearer válido e usam o usuário autenticado; nenhum aceita `userId`, plano ou permissão vindos do client.

| Endpoint | Plano | Flag | Função |
|---|---|---|---|
| `GET /api/me/plan` | Free/Plus | nenhuma | retorna entitlement derivado do banco, sem ids do provedor |
| `GET /api/me/gamification` | Free | gamificação | XP/nível por hobby, badges, recordes e desafio mensal |
| `POST/GET/PATCH/DELETE /api/me/goals` | Free/Plus | gamificação | meta semanal Free; múltiplas, mensal, global e customizada exigem Plus; `DELETE` arquiva |
| `GET /api/me/insights?from&to` | Plus | gamificação + Plus | resumo, série diária e comparação com período anterior equivalente, até 366 dias |
| `GET /api/me/wrapped?year&month` | Plus | gamificação + Plus | retrospectiva mensal/anual (ano entre 1900 e o próximo ano) e badges conquistados no período |
| `GET/PATCH /api/me/profile-customization` | leitura Free, escrita Plus | Plus na escrita | tema e até três badges próprios em destaque; selo é derivado |
| `GET/POST/PATCH/DELETE /api/me/equipment-maintenance` | leitura Free, escrita Plus | Plus na escrita | regras e alertas; `POST /{id}/complete` reinicia a janela |
| `GET /api/me/export/json` | Free | nenhuma | exporta dados próprios sem storage keys ou credenciais |
| `GET /api/me/export/sessions.csv` | Free | nenhuma | exporta sessões com escaping e proteção contra fórmula CSV |

Os campos avançados Plus do backlog usam os endpoints existentes em `/api/me/backlog-items`: `dueDate`, `priority`, `archived` e `position`. Payload legado sem esses campos preserva os dados avançados atuais; para limpá-los o client envia explicitamente os defaults/valores substitutos no mesmo update.

## Perfil Plus

- `profile_theme`: enum de temas fornecidos pelo produto;
- até três badges conquistados em `featured_badges`, com posição explícita;
- `supporter_badge`: derivado de assinatura ativa, nunca editável pelo client.

Tema e vitrine são cosméticos. Perfil público pode expor esses campos, mas nunca detalhes de pagamento, provider customer id ou datas financeiras.

## Planejamento avançado

O backlog básico continua gratuito. Campos avançados Plus:

- `due_date`;
- `priority`;
- `archived`;
- `position` para ordenação estável.

O usuário Free continua podendo ler dados avançados criados durante uma assinatura anterior, mas não criar/alterar campos Plus até reativar. Rebaixamento nunca apaga dados.

## Manutenção de equipamentos

Plus pode definir por equipamento:

- nome da regra;
- intervalo de uso em minutos;
- data da última manutenção;
- estado ativo/inativo.

Uso acumulado é derivado de `session_equipment` + sessões posteriores à última manutenção. O alerta é calculado sem worker externo. Marcar manutenção atualiza o marco e reinicia a contagem; não apaga sessões.

## Exportação e portabilidade

- exportação bruta dos dados próprios em CSV/JSON é Free;
- PDF visual, relatórios formatados ou exportações agendadas podem ser Plus no futuro;
- exportação nunca inclui secrets, tokens, storage keys internas de mídia ou dados privados de terceiros; CSV neutraliza células iniciadas por caracteres de fórmula;
- o escopo comercial não substitui obrigações de acesso/portabilidade aplicáveis pela LGPD.

## Fora desta implementação

- provedor, checkout, cobrança, trial e webhook de pagamento;
- IA para metas ou lembretes;
- FCM e notificações push;
- múltiplas fotos ou vídeos;
- família/casal;
- seguidores, feed, ranking e desafios sociais;
- PDF/PNG server-side de Wrapped;
- marketplace de templates da comunidade.

Esses itens permanecem documentados no roadmap, mas exigem decisão ou dependência própria antes de código.
