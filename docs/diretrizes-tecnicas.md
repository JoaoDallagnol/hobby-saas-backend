# Diretrizes Técnicas

## Idioma
DB e código: inglês. Tradução (pt-BR e outros): futuro, fora de escopo agora.

## Sessão — atributos fixos (free tier)

| Campo | Tipo | Regra |
|---|---|---|
| title | string | default sugerido no front, editável |
| startedAt | timestamp | |
| durationMinutes | int | |
| notes | text | campo único (notas + reflexão unificados) |
| satisfaction | int | 1–5, obrigatório, validado no backend independente do front |
| visibility | enum | `everyone` ou `only_me`; default `only_me`; `followers` reservado para Fase 2 |
| location | object opcional | ver seção Localização |
| equipmentIds | uuid[] | refs pra biblioteca do usuário, nunca texto livre na sessão |
| projectId | uuid opcional | ref pro backlog Kanban (mesma entidade) |
| photos | object[] | na criação, storage keys já enviadas; na edição, IDs existentes e/ou novas storage keys; nunca binário no payload |

## Contrato de API — perfil do hobbista

### Catálogo de hobbies

- `GET /api/hobbies`
- Retorna o catálogo oficial ordenado por categoria e nome, com `id`, `name`, `categoryName` e `icon`.
- Esse endpoint é a fonte dos `hobbyId` usados ao incluir um hobby no perfil; o client não deve inventar ids ou nomes fora do catálogo.

### Leitura do perfil atual

- `GET /api/me`
- Retorna o perfil já provisionado do usuário autenticado.
- Payload de resposta:

```json
{
  "id": "firebase-user-123",
  "email": "user@example.com",
  "name": "Example User",
  "username": "example.user",
  "emailVerified": true,
  "bio": "Corro, leio e fotografo.",
  "createdAt": "2026-07-19T12:00:00Z"
}
```

Regras:
- `id` é sempre o `sub`/`uid` do Firebase Authentication.
- `email`, `name` e `emailVerified` vêm do registro persistido do usuário provisionado via token válido.
- `bio` pertence só ao banco do produto e pode ser `null`.
- `username` é único sem distinção de maiúsculas/minúsculas, pode ser `null` para usuários antigos/JIT até ser escolhido e nunca substitui o UID como PK interna.

### Atualização do perfil atual

- `PATCH /api/me`
- Payload de escrita:

```json
{
  "name": "Example User",
  "bio": "Corro, leio e fotografo.",
  "username": "example.user"
}
```

Validações:
- `name` obrigatório, não pode ser blank.
- `name` máximo de 255 caracteres.
- `bio` opcional.
- `bio` máximo de 2000 caracteres.
- `username`, quando enviado, usa 3–30 caracteres minúsculos (`a-z`, números, `.`, `_`, `-`), não pode ser reservado nem pertencer a outra conta.
- Implementação faz `trim()` em `name` antes de persistir.

Observação de produto:
- No estado atual do backend, o usuário pode editar `name` e `bio` no banco do produto.
- Como o usuário também tem `name` vindo do token do Firebase no provisionamento JIT, qualquer mudança futura nessa política precisa revisar explicitamente sincronização token ↔ perfil para evitar divergência de comportamento/documentação.

### Perfil e sessões públicas

- `GET /api/users/{username}` retorna apenas `username`, `name`, `bio` e hobbies do perfil.
- `GET /api/users/{username}/sessions?page=0&size=20&hobbyId={opcional}` lista apenas sessões `everyone`.
- `GET /api/users/{username}/sessions/{sessionId}` retorna detalhe somente se a sessão pertencer ao perfil e estiver `everyone`; caso contrário responde como não encontrada, evitando enumeração.
- Esses endpoints continuam autenticados no MVP: "público" significa visível a qualquer usuário do app, não indexado anonimamente na web.
- O DTO público nunca contém e-mail, UID Firebase, `place_id`, lat/lng, `projectId`, `equipmentIds` nem storage keys. A localização pública contém somente o nome seguro do lugar.
- Não existe listagem/busca de usuários, feed ou relação de seguidores nesta fase.

### Leitura dos hobbies do perfil

- `GET /api/me/hobbies`
- Retorna hobbies já associados ao usuário autenticado.
- Payload de resposta:

```json
[
  {
    "hobbyId": "550e8400-e29b-41d4-a716-446655440000",
    "hobbyName": "Running",
    "categoryName": "Sports & Movement",
    "icon": "shoe",
    "experienceLevel": "intermediate"
  }
]
```

### Inclusão de hobby no perfil

- `POST /api/me/hobbies`
- Payload:

```json
{
  "hobbyId": "550e8400-e29b-41d4-a716-446655440000",
  "experienceLevel": "intermediate"
}
```

Validações e regras:
- `hobbyId` obrigatório.
- `experienceLevel` opcional, máximo de 50 caracteres.
- Não aceitar hobby duplicado no mesmo perfil.
- `hobbyId` precisa existir no catálogo oficial.

### Atualização de hobby no perfil

- `PATCH /api/me/hobbies/{hobbyId}`
- Payload:

```json
{
  "experienceLevel": "advanced"
}
```

Validações e regras:
- `experienceLevel` opcional, máximo de 50 caracteres.
- `hobbyId` precisa já pertencer ao usuário autenticado.

### Remoção de hobby do perfil

- `DELETE /api/me/hobbies/{hobbyId}`
- Remove o vínculo em `user_hobbies`.
- Regra atual: se o hobby não pertencer ao usuário autenticado, a operação falha.

## Atributos dinâmicos por hobby

Modelo: **template + JSON** (não EAV, não coluna própria por atributo).

- `hobby_attribute_template(id, hobby_id, key, label, type, unit, display_order)` — metadado, define o que existe por hobby.
- `sessions.attributes` (JSONB) — valores reais, validados contra o template antes de persistir.
- Implementação atual do backend: Hibernate ORM nativo com `@JdbcTypeCode(SqlTypes.JSON)` sobre `Map<String, Object>` em `SessionRecord`; sem Hypersistence Utils nem outra lib auxiliar neste estágio.
- Todos os atributos dinâmicos ficam **free tier** (split premium/free foi considerado e descartado).
- No MVP atual, os atributos dinâmicos são opcionais. O template ainda não possui metadado `required`; se essa regra entrar depois, exige migração, atualização do contrato e revalidação de create/update.
- Trade-off aceito: agregação de um atributo específico (ex: soma de `distance_km`) exige extração de JSON em SQL; endereçar com índice de expressão só se virar gargalo.

## Equipamento

- **Biblioteca** (`equipment`): itens do usuário, reutilizáveis.
- **Uso em sessão** (`session_equipment`): many-to-many, nunca texto livre na sessão.
- `category`: select fixo curado internamente (lista ainda não enumerada).
- `name`: texto livre, autocomplete pelo histórico do próprio usuário. Sem validação contra lista externa.
- `category` e `name` são **duas colunas do mesmo registro**, não chave/valor.
- Vínculo opcional a um hobby.
- Se um equipamento já estiver vinculado a sessões históricas, o backend não deve permitir exclusão no MVP; a remoção precisa ser rejeitada para preservar integridade do histórico.
- Autocomplete inicial é feito no client sobre `GET /api/me/equipment`, opcionalmente filtrado por `hobbyId`; busca/paginação server-side só entra quando o volume real justificar.

## Localização

- Client: Google Places Autocomplete → obtém `place_id`.
- Client → backend: envia só `place_id` (+ `displayName` opcional, só UI otimista, nunca persistido).
- Backend: chama Place Details com `FieldMask` restrito a Essentials (`place_id`, nome, endereço, geometria) — nunca campos Pro/Enterprise.
- Backend nunca confia em lat/lng vindo do cliente.
- Cache: tabela `places(place_id PK, name, lat, lng)`, evita chamada repetida.
- Se o `place_id` já estiver em cache, backend reutiliza sem nova chamada externa; se não estiver, resolve via Google Place Details e persiste no cache antes de salvar a sessão.
- Resposta privada do dono pode expor `location` com `placeId`, `name`, `lat` e `lng` a partir do cache do backend. Resposta pública expõe somente `locationName`.
- Privacidade: sessão em casa não expõe endereço exato, só agrega em nível de bairro. Heatmap sempre agrega por "balde" geográfico, nunca ponto exato.

## Fotos

- Upload: client pede presigned URL, sobe direto pro storage (Cloudflare R2), nunca via backend.
- Processamento assíncrono: thumbnail, compressão/WebP, **remoção de EXIF** (GPS embutido).
- Cada sessão aceita **no máximo uma foto**. O contrato continua como lista para permitir evolução futura sem trocar o shape.
- 2 variantes processadas armazenadas (imagem de até 2048 px + thumbnail), sem nunca servir o upload cru.
- Só URL/key persistida no banco, nunca binário.
- Storage: Cloudflare R2 (S3-compatible, egress $0, free tier não expira) — não AWS S3.
- A key temporária de upload é namespaced por uma codificação URL-safe do `uid`; ao criar/atualizar sessão, o backend rejeita key de outro usuário, duplicatas e mais de uma foto.
- Enquanto o worker não finalizar, as URLs são `null`, `processingStatus` é `pending` e `deliveryStatus` é `processing`; nunca expor a key original como falsa URL. Estados de processamento: `pending`, `ready`, `failed`.
- O processamento roda em worker agendado na própria aplicação (adequado ao monolito de instância única), consulta lotes de até 10 fotos pendentes e tenta cada item no máximo 3 vezes.
- O worker baixa a key temporária do R2, executa `cwebp` sem copiar metadata, gera variante de até 2048 px (quality 82) e thumbnail de até 480 px (quality 75), envia ambas ao R2 e remove a key temporária somente depois de persistir as novas referências.
- Upload temporário e mídia `only_me` ficam no bucket privado. Leitura privada usa GET presigned de 15 minutos. Variantes de sessão `everyone` ficam no bucket público e são entregues por URL estável no domínio/CDN configurado.
- Ao editar `visibility`, um worker idempotente move as variantes entre os buckets. A API para de expor a foto publicamente assim que a sessão vira `only_me`; o worker remove o objeto público e solicita purge das URLs no cache da Cloudflare. Durante a movimentação, URLs ficam `null` com `deliveryStatus=updating_visibility`.
- Em `local`, Adobe S3Mock 5.1.0 fornece a mesma API S3 e persiste objetos em volume/pasta Docker; não exige conta Cloudflare para desenvolver.
- O rollout é controlado por `FEATURE_PHOTO_UPLOADS_ENABLED` e `FEATURE_PHOTO_PROCESSING_ENABLED`. Em produção, uploads só ficam ready quando ambos estão ativos e o health check rejeita upload ativo com processamento desligado.
- Em `PATCH /api/sessions/{id}`, omitir `photos` preserva a foto atual; uma lista vazia remove. Para manter a foto, enviar `{ "id": "<photoId>" }`; para anexar uma nova, enviar `{ "storageKey": "<uploadKey>" }`. IDs são validados contra a própria sessão para impedir IDOR/BOLA.
- Ao remover foto ou sessão, uma trigger transacional grava escopo + key em `photo_storage_deletions`; um worker remove o objeto do bucket correto com retry/backoff e só então exclui a tarefa. A key de upload original é única para impedir reutilização ambígua entre sessões.
- Formatos aceitos no MVP: JPEG, PNG e WebP, até 10 MB. HEIC/HEIF ficam rejeitados até existir decoder seguro compatível com o pipeline.

## Listagem de sessões

- `GET /api/sessions?hobbyId={opcional}&page=0&size=20` usa paginação desde o MVP.
- `page` é zero-based; `size` aceita de 1 a 100.
- Resposta: `items`, `page`, `size`, `totalItems`, `totalPages`, `hasNext`.
- Ordenação estável: `startedAt DESC`, depois `id DESC`.

## Feature flags operacionais

- `GET /api/features` expõe ao client autenticado somente flags não sensíveis: `photoUploads`, `sessionLocation`, `photoProcessing`.
- Flags não substituem autorização nem podem ser enviadas/alteradas pelo client; são configuração de servidor por ambiente.
- Feature desligada falha com HTTP `503` quando o payload tenta usá-la, permitindo rollout/fallback explícito.
- Equipamentos, backlog, perfil, sessões básicas e streak são núcleo do MVP e não recebem flag para evitar um produto parcialmente incoerente.

## Taxonomia de hobbies

- Own-DB, não é dependência de API externa em runtime.
- Seed único (não integração contínua) a partir de: https://gist.github.com/carlelieser/884584d06b2d9429f321ec192f6dc7b5
  - **Tem duplicatas** (Photography, Astronomy, Badminton, Bowling, Metal detecting aparecem repetidos) e categorização fraca (`Indoors`/`Outdoors`) — usar só como lista de nomes candidatos, não importar estrutura.
- Crescimento: curadoria manual + fluxo de sugestão do usuário (`hobby_suggestions`: texto livre → moderação → aprovação vira hobby oficial).
- Equipamento: sem dataset externo (itens são específicos de marca/modelo, não catalogáveis); categorias serão lista curada interna, ainda não enumerada.

## Exemplo `createSession`

```json
{
  "hobbyId": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Corrida no parque",
  "startedAt": "2026-07-02T07:30:00Z",
  "durationMinutes": 45,
  "notes": "Peguei um ritmo bom hoje",
  "satisfaction": 4,
  "visibility": "only_me",
  "location": { "placeId": "ChIJ...xyz" },
  "equipmentIds": ["8f14e45f-ceea-4c8c-b5c6-3e0d1a2f9b11"],
  "projectId": null,
  "photos": [{ "storageKey": "uploads/user_123/session_temp/a1b2c3.webp" }],
  "attributes": { "distance_km": 8.5 }
}
```

## Em aberto
- Categorização final de hobbies (além do split fraco do seed).
- Enumeração de categorias de equipamento.
- Split premium/free em atributos dinâmicos: descartado por ora, pode ser revisitado.
