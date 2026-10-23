# Modified PvP — Especificação funcional e técnica da versão 1.3.1

> **Status:** implementação inicial aplicada neste repositório (branch `feature/modifiedpvp-1.3.1-hybrid-combat`).  
> **Adiado de propósito:** Duelo Coordenado, Eco do Trovão e Último Recurso ficam atrás de `hybridOptionalStyles=false`. Os pares da seção 11 não foram implementados. `spearMaceExtendedReachEnabled` não altera o alcance; o Golpe Estendido só reconhece e confirma a troca.  
> **Base:** Minecraft Java 1.21.11, Fabric, Java 21; projeto Modified PvP 1.2.2 e especificação individual 1.3.0.  
> **Escopo 1.3.1:** sistema adaptativo **híbrido** (duas armas), novo encantamento **Pull** para tridente, integração de controles e advancement especial **Technoblade Never Dies**.  
> **Não confundir:** a versão **1.3.0** cobre os estilos e finalizadores **individuais**; a **1.3.1** acrescenta sinergias **entre equipamentos**. Não substituir os mecanismos individuais da 1.3.0.  
> **Regra principal:** implementar de modo incremental, preservando código, assets, saves, encantamentos, mecânicas vanilla, progresso de advancements e o projeto original.

## 0. Resumo executivo e decisões tomadas

- O motor híbrido identifica **como** o jogador usa duas armas, observando eventos reais e validados no servidor; não se baseia só na ordem dos botões ou na troca de slot.
- Todo ataque permanece **manual**; sem auto-combos, auto-mira, perseguição automática, teleporte, animações cinematográficas ou poderes incompatíveis com a estética Vanilla+.
- **Duas espadas** continuam na **Opção A da 1.3.0**: manter os controles já existentes. O sistema híbrido não deve alterar esse acordo.
- **Spear + Mace** (lança da 1.21.11, distinta do tridente) fica centrada em Wind Charge + Lunge no ar + smash e em *reach swap* de lança para maça; reconhecer o timing e a trajetória sem criar multiplicadores extras de dano.
- **Espada + Tridente** oferece perseguição via **Pull** e continuidade ofensiva via **Lealdade** sem Pull.
- **Maça + Tridente** oferece controle de posicionamento e impactos, não duplica o estilo aéreo de Spear + Mace.
- **Machado + Escudo** torna a defesa uma preparação de golpes pesados; **Espada + Escudo** enfatiza bloqueio preciso, estocada e iniciativa.
- **Espada + Besta** trabalha surpresa, flechas e foguetes; **Machado + Besta** é a combinação de homenagem ao Technoblade, nome de estilo **Coroa Carmesim**, com advancement **Technoblade Never Dies** (texto exato, em inglês).
- Há combinações adicionais exploradas, porém não aprofundadas o suficiente para exigir implementação na entrega inicial: Tridente + Escudo, Arco + Espada, Maça + Escudo e Lança + Besta. Mantê-las como **backlog**, não inventar efeitos finais para preencher lacunas.
- **Sem valores finais obrigatórios** para dano, duração, força de Pull, distância, cooldown ou tolerância de reach. Definir parâmetros nomeados e ajustáveis; fazer tuning com testes PvP.

## 1. Princípios de gameplay e não objetivos

### 1.1. Identidade Vanilla+

1. Reusar animações de ataque, projéteis, sons, partículas e física reconhecíveis no Minecraft.
2. Técnica híbrida = ação real da arma A + ação real da arma B + situação validada + recompensa contextual limitada.
3. A recompensa deve favorecer **uma especialidade**: perseguição, timing, recuperação, posicionamento, interceptação ou controle de grupos — não apenas multiplicar dano.
4. Sempre oferecer contrajogada: desviar, bloquear quando permitido, recuar, quebrar linha de visão, explorar janela de carga, atacar pelas costas ou induzir erro.
5. Não criar buff permanente de alcance, invulnerabilidade, cancelamento instantâneo de recuperação ou stun em cadeia.

### 1.2. Exclusões explícitas

- Nenhuma arma na off-hand passa a ter clique esquerdo próprio por padrão.
- Nenhuma troca de arma transfere atributos de alcance, Density, Breach, dano, encantamento ou nível entre itens, exceto uma **tolerância contextual expressamente validada** na técnica Golpe Estendido.
- Não fortalecer o dano de explosões de TNT minecart ou foguetes; preservar suas condições e mecânicas vanilla.
- Não usar dano de smash baseado em velocidade horizontal; exigir queda válida e regras originais da maça.
- Não permitir simultaneamente finalizador individual, híbrido e outro finalizador no mesmo `actionId`.
- Não conceder raios fora das condições vanilla nem Pull sem Lealdade.

## 2. Regras de implementação do motor híbrido

### 2.1. Modelo conceitual

**Equipamento A/B:** duas famílias de armas em hotbar ou main-hand/off-hand. **Transição:** relação temporal/causal entre evento válido de A e de B. **Estilo híbrido:** padrão identificado pela transição e pelo contexto. **Oportunidade híbrida:** autorização temporária para um finalizador, com tipo, alvo, pré-requisitos e expiração. **Tentativa:** comando manual concreto que valida e consome oportunidade. **Família híbrida:** par de armas normalizado e reconhecido; ordem poderá importar conforme o estilo.

Criar motor **server-authoritative** que reutilize, quando possível, o catálogo de eventos e a máquina de estados da 1.3.0, em vez de duplicar a lógica para cada par.

### 2.2. Eventos mínimos propostos

| Evento | Dados indispensáveis |
|---|---|
| `WEAPON_SWAP` | jogador, tick, família/item anterior e seguinte, mão/slot; sem progresso sozinho |
| `MELEE_VALID_HIT` | actionId, arma de origem, alvo, hit confirmado, bloqueio, cooldown de ataque |
| `SWORD_THRUST_HIT` | actionId, alvo, contexto de avanço |
| `AXE_HEAVY_HIT` / `AXE_SHIELD_DISABLE` | carga válida, alvo e estado de bloqueio |
| `SHIELD_BLOCK_VALID` / `SHIELD_BLOCK_TIMED` | defensor, atacante, orientação, instante, bloqueio real |
| `TRIDENT_THROW_HIT` | identidade do projétil, dono, item/enchantments de origem, alvo |
| `TRIDENT_RETURN_CONFIRMED` | mesma identidade do projétil e retorno efetivo |
| `PULL_APPLIED` | alvo, deslocamento realmente permitido, Lealdade presente, resistência a controle |
| `MACE_CHAIN_HIT` / `MACE_CHAIN_RETURN` | acerto direto ou bloco, retorno confirmado, posição |
| `MACE_SMASH_VALID` | queda realmente válida, alvo, contexto, encantamentos aplicáveis |
| `WIND_CHARGE_BOOST` / `SPEAR_LUNGE_USED` | trajetória, fonte de impulso e vetor, tick |
| `CROSSBOW_FIRED` / `PROJECTILE_HIT` | besta e carga reais, ação original, munição, alvo(s) |
| `FIREWORK_SPLASH_VALID` | ação original, alvos válidos distintos, dano base vanilla |
| `CONTROL_IMMUNITY_APPLIED` | tipo e contexto de controle do alvo |

Eventos com várias entidades atingidas mantêm **um `actionId` de origem**; para progressão usar um evento de ação com conjunto de alvos, nunca contar cada pelota/foguete como uma etapa independente.

### 2.3. Contexto de trajetória aérea

Para Spear + Mace e parte de Maça + Tridente, rastrear `aerialSequenceId` por ciclo real de movimento. Começa com transição legítima de solo para ar/impulso e termina na aterrissagem, morte, teleporte, mudança de dimensão ou invalidade do contexto. Registrar Wind Charge, Lunge, swap, ataques, smash, velocidade vertical/horizontal e item efetivo em cada evento. **Não considerar apertar Lunge no vazio suficiente para preparo**.

### 2.4. Máquina de estados

`IDLE → OBSERVING → CANDIDATE → READY → ATTEMPTED → COOLDOWN → IDLE`

- `OBSERVING`: coletar janela curta de eventos genuínos, por alvo/contexto e família de par.
- `CANDIDATE`: detectar uma transição plausível; exigir segundo equipamento e eventos efetivos.
- `READY`: estilo escolhido deterministicamente, congelado e exibido discretamente. Apenas **uma** oportunidade híbrida ofensiva por jogador de cada vez; configurar política global para evitar armazenar vários pares em slots distintos.
- `ATTEMPTED`: validar o ataque real, arma, alvo, alcance, linha de visão, bloqueio, cooldown, encantamentos e janela. Aplicar efeito apenas quando elegível. Tentativa real mal executada pode consumir oportunidade sem bônus.
- `COOLDOWN`: bloquear preparo/executar novo finalizador híbrido até expirar; limpar contexto incompatível.

Limpar estados temporários na morte, logout, troca de mundo, expiração, perda da arma indispensável ou troca para contexto incompatível. O avanço/advancement já conquistado permanece.

### 2.5. Algoritmo de classificação

1. Servidor recebe o evento; confirma validade e evita duplicatas por `actionId`.
2. Insere na janela recente com alvo, dono, arma usada e contexto de movimento.
3. Avalia **regras por técnica**, em vez de somar pontuação genérica por qualquer swap.
4. Considera somente estilos com pré-requisitos cumpridos; desempate determinístico e registrado.
5. Em `READY`, fixa `styleId`, `targetId` (quando aplicável), `sourceActionIds`, `expiresAt`, `requiredEquipment` e `aerialSequenceId` (quando pertinente).
6. Consumir em comando compatível, prevenindo aplicar efeito em arma distinta ou outros ataques provenientes do mesmo `actionId`.

**Importante:** critérios de **preparo** e de **execução** são diferentes. Nunca conceder bônus imediatamente após o evento que apenas completou o preparo, a menos que uma técnica explicitamente defina execução no mesmo evento e isso esteja coberto por testes.

### 2.6. Prioridade com finalizadores individuais da 1.3.0

- Um evento de ataque só pode executar **um finalizador**, com prioridade contextual explícita: técnica híbrida `READY` e elegível, se for aquela ação escolhida pelo jogador; senão individual `READY` e elegível; senão ataque comum. Em eventos defensivos, avaliar primeiramente a defesa válida, impedindo consumo simultâneo por parry/escudo/lança.
- Não apagar automaticamente progresso individual de arma apenas ao trocar de slot; ao usar **a mesma abertura** (ex.: Defesa Rompida da lança) consumir a abertura uma só vez e proibir reaproveitamento para finalização híbrida.
- Bônus de recuperação/deslocamento de dois finalizadores não acumulam no mesmo golpe. Knockback adicional também não empilha de forma independente.
- Não duplicar progresso de estilos com a própria ação de finalização. Evitar reentrada de handlers de dano/projétil.

### 2.7. Pseudocódigo orientativo (não é código pronto)

```java
record HybridCombatEvent(
    UUID actionId,
    UUID playerId,
    long serverTick,
    WeaponFamily weaponFamily,
    ActionType actionType,
    @Nullable UUID targetId,
    @Nullable UUID projectileId,
    @Nullable UUID aerialSequenceId,
    Set<ContextTag> context,
    boolean validated
) {}

record HybridOpportunity(
    HybridPairId pairId,
    HybridStyleId styleId,
    @Nullable UUID targetId,
    long preparedTick,
    long expiresAtTick,
    Set<UUID> provenanceActionIds,
    Set<Requirement> requiredConditions
) {}

// Fluxo esquemático, com autoridade do servidor:
void onValidatedCombatEvent(HybridCombatEvent event) {
    if (seenAction(event.actionId())) return;
    recentEvents(event.playerId()).append(event);
    updateHybridCandidates(event.playerId());
}

void onManualFinisherAttempt(AttackContext attack) {
    var opportunity = activeHybridOpportunity(attack.playerId());
    if (opportunity == null || !matches(attack, opportunity)) {
        resolveIndividualOrVanilla(attack);
        return;
    }
    resolveExactlyOneFinisher(attack, opportunity);
}
```

**Não** copiar estas APIs cegamente: adaptar às classes reais do projeto, versões Yarn/Fabric e hooks disponíveis.

## 3. Encantamento novo: Pull (tridente)

### 3.1. Separação obrigatória de Lealdade

**Mudança de comportamento:** remover o *puxão e controle adicionais do mod* atualmente ligados à **Lealdade**. Lealdade sozinha volta a fazer somente seu retorno vanilla. O puxão é deslocado para o novo encantamento **Pull**.

- Pull exclusivo do tridente, níveis **I–III** propostos.
- **Pré-requisito funcional obrigatório:** tridente com Lealdade; sem Lealdade, Pull **não ativa**, inclusive em itens gerados por comandos ou mod externo.
- Pull I, II e III diferem na força/eficiência limitada do puxão, **não** em dano, stun ou duração de imobilização. Nenhum nível garante acerto posterior.
- Incompatível na prática com Correnteza, porque Lealdade e Correnteza não são compatíveis no vanilla; preservar regras vanilla.
- Compatível, quando vanilla permitir, com Condutividade e demais encantamentos aplicáveis, sem adicionar raio ou dano.
- Obtenção proposta: livros em loot de estruturas oceânicas; pesos, chances e processo de combinação em bigorna devem ser parametrizados e testados, não inventados como números fechados.

### 3.2. Ativação do Pull

1. Tridente efetivamente lançado por jogador, associado à cópia/carga de origem (não inferir apenas pelo item atualmente na mão).
2. Acerto contra entidade elegível; verificar Lealdade e Pull do **item de origem do projétil**.
3. Checar linha de visão/obstáculos e imunidades de controle da 1.2.2; cancelar quando percurso atravessaria sólido ou a situação deixar de ser válida.
4. Aplicar deslocamento curto em direção ao dono, sem stun completo, sem anular salto/dash por toda a duração.
5. Iniciar retorno de Lealdade normalmente, sem duplicar projétil, durabilidade ou item.
6. Usar cooldown/resistência por alvo para impedir chain-pull infinito por múltiplos tridentes ou jogadores.

### 3.3. Integração com combos

- **Espada + Tridente:** Pull habilita Caçada Implacável; Lealdade **sem** Pull mantém Retorno Ofensivo e Duelo Coordenado.
- **Maça + Tridente:** Pull habilita Impacto de Âncora; outros estilos não precisam necessariamente de Pull.
- **Tridente individual 1.3.0:** atualizar descrições e testes para não presumir puxão por Lealdade pura.
- **Migração:** quem já possui Lealdade mantém o item e seu retorno; **não adicionar Pull automaticamente** a tridentes antigos. Informar alteração de comportamento em changelog e guia do jogador.

## 4. Spear + Mace — especialização aérea (lança + maça)

**Identidade:** técnica avançada de movimento, ganho de altura, Lunge aéreo, troca precisa e smash. **Não confundir lança com tridente.** Preferir hotbar swaps, preservando clique direito da corrente da maça.

### 4.1. Impacto de Investida

- **Preparar:** sequência aérea válida com impulso de Wind Charge, uso real de Lunge no ar, troca para maça e smash legítimo que acerte alvo. Contar uma trajetória como unidade de evidência, não cada troca.
- **Executar:** na próxima trajetória elegível, o jogador realiza manualmente Wind Charge/Lunge/troca e conecta smash com maça.
- **Recompensa:** recuperação/continuidade horizontal sutil após acerto, com eventual preservação controlada de direção; **sem bônus de dano de queda, altura gratuita ou reset de dash**.
- **Falha:** queda inválida, troca fora do contexto, smash errado, interrupção ou expiração não concedem bônus.
- **Contrajogo:** esquiva lateral, alterar distância, atacar preparação, forçar pouso fora de alvo.

### 4.2. Golpe Estendido (*reach swap*)

- **Preparar:** reconhecer queda válida e uso preciso de lança seguido de troca de último instante para maça, com smash real.
- **Executar:** durante nova queda válida, trocar da lança para maça na janela curta validada pelo servidor e acertar smash.
- **Recompensa:** **tolerância contextual pequena e configurável de alcance no ataque de maça**, somente nesta finalização e no golpe elegível. Não conceder permanentemente alcance de lança; não somar duas vezes o bônus do vanilla/atributo efetivo.
- **Modo conservador recomendado:** primeiro implementar apenas reconhecimento, feedback e validação sem aumento de alcance; liberar tolerância após testes de hit registration e anti-cheat.
- **Contrajogo:** evasão lateral, quebrar trajetória, sair da janela de impacto; não atravessar blocos.

### 4.3. Reação Aérea

- **Preparar:** movimentos aéreos válidos do mod e smash real após uso de lança na mesma trajetória.
- **Executar:** smash manual após trajetória compatível.
- **Recompensa:** recuperar controle horizontal mais cedo depois do impacto; **sem** novo pulo, dash gratuito ou invulnerabilidade.
- **Prioridade em elegibilidade simultânea:** Golpe Estendido > Impacto de Investida > Reação Aérea, antes de congelar `READY`; não mudar silenciosamente depois.

**Interações:** Wind Burst, Density e Breach seguem regras vanilla; Lunge não concede altura própria; velocidade horizontal não é dano de queda; evitar attribute swapping não intencional causado pelo mod.

## 5. Espada + Tridente — especialização Predador

**Equipamento preferencial:** espada principal, tridente secundário. Com tridente na off-hand, o clique direito deve priorizar **uso/arremesso do tridente**; estocada da espada segue em `V`, parry em `X`. Validar precedência real no Fabric.

### 5.1. Caçada Implacável → Corte de Caça

- **Requer:** tridente com **Lealdade + Pull**.
- **Preparar:** ataques de espada acertam alvo, tridente atinge **mesmo alvo** enquanto este aumenta distância, Pull ocorre validamente, seguido de continuação ofensiva elegível.
- **Executar:** novo puxão válido contra alvo marcado e estocada manual de espada (`V`) dentro da janela de oportunidade.
- **Recompensa:** recuperação ofensiva limitada após estocada acertada; **Pull já é a atração**, não adicionar segundo puxão, stun ou bloqueio de dash.
- **Contrajogo:** escapar lateralmente, dash, obstáculos, defender tridente, evitar estocada.

### 5.2. Retorno Ofensivo → Retomada

- **Requer:** Lealdade; Pull **opcional**.
- **Preparar:** lançar tridente, acertar com espada durante ausência do projétil e confirmar retorno real do mesmo tridente.
- **Executar:** depois da oportunidade, preparar manualmente novo arremesso com pequena vantagem de prontidão/continuidade, sem disparo automático nem suprimir requisitos de uso.
- **Recompensa:** ritmo de alternância, não dano ou puxão adicional.
- **Contrajogo:** evitar ataques de espada durante o retorno, forçar distância, atacar janela sem tridente.

### 5.3. Duelo Coordenado → Estocada Coordenada (alternativa secundária)

- **Preparar:** acertos alternados de espada e tridente no mesmo alvo, sem exigência de fuga.
- **Executar:** estocada manual após arremesso confirmado.
- **Recompensa:** estabilidade leve na trajetória da estocada; não ampliar alcance ou ignorar escudo.
- **Nota de produto:** só implementar após validar diferencial em relação às duas técnicas principais e ao finalizador individual da espada.

**Conflitos:** uma janela de perseguição não deve consumir também Corte Relâmpago/Contra-Corte individual; identificar e rastrear identidade de projétil, evitar exploração com vários tridentes.

## 6. Maça + Tridente — controle do alvo e impacto

**Distinta de Spear + Mace:** o tridente posiciona o **alvo**; a maça executa impactos e usa corrente/queda. **Controles preferenciais:** troca de slots; evitar conflito de clique direito entre arremesso de tridente e corrente de maça.

### 6.1. Âncora das Marés → Impacto de Âncora

- **Requer:** Lealdade + Pull para o tridente.
- **Preparar:** acertar tridente, aplicar Pull válido e acertar **mesmo alvo** com maça depois do deslocamento.
- **Executar:** repetir interação válida; golpe manual de maça converte a oportunidade em empurrão **direcional controlado** após o acerto.
- **Recompensa:** escolher onde continuar a luta, sem aumentar dano de smash e sem puxar de novo.
- **Contrajogo:** evitar projétil, resistir ao controle conforme regras da 1.2.2, desviar da maça, defender quando aplicável.

### 6.2. Queda Guiada → Impacto Guiado

- **Preparar:** impulso legítimo de Wind Charge/maça acorrentada/Wind Burst, tridente acerta alvo durante trajetória e smash válido de maça posteriormente.
- **Executar:** novo tridente válido e smash **manual no mesmo alvo** na trajetória aérea.
- **Recompensa:** melhor recuperação de controle **horizontal após acertar**; sem mira automática, correção de trajetória, alteração de altura ou dano extra de queda.
- **Contrajogo:** esquiva lateral, bloqueio/obstáculo, desestabilizar preparação.

### 6.3. Eco do Trovão (situacional/opcional)

- **Preparar:** tridente e maça utilizados validamente em situação com múltiplos adversários.
- **Executar:** impacto de maça depois de preparar contexto de tridente; dispersão moderada de inimigos próximos, com dano secundário mínimo ou nulo.
- **Em tempestade:** se Condutividade **de fato** gerar raio vanilla, só aplicar partículas extras discretas; não invocar raio adicional.
- **Estado:** **opcional**; avaliar redundância com Julgamento do Trovão (tridente individual) e Repulsão (maça individual) antes de implementar.

## 7. Machado + Escudo — Bastião

**Identidade:** defender para preparar ataques pesados; mais lento, de maior comprometimento do que espada + escudo.

### 7.1. Retaliação Soberana → Contraimpacto

- **Preparar:** bloquear ataques reais com escudo, responder com acertos válidos de machado, com peso maior a bloqueios precisos.
- **Executar:** um novo bloqueio válido abre janela para **começar** a carregar ataque pesado; liberar o golpe manualmente.
- **Recompensa:** preparação de carga ligeiramente favorecida, nunca pesada instantânea, sem multiplicar o Golpe Soberano individual.
- **Contrajogo:** recuar durante carga, atacar flanco, explorar exposição do escudo abaixado.

### 7.2. Fortaleza Avançada → Avanço do Bastião

- **Preparar:** bloqueios efetivos e recuperação de terreno com escudo ativo.
- **Executar:** comando de avanço curto frontal, proteção apenas na direção e fase válida da guarda, encerrada **antes** do golpe de machado.
- **Recompensa:** recuperar espaço, não causar dano automático ou invulnerabilidade.
- **Contrajogo:** movimento lateral, machado anti-escudo, ataque por trás.
- **Controle:** não introduzir comando definitivo sem teste de conflitos; usar tecla de guarda configurável para machado + escudo ou modificador documentado.

### 7.3. Ruptura Defensiva → Quebra de Formação

- **Preparar:** bloqueios reais + golpes de machado contra escudo adversário.
- **Executar:** pesado conectado em **bloqueio ativo** especializa a abertura da guarda; não elevar gratuitamente stun/tempo de desabilitação de escudo.
- **Recompensa:** utilidade anti-escudo, não dano massivo.
- **Conflito:** compartilhar consumo com Ruína da Guarda individual do machado.

## 8. Espada + Escudo — Duelista

**Identidade:** bloqueio de timing, reação curta e estocada; não duplicar parry universal.

### 8.1. Contracorte → Resposta Imediata

- **Preparar:** bloqueio de escudo bem cronometrado + resposta de espada válida.
- **Executar:** novo bloqueio preciso + corte carregado manual.
- **Recompensa:** recuperar iniciativa com ligeira melhoria de recuperação; não refletir dano nem aplicar stun.
- **Conflito:** Contra-Corte individual exige **parry**; Resposta Imediata exige **bloqueio real do escudo**.

### 8.2. Ponta Protegida → Estocada Protegida

- **Preparar:** bloqueios válidos seguidos de estocadas (`V`) acertadas.
- **Executar:** bloquear corretamente, abaixar escudo e realizar estocada manual.
- **Recompensa:** deslocamento/recuperação defensiva mais consistente depois do hit.
- **Limite:** escudo **não** protege durante toda a estocada; sem alcance novo, atravessar escudo ou invulnerabilidade.

### 8.3. Desvio de Guarda → Corte de Abertura

- **Preparar:** interceptar golpes com escudo, continuar ofensiva de espada.
- **Executar:** corte carregado válido depois de abrir guarda.
- **Recompensa:** empurrão lateral moderado e controle de posicionamento; apenas em hit realmente válido.
- **Conflito:** não somar Empurrão do escudo como uma segunda força independente no mesmo evento.

## 9. Espada + Besta — Duelista Armado

**Identidade:** ritmo entre espada e disparo inesperado. Preferir espada main-hand + besta carregada off-hand (quando uso permitido) ou hotbar swap. Com besta secundária, clique direito deve priorizar uso da besta; estocada sempre disponível em `V`.

### 9.1. Finta Armada → Corte de Surpresa

- **Preparar:** acertos válidos de espada e projétil de besta ligados ao mesmo alvo; exigir ataque corpo a corpo após disparo, não só equipamento trocado.
- **Executar:** disparo manual válido seguido de estocada (`V`) manual acertada dentro da oportunidade.
- **Recompensa:** recuperação ofensiva curta, sem buff acumulativo com Corte Relâmpago individual.
- **Contrajogo:** esquiva da flecha/foguete, parry/escudo da estocada, recuar durante recarga.

### 9.2. Tiro Repentino → Disparo de Interceptação

- **Preparar:** atingir com espada e usar a besta contra aproximações reais do adversário.
- **Executar:** flecha de besta já carregada, disparada manualmente contra inimigo em avanço, concede knockback direcional moderado.
- **Limite:** foguetes mantêm dano vanilla; não aplicar bônus de knockback em área a cada pessoa atingida por fogos.
- **Contrajogo:** finta de aproximação, esquiva, linha de visão, forçar disparo antecipado.

### 9.3. Último Recurso → Disparo de Emergência (opcional)

- **Preparar:** espada em combate próximo seguida de foguete de besta que efetivamente afete um adversário.
- **Executar:** novo disparo de foguete acertado perto de inimigo em pressão; oportunidade de reposicionamento sem invulnerabilidade.
- **Estado:** opcional até diferenciar da Barragem Explosiva da besta individual e testar auto-dano/colisões.

## 10. Machado + Besta — Coroa Carmesim (homenagem)

**Identidade aprovada:** estilo agressivo e preparado de machado + besta, inspirado na memória de **Technoblade**. A homenagem é expressa por nome, advancement e feedback discreto — **não** criar poderes mágicos, mascotes ou efeitos que destoem de vanilla.

**Nome do estilo:** `Coroa Carmesim`.  
**Advancement obrigatório:** `Technoblade Never Dies` (grafia e maiúsculas exatas).  
**Categoria proposta:** aba **Especiais**, moldura de **challenge**.  
**Descrição sugerida:** `A coroa permanece. Domine o machado e a besta em combate.`  
**Ícone sugerido:** coroa pixel art dourada com detalhes vermelhos, arte original do mod.

### 10.1. Abertura da Coroa → Execução Real

- **Preparar:** acertar besta previamente carregada, trocar para machado e conectar golpe válido no mesmo alvo; demonstrar abertura com projétil e conclusão corpo a corpo.
- **Executar:** disparo manual de besta seguido de pesado de machado completamente executado, dentro da janela.
- **Recompensa:** recuperação ofensiva controlada e consistência da abertura, **não** crítico automático massivo ou dano multiplicado.
- **Contrajogo:** evitar disparo, retirar alcance de machado, punir a carga.

### 10.2. Pressão do Rei → Cerco Carmesim

- **Preparar:** pressão real com machado sobre o escudo/posicionamento adversário, combinada a disparos acertados da besta.
- **Executar:** ataque manual correspondente à oportunidade consolidada, favorecendo fluidez de transição entre as duas armas.
- **Recompensa:** ritmo e recuperação de iniciativa; não aplicar novas desativações de escudo ou dano explosivo adicional.
- **Contrajogo:** quebrar linha de visão, mobilidade lateral, forçar recarga da besta, recuar do machado.

### 10.3. Última Palavra → Disparo do Rei

- **Preparar:** combate válido com machado e uso de besta carregada como resposta a avanço/retaliação do alvo.
- **Executar:** disparo manual de besta na janela de resposta; validar munição e colisão real.
- **Recompensa:** empurrão direcional moderado ou retomada de espaço; **foguetes não ganham dano explosivo adicional**.
- **Contrajogo:** finta de avanço, cobertura, esquiva do projétil, pressão quando a besta está descarregada.

### 10.4. Regras específicas da homenagem e do advancement

1. O desbloqueio deve exigir domínio legítimo do estilo **Machado + Besta** — proposta: completar cada uma das três técnicas contra alvos hostis válidos em combate; parâmetros/configuração de progresso editáveis.
2. A conquista não é liberada por equipar dois itens, disparar no vazio, acertar aliados repetidamente ou criar múltiplas bestas pré-carregadas.
3. `Technoblade Never Dies` aparece exatamente assim, sem traduzir o título. Localização pt-BR e en-US pode traduzir descrição, mas não título.
4. Ao completar, desbloquear os finalizadores do estilo conforme a política de advancements adotada pelo projeto (validar dependência do avanço e evitar remover progressos anteriores).
5. Partículas vermelhas **sutis**, som vanilla de golpe/disparo e indicador pequeno de coroa quando oportunidade estiver pronta; nenhum efeito incessante na tela.
6. O nome homenageia uma figura pública; não incluir falas, músicas, imagens protegidas ou conteúdos de terceiros sem direito de uso.

### 10.5. Controles e conflitos

- **Preferir hotbar swap**: machado na principal, clique direito carrega pesado; trocar para besta e usar clique direito para disparar.
- Não redefinir clique direito do machado ou permitir heavy + disparo simultâneos.
- `Quick Charge`/Carga Rápida seguem vanilla; múltiplos projéteis por um disparo contam como uma ação; vários slots com bestas carregadas não multiplicam oportunidades.
- Escopeta, Grappling Hook, Tiro Múltiplo e foguetes respeitam incompatibilidades da 1.2.2 e o controle de dano em alvo único.

## 11. Outras combinações — backlog de produto, sem implementação obrigatória

| Par | Identidade preliminar | Possível direção | Status |
|---|---|---|---|
| Tridente + Escudo | Guardião das Marés | bloquear e criar distância com tridente | não aprofundado |
| Arco + Espada | Caçador Versátil | disparo válido e estocada após swap | não aprofundado |
| Maça + Escudo | Bastião de Impacto | defesa seguida de reposicionamento vertical com corrente | não aprofundado |
| Lança + Besta | Interceptador | manter distância e disparo contra avanço | não aprofundado |

Não transformar estas quatro possibilidades em features obrigatórias sem nova aprovação; quando implementar, usar o mesmo motor híbrido.

## 12. Controles consolidados e resolução de input

| Configuração | Mão principal | Secundária ou hotbar | Input preservado | Exceção/decisão |
|---|---|---|---|---|
| Espada + Tridente | Espada | Tridente off-hand | esquerda corte, `V` estocada, direita tridente | direita prioriza off-hand neste par; verificar compatibilidade real |
| Espada + Besta | Espada | Besta off-hand ou hotbar | esquerda corte, `V` estocada, direita besta quando off-hand | não disparar automaticamente |
| Machado + Escudo | Machado | Escudo off-hand | direita carrega pesado | **nova tecla de guarda configurável** quando par ativo; não sacrificar heavy |
| Espada + Escudo | Espada | Escudo off-hand | direita escudo, `V` estocada | evitar conflito com estocada por clique direito |
| Machado + Besta | Machado | Besta por hotbar swap | direita heavy com machado; direita dispara com besta | configuração preferencial |
| Spear + Mace | Lança | Maça via hotbar | jab/Charge/Lunge normais; corrente via direita com maça | detectar swap e trajetória; sem tecla híbrida |
| Maça + Tridente | Maça / Tridente alternados | Hotbar swap | direita usa arma selecionada | evitar direito concorrente entre corrente e tridente |

Todas as teclas específicas deverão ser registradas em Controles, respeitar bloqueios globais (stun, elytra, montaria, água/lava conforme mecânica, cooldown) e permitir rebinding. **Não** presumir que a prioridade de off-hand desejada já esteja implementada: inspecionar e testar `use item` no ambiente alvo.

## 13. PvP/PvE, contrajogadas e anti-exploit

1. **PvP:** aplicar sistema de resistência a controles consecutivos da 1.2.2 ao Pull, empurrões especiais e interrupções; nunca encadear imobilização sem contrajogo.
2. **PvE:** pode usar limites ajustados para mobs comuns e chefes, preservando imunidades e mecânicas preexistentes. Não fazer knockback de boss ignorar proteções.
3. **Linha de visão/alcance:** validar servidor-side no instante do acerto; nunca aceitar posição/alcance informado livremente pelo cliente.
4. **Projectile ownership:** guardar entidade/projétil/encantamentos da origem; não contar retorno de tridente que pertence a outro jogador.
5. **Ação versus dano:** explosão de foguete/TNT, Escopeta, MultiShot, ondas da corrente e golpes em área contam uma ação para progresso, sem duplicações.
6. **Sem exploração de slots:** trocar de armas repetidamente não constrói estilo. Equipar várias bestas/tridentes não multiplica cooldown, oportunidade ou progresso.
7. **Sem dano acumulado:** não somar dano de finalizador híbrido a dano de finalizador individual, Breach/Density/Charge multiplicados artificialmente ou dano de raio extra.
8. **Sem autocombos:** hit de um finalizador não agenda um segundo ataque automático ou gatilho recursivo.
9. **Tolerância de alcance:** Golpe Estendido é o único caso de exceção contextual; começar em modo observação, registrar resultados e oferecer chave de configuração para desligar.
10. **Finalização anulada:** ataques sem acerto, sem munição, contra alvo inválido, durante imunidade ou através de parede não aplicam efeitos. Consumo de oportunidade em erro deve seguir a regra da técnica, registrada em testes.

## 14. Parametrização recomendada (sem números finais)

```properties
# Nomes orientativos, adaptar ao sistema de config existente:
hybridCombosEnabled=true
hybridRequireAdvancements=true
hybridOpportunityWindowTicks=<definir_em_teste>
hybridSharedCooldownTicks=<definir_em_teste>
hybridEvidenceWindowTicks=<definir_em_teste>
hybridMaxKnockbackBonus=<definir_em_teste>
hybridAllowFinisherOnMiss=false
pullEnabled=true
pullRequireLoyalty=true
pullTargetResistanceTicks=<definir_em_teste>
pullStrengthLevel1=<definir_em_teste>
pullStrengthLevel2=<definir_em_teste>
pullStrengthLevel3=<definir_em_teste>
spearMaceExtendedReachEnabled=false
spearMaceReachTolerance=<definir_em_teste>
hybridPvPControlsEnabled=true
```

Valores entre `<...>` são **marcadores documentais, não um arquivo `.properties` executável**. Cursor deve escolher defaults conservadores, registrar decisões e permitir ajuste por configuração. Manter as opções anteriores `unlockSpecials` e `drainWater` sem ruptura.

## 15. Organização Java sugerida (adaptar ao repositório real)

```text
combat/
  adaptive/                 # reusar motor e estados individuais da 1.3.0
  hybrid/
    HybridCombatManager.java
    HybridPairRegistry.java
    HybridStyleDetector.java
    HybridOpportunityResolver.java
    HybridCombatState.java
    AerialSequenceTracker.java
    HybridCombatEvent.java
    pairs/
      SpearMaceHybrid.java
      SwordTridentHybrid.java
      MaceTridentHybrid.java
      AxeShieldHybrid.java
      SwordShieldHybrid.java
      SwordCrossbowHybrid.java
      AxeCrossbowHybrid.java
  enchantments/
    PullEnchantmentIntegration.java
  client/
    HybridCombatHud.java
  network/
    HybridStateSync.java
```

A árvore é **ilustrativa**. Não gerar cópias paralelas desnecessárias de gerenciadores já existentes; localizar e reutilizar os módulos atuais de combate, projéteis, tridente, maça acorrentada, escudo e advancement. Minimizar mixins e side effects. Criar testes do motor fora do mundo, quando viável, mais testes de integração multiplayer.

## 16. Plano de migração incremental para Cursor

### Fase 0 — Proteção e inventário

- Criar branch `feature/modifiedpvp-1.3.1-hybrid-combat`; manter commit/tag original como referência. Não sobrescrever arquivos fonte sem diff revisável.
- Mapear estruturas da 1.2.2/1.3.0 reais no repositório; verificar quais features 1.3.0 foram de fato implementadas (o MD **não prova** implementação).
- Rodar build inicial e smoke tests multiplayer; capturar comportamento de slots, mão secundária, heavy e projéteis.

### Fase 1 — Pull e regressão do tridente

- Remover exclusivamente os efeitos de atração e stun acrescentados por Lealdade no mod.
- Registrar Pull, dependências, loot, textos pt-BR/en-US, aparência de livro, validações PvP/PvE e testes de imunidade.
- Garantir que Lealdade sem Pull voltou ao vanilla; Lealdade + Pull aplica deslocamento limitado.

### Fase 2 — Motor híbrido e telemetria

- Reusar eventos do motor adaptativo 1.3.0; inserir identidade de arma/projétil, pares, janela, alvo, trajetória aérea e `actionId`.
- Primeiro registrar e mostrar **somente** classificação/estado (sem efeitos de combate) para verificar falsos positivos.
- Definir resolução global de conflitos entre oportunidades.

### Fase 3 — Protótipos que validam os casos difíceis

1. **Spear + Mace:** tracker aéreo, timing de troca, reconhecimento de Wind Charge/Lunge/smash. Golpe Estendido começa **sem** bônus de reach.
2. **Espada + Tridente:** Lealdade pura versus Pull, identidade de projétil e off-hand.
3. **Machado + Escudo:** tecla de guarda e prioridades de right-click.
4. **Machado + Besta:** besta pré-carregada, foguetes, estilo Coroa Carmesim e advancement.

### Fase 4 — Demais pares aprovados

Implementar Maça + Tridente, Espada + Escudo e Espada + Besta; técnicas marcadas *opcional* permanecem sob feature flags ou fora do build de produção até diferencial e balanceamento serem aprovados.

### Fase 5 — QA e release

- Testes integrados PvP 1v1 e equipes, PvE, mundos locais e servidor dedicado.
- Comparar dano, ticks de controle, range e movimento antes/depois; tunar configurações.
- Confirmar versão 1.3.1 em `fabric.mod.json`, README, changelog, advancements, lang e arquivo do release **somente ao concluir a implementação real**, não ao gerar este MD.

## 17. Critérios de aceitação e matriz de testes

### 17.1. Motor e rede

- [ ] Build limpo; cliente e servidor dedicado iniciam sem erros.
- [ ] Sem dano/knockback executado apenas no cliente; servidor decide os efeitos.
- [ ] `actionId` impede progresso duplicado de hits, explosões, projéteis e retorno.
- [ ] Uma ação não executa dois finalizadores; decisões de prioridade têm testes.
- [ ] Oportunidades cancelam por morte/logout/dimensão e não são armazenáveis por slots múltiplos.
- [ ] Sincronização HUD não reposiciona os ícones já existentes sem solicitação.

### 17.2. Pull

- [ ] Lealdade sem Pull retorna tridente e **não puxa** alvo.
- [ ] Lealdade + Pull funciona somente com acerto elegível e respeita obstáculos e resistência a controle.
- [ ] Pull sem Lealdade, gerado por comando, não ativa.
- [ ] Lealdade e Correnteza não passam a coexistir por causa do mod.
- [ ] Nenhuma cópia de tridente ou retorno duplicado.

### 17.3. Combos individuais/híbridos

- [ ] Finalizadores da 1.3.0 continuam disponíveis e sem empilhamento com os da 1.3.1.
- [ ] Spear + Mace exige smash válido: sem bônus por Lunge no vazio e sem dano por velocidade horizontal.
- [ ] Reach swap não concede alcance permanente, atravessa paredes ou duplica atributos.
- [ ] Espada + Tridente preserva `V`; Pull não adiciona segundo puxão.
- [ ] Maça + Tridente não executa smash automaticamente após puxão.
- [ ] Machado + Escudo permite heavy e defesa por comandos não conflitantes.
- [ ] Espada + Escudo diferencia bloqueio real de parry.
- [ ] Espada + Besta exige disparo manual e respeita munição.
- [ ] Machado + Besta não fortalece foguetes, ignora recarga ou soma Golpe Soberano ao mesmo ataque.

### 17.4. Homenagem Technoblade

- [ ] Advancement tem título **exatamente** `Technoblade Never Dies` em todas as localizações.
- [ ] Advancement pertence à aba Especiais e utiliza formato de desafio.
- [ ] Progresso requer execução legítima das técnicas do estilo Coroa Carmesim.
- [ ] Não desbloqueia com uso no vazio/combate falso e não perde conclusão ao relogar.
- [ ] Ícone original de coroa e partículas/som discretos, sem ativos de terceiros não autorizados.

### 17.5. Balanceamento e regressão

- [ ] Testar latência, combate cruzado, hit simultâneo, anti-cheat e servidores com múltiplos jogadores.
- [ ] Verificar Pull + combo em adversários com e sem dash, escudo, parry, Lunge e elytra.
- [ ] Verificar Wind Burst, Wind Charge, Density, Breach, Chain e impacto da maça no terreno.
- [ ] Testar MultiShot, Escopeta, foguetes com diferentes estrelas e múltiplos jogadores na área.
- [ ] Testar drops, encantamentos em bigorna, loot de Pull, itens legados e dados de mundo existentes.
- [ ] Verificar avanço de Technoblade sem alterações no dano PvP padrão.

## 18. Checklist de entregáveis ao Cursor

O Cursor deve fornecer, ao implementar a 1.3.1:

1. Relatório de **arquivos alterados/criados** com justificativa.
2. Registro de conflitos de input resolvidos, especialmente machado/escudo, espada/tridente e espada/besta.
3. Pull registrado com dependência de Lealdade e loot/documentação.
4. Motor híbrido implementado por extensão da 1.3.0, sem recriar tudo ou apagar seus especiais.
5. Testes automatizados possíveis e roteiro manual de servidor dedicado para cada estilo.
6. Advancement `Technoblade Never Dies` e estilo Coroa Carmesim, com tradução e assets próprios.
7. Tabela de parâmetros ajustáveis e decisões provisórias de balanceamento.
8. Changelog v1.3.1, documentação de migração e notas de compatibilidade.
9. Diff/commits revisáveis que preservem o código e comportamentos originais.
10. Lista explícita de funcionalidades adiadas: técnicas opcionais e combos do backlog.

### Instrução operacional sugerida ao Cursor

> Implemente a proposta do `Modified_PvP_v1.3.1_Especificacao_Combos_Hibridos.md` como incremento da especificação individual 1.3.0 e da implementação real existente. **Primeiro inspecione o repositório** e identifique o que já existe; não presuma que toda a 1.3.0 esteja implementada. Preserve o projeto original, trabalhe em branch, use autoridade do servidor, mantenha comportamento vanilla e controles atuais, resolva conflitos de off-hand de forma explícita, implemente Pull separado de Lealdade, entregue os híbridos priorizados e o advancement `Technoblade Never Dies`. Não aplique números agressivos ou multiplicadores extras sem testes. Técnicas sinalizadas como opcionais/backlog não devem ser incluídas silenciosamente.

---

**Fim da especificação 1.3.1.** Este documento é uma proposta de trabalho: não constitui código implementado, nem substitui o Markdown 1.3.0.
