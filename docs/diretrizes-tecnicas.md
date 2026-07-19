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
| location | object opcional | ver seção Localização |
| equipmentIds | uuid[] | refs pra biblioteca do usuário, nunca texto livre na sessão |
| projectId | uuid opcional | ref pro backlog Kanban (mesma entidade) |
| photos | object[] | só storage keys já enviadas, nunca binário no payload |

## Contrato de API — perfil do hobbista

### Leitura do perfil atual

- `GET /api/me`
- Retorna o perfil já provisionado do usuário autenticado.
- Payload de resposta:

```json
{
  "id": "firebase-user-123",
  "email": "user@example.com",
  "name": "Example User",
  "emailVerified": true,
  "bio": "Corro, leio e fotografo.",
  "createdAt": "2026-07-19T12:00:00Z"
}
```

Regras:
- `id` é sempre o `sub`/`uid` do Firebase Authentication.
- `email`, `name` e `emailVerified` vêm do registro persistido do usuário provisionado via token válido.
- `bio` pertence só ao banco do produto e pode ser `null`.

### Atualização do perfil atual

- `PATCH /api/me`
- Payload de escrita:

```json
{
  "name": "Example User",
  "bio": "Corro, leio e fotografo."
}
```

Validações:
- `name` obrigatório, não pode ser blank.
- `name` máximo de 255 caracteres.
- `bio` opcional.
- `bio` máximo de 2000 caracteres.
- Implementação faz `trim()` em `name` antes de persistir.

Observação de produto:
- No estado atual do backend, o usuário pode editar `name` e `bio` no banco do produto.
- Como o usuário também tem `name` vindo do token do Firebase no provisionamento JIT, qualquer mudança futura nessa política precisa revisar explicitamente sincronização token ↔ perfil para evitar divergência de comportamento/documentação.

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
- Trade-off aceito: agregação de um atributo específico (ex: soma de `distance_km`) exige extração de JSON em SQL; endereçar com índice de expressão só se virar gargalo.

## Equipamento

- **Biblioteca** (`equipment`): itens do usuário, reutilizáveis.
- **Uso em sessão** (`session_equipment`): many-to-many, nunca texto livre na sessão.
- `category`: select fixo curado internamente (lista ainda não enumerada).
- `name`: texto livre, autocomplete pelo histórico do próprio usuário. Sem validação contra lista externa.
- `category` e `name` são **duas colunas do mesmo registro**, não chave/valor.
- Vínculo opcional a um hobby.
- Se um equipamento já estiver vinculado a sessões históricas, o backend não deve permitir exclusão no MVP; a remoção precisa ser rejeitada para preservar integridade do histórico.

## Localização

- Client: Google Places Autocomplete → obtém `place_id`.
- Client → backend: envia só `place_id` (+ `displayName` opcional, só UI otimista, nunca persistido).
- Backend: chama Place Details com `FieldMask` restrito a Essentials (`place_id`, nome, endereço, geometria) — nunca campos Pro/Enterprise.
- Backend nunca confia em lat/lng vindo do cliente.
- Cache: tabela `places(place_id PK, name, lat, lng)`, evita chamada repetida.
- Se o `place_id` já estiver em cache, backend reutiliza sem nova chamada externa; se não estiver, resolve via Google Place Details e persiste no cache antes de salvar a sessão.
- Resposta de sessão pode expor `location` com `placeId`, `name`, `lat` e `lng` a partir do cache do backend.
- Privacidade: sessão em casa não expõe endereço exato, só agrega em nível de bairro. Heatmap sempre agrega por "balde" geográfico, nunca ponto exato.

## Fotos

- Upload: client pede presigned URL, sobe direto pro storage (Cloudflare R2), nunca via backend.
- Processamento assíncrono: thumbnail, compressão/WebP, **remoção de EXIF** (GPS embutido).
- 2 variantes armazenadas (original + thumbnail), servidas via CDN.
- Só URL/key persistida no banco, nunca binário.
- Storage: Cloudflare R2 (S3-compatible, egress $0, free tier não expira) — não AWS S3.

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
