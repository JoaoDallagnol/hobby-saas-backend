# Roadmap de Funcionalidades

> App multi-hobby de tracking (tipo Strava/Letterboxd, mas não nichado). Objetivo da Fase 0: validar se o usuário cria o hábito de registrar. O MVP inclui somente a superfície social mínima de perfil/sessão pública, sem feed, seguidores ou descoberta.

## Fase 0 — MVP (sem depender de massa crítica)
- Perfil do hobbista (username, hobbies, nível, bio), com leitura do perfil de outra pessoa por username
- Tracker de sessão (título, data, tempo, no máximo uma foto, notas e visibilidade `everyone`/`only_me`)
- Atributos dinâmicos por hobby (ver `diretrizes-tecnicas.md` — Alternativa C) — o que evita o app parecer raso vs. app nichado
- Diário de reflexão → unificado no campo `notes`
- Biblioteca de equipamentos
- Backlog Kanban por hobby (fila de projetos/ideias)
- Streak de constância

## Fase 1 — retenção e identidade do app (funciona com poucos usuários)
- Painel de estatísticas de tempo por hobby
- Metas semanais flexíveis por hobby
- Marcos, badges e recordes pessoais
- XP/nível por hobby com fórmula configurável por categoria
- Desafio mensal oficial e retrospectiva básica de progresso
- Sessões colaborativas/em grupo (não depende de descoberta, usuário convida quem já conhece)
- Teste de personalidade/descoberta de hobby (onboarding)

## Fase 2 — efeito rede ⚠️ não lançar sem massa crítica de usuários/cidade
- Relação de seguidores e visibilidade `followers` nas sessões
- Feed social (cronológico, amigos/mesma cidade)
- Hobby Buddy (matching por proximidade — mesma restrição do feed)
- Indicações/rankings locais
- Heatmap de cidade — exige volume mínimo de sessões geolocalizadas por região, senão fica vazio/enganoso

## Fase 3 — gamificação social e expansão
- Desafios customizados entre amigos/grupos e ferramentas de administração
- Progressões temáticas por hobby sobre a base de XP da Fase 1
- Coleções sazonais e raridade de badges quando houver volume suficiente

## Fase 4 — premium
- Estatísticas avançadas, comparação entre períodos e Wrapped mensal/anual em dados estruturados
- Comparação "eu do passado"; timelapse de fotos permanece evolução posterior de mídia
- Múltiplas metas, cadências avançadas e desafios pessoais customizados
- Planejamento avançado do backlog
- Multi-perfil família/casal
- Metas com IA
- Alerta de manutenção de equipamento
- Lembrete de abandono (IA detecta queda de frequência)
- Exportação visual/agendada; exportação bruta dos próprios dados permanece gratuita
- Templates de hobby da comunidade com acesso antecipado pra assinante
- Customização de perfil (vitrine de badges, temas)
- Destaque de assinante na listagem (⚠️ como badge visível, nunca como ranking artificialmente inflado)

## Regra de priorização
Seguidores/feed, Hobby Buddy e Heatmap compartilham a mesma trava: não implementar/expor sem massa crítica local. A leitura direta de perfil e sessões `everyone` por username já existe no MVP como base, mas não constitui descoberta nem feed.

A fundação backend da Fase 1 e dos benefícios Plus pode ser construída antecipadamente, mas só deve ser ativada depois da validação end-to-end do MVP. Disponibilidade por feature flag não substitui entitlement Free/Plus; cobrança continua bloqueada até escolha de provedor e webhook assinado. Regras detalhadas: `gamificacao-e-planos.md`.
