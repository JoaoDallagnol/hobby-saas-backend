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

## Atributos dinâmicos por hobby

Modelo: **template + JSON** (não EAV, não coluna própria por atributo).

- `hobby_attribute_template(id, hobby_id, key, label, type, unit, display_order)` — metadado, define o que existe por hobby.
- `sessions.attributes` (JSONB) — valores reais, validados contra o template antes de persistir.
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
