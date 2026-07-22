# Estratégia de cache

Este documento define onde cache é seguro e útil no MVP. A regra central é: cache reduz trabalho repetido, mas nunca vira fonte de verdade, mecanismo de autorização ou requisito para o sistema funcionar.

## Princípios

- PostgreSQL continua sendo a fonte de verdade de sessões, perfil, entitlement, equipamentos, backlog e gamificação.
- Não adicionar Redis, Memcached ou serviço SaaS de cache no MVP. A VPS única não precisa desse custo nem dessa complexidade.
- Nunca cachear bearer token, credencial, URL pré-assinada, resposta privada compartilhada, autorização por recurso ou entitlement do usuário.
- Cache em memória deve ser limitado por quantidade e tempo, aceitar perda em restart e poder ser reconstruído.
- Toda projeção derivada precisa de regra explícita de invalidação. Invalidação associada a uma escrita transacional ocorre somente depois do commit.
- Em futura execução com múltiplas instâncias, caches derivados por usuário precisam de invalidação distribuída ou devem ser removidos até ela existir.

## Matriz atual

| Dado | Camada | Chave | TTL/limite | Invalidação e fallback |
|---|---|---|---|---|
| Catálogo oficial de hobbies | Caffeine na aplicação | catálogo único | 24 h; 1 entrada | expiração/restart; consulta PostgreSQL no miss |
| Templates de atributos | Caffeine na aplicação | `hobbyId` | 24 h; até 500 hobbies | expiração/restart; consulta PostgreSQL no miss |
| Dashboard de gamificação | Caffeine na aplicação | `userId` autenticado | 5 min; até 10.000 usuários | create/update/delete de sessão remove após commit; reconstrução idempotente no miss |
| `place_id` validado | PostgreSQL | `place_id` | revalidar após 365 dias | Place Details com FieldMask somente `id`; falha segura se Google estiver indisponível |
| Foto pública processada | Cloudflare CDN/R2 | URL versionada pela storage key | `public, max-age=31536000, immutable` | nova key para nova variante; purge ao tornar privada |
| Upload cru e foto privada | sem cache compartilhado | URL pré-assinada temporária | `private, no-store` | URL expira; objeto permanece em bucket privado |

Os limites do Caffeine protegem a memória da JVM. Métricas internas de hit/miss são habilitadas, mas nenhum serviço externo de observabilidade é exigido para o cache funcionar.

## Google Places e localização

O backend persiste apenas o identificador permitido para cache de longo prazo:

- `places.place_id`: identificador retornado/validado pelo Google;
- `places.validated_at`: instante da última validação, para renovar IDs com mais de 12 meses;
- `sessions.location_label`: rótulo escolhido/enviado pelo usuário para exibição no produto.

Não persistir nome, endereço, latitude ou longitude retornados por Place Details como cache permanente. O client envia `location.placeId` e `location.label`; o backend valida o ID com o Google, trata o label como conteúdo do usuário e nunca aceita coordenadas. O DTO privado devolve `placeId` + `label`; o DTO público devolve somente o label, sem identificador do provedor.

Qualquer funcionalidade futura que realmente dependa de coordenadas — Heatmap, proximidade ou indicações locais — exige nova revisão de produto, privacidade, termos do provedor e retenção antes de ser implementada.

## R2 e CDN

- Conectar o bucket público a um domínio customizado da Cloudflare; `r2.dev` não deve ser a URL de produção nem é a camada de cache planejada.
- Variantes públicas usam keys imutáveis. Substituição de imagem gera nova key, evitando purge no fluxo normal.
- Variantes privadas, uploads temporários e URLs GET pré-assinadas usam `no-store` e nunca entram em cache público.
- Cache hit na CDN evita leitura repetida no R2, reduzindo operações Class B; armazenamento e operações que ultrapassarem o free tier continuam sujeitos à tabela vigente da Cloudflare.
- Ao mudar `everyone` para `only_me`, a API deixa de expor a URL imediatamente; o worker move/remove o objeto público e solicita purge da URL anterior.

## Client mobile futuro

O app pode usar cache de requests local para melhorar UX, mas deve respeitar o mesmo modelo:

- catálogo/templates com `staleTime` longo;
- listas e detalhes do próprio usuário invalidados após mutação;
- dados privados separados por UID e eliminados no logout;
- URLs privadas não persistidas como dado permanente;
- nenhuma decisão de permissão baseada apenas no cache do client.

A biblioteca concreta do client será escolhida no projeto mobile. Esta diretriz não adiciona dependência ao backend.

## Quando reavaliar

Reavaliar Redis/cache distribuído somente se houver múltiplas instâncias, medições mostrarem pressão real no PostgreSQL ou surgir coordenação distribuída. Antes disso, ele aumentaria custo, superfície operacional e possibilidade de inconsistência sem benefício comprovado.
