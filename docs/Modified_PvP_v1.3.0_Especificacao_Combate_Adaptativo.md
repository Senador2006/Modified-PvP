# Modified PvP — Especificação funcional e técnica da versão 1.3.0

> **Status:** implementada na branch `feature/adaptive-combat-1.3.0`. Valores de evidência/recarga ainda calibráveis.  
> **Base:** Modified PvP 1.2.2, Minecraft Java 1.21.11, Fabric, Java 21.  
> **Escopo:** combos e finalizadores adaptativos de todas as armas; **não** inclui ainda o sistema futuro de combos entre armas nem revisão ampla de dash/slide/pulo duplo.  
> **Prioridade:** preservar o projeto original e suas funcionalidades antes de migrar gradualmente a arquitetura.

## 0. Objetivo, decisões aprovadas e restrições

Atualizar os especiais de combate para que o servidor reconheça **o modo como o jogador usa uma arma**, e não apenas a contagem linear de golpes. Essa leitura habilita **uma oportunidade contextual** de executar um finalizador, por tempo limitado, consumida após uma tentativa válida. O ataque permanece manual: não devem ser introduzidos ataques automáticos, teleportes, partículas extravagantes, efeitos mágicos arbitrários ou sequências cinematográficas. Toda a estética deve continuar próxima do Minecraft vanilla.

### Decisões de design consolidadas

- **Espada única:** Corte Relâmpago, Perfuração Precisa e Contra-Corte.
- **Duas espadas:** Corte Cruzado, Rajada Dupla e Corte de Abertura. **Escolhida a Opção A:** preservar controles existentes; detectar a alternância entre ataque principal (esquerdo) e ataque conjunto (direito), **não** criar um botão independente para a mão secundária.
- **Machado:** somente Golpe Soberano e Ruína da Guarda. A inspiração em Escanor se expressa na preparação, peso e ameaça dos impactos; sem magia, sol, explosões ou transformação.
- **Maça:** foco na **maça acorrentada**; Impacto de Retorno e Repulsão. Preservar smash, Wind Burst e técnicas vanilla, sem dar multiplicadores gratuitos para attribute swapping.
- **Tridente:** Arremesso Soberano, Investida das Marés e Julgamento do Trovão, com uso significativo **em qualquer clima** e variantes quando há água/chuva/tempestade e encantamentos compatíveis.
- **Arco:** equilíbrio entre Disparo Certeiro e Ignição Perfeita voltada a Cart PvP; **não** amplificar TNT minecart nem automatizar carrinhos/trilhos/disparo.
- **Besta:** prioridade a foguetes; Barragem Explosiva (grupos) e Descarga Concentrada (alvo individual); flechas, Escopeta e Grappling Hook continuam úteis.
- **Lança:** preservar Pressão, Defesa Rompida, Contra Golpe, Romper, Reação Total e Colapso Circular; acrescentar Investida Implacável explorando Charge e Lunge. **Decisão adotada:** a Investida Implacável usa a **mesma abertura compartilhada** da Defesa Rompida, e não uma segunda barra/carga independente.
- **Escudo:** identidade de **interceptação e respostas ofensivas**; Repulsão, Choque de Escudo e Intervenção (proteção de aliados), com a possibilidade de Resposta Protegida. Sem botão de ataque de escudo obrigatório.

### Restrições inegociáveis

1. Manter a autoridade do **servidor** sobre dano, knockback, hit detection, combos, finalizadores e efeitos; o cliente apenas envia intenção e exibe feedback.
2. Não alterar a física/cálculo vanilla dos ataques da lança (jab, charge, Lunge, alcance mínimo), dos smashes da maça, explosões de TNT minecart e foguetes, nem inventar um novo atributo de dano cinético.
3. Não conceder dois finalizadores simultâneos para a mesma preparação; não dar recompensas a cada projétil de uma rajada como se fossem várias ações independentes.
4. Não fazer finalizadores bypassarem escudo, imunidade entre hits, distância, linha de visão, proteção ou regras atuais de dano sem um requisito explicitamente escrito e testado.
5. Respeitar o balanço PvP/PvE da versão 1.2.2 (parry e resistência a controle), sem voltar ao stun completo e ao reflexo de dano em PvP.
6. Não ampliar neste escopo a movimentação universal nem implementar combos de múltiplas armas. Esses sistemas serão documentados em etapa posterior.
7. Preservar os arquivos originais e criar nova branch para a implementação. Não remover progressão por advancements já conquistados.

---

## 1. Arquitetura do sistema adaptativo

### 1.1. Conceitos e definições

- **Ação (CombatActionEvent):** evento validado pelo servidor, com tipo, arma/família, atacante, alvo opcional, instante/tick, dados de impacto, tags de contexto e um identificador único.
- **Família de arma (WeaponFamily):** SWORD, DUAL_SWORD, AXE, MACE, SPEAR, TRIDENT, BOW, CROSSBOW, SHIELD. Escudo deve ser reconhecido como **equipamento defensivo ativo**, mesmo na mão secundária; isso não autoriza finalizadores simultâneos com a arma principal.
- **Estilo (AdaptiveStyle):** padrão identificado por eventos válidos de uma família (ex.: espada com estocadas ou machado contra bloqueios).
- **Evidência (StyleEvidence):** contribuição de um evento para cada estilo; não equivale a dano nem a um recurso consumível global.
- **Oportunidade (FinisherOpportunity):** um estilo consolidado e travado, com alvo/condições, instante de expiração, arma de origem e categoria de finalizador.
- **Execução (FinisherAttempt):** ação manual elegível que consome ou resolve a oportunidade mediante checagens do servidor.
- **Recarga compartilhada (FamilyCooldown):** bloqueia múltiplos finalizadores da mesma família em sequência, inclusive ao trocar itens/slots.

### 1.2. Máquina de estados (por jogador e família)

`IDLE -> BUILDING -> READY -> CONSUMED -> COOLDOWN -> IDLE`

Transições:

1. `IDLE -> BUILDING`: primeiro evento válido para aquela família, sem recarga ativa.
2. `BUILDING -> READY`: um estilo atinge os critérios específicos de evidência, variedade, tempo e alvo/contexto; selecionar o estilo dominante validado, **sem aleatoriedade**.
3. `READY -> CONSUMED`: tentativa manual elegível do finalizador, com alvo/condições corretas. Aplicar efeitos só se o golpe válido realmente atingir quando a técnica exige acerto.
4. `READY -> IDLE`: expiração, morte, troca de mundo, perda de contexto relevante ou outra condição específica. **Não** aplicar efeito após expiração.
5. `CONSUMED -> COOLDOWN`: iniciar recarga compartilhada, notificar HUD e limpar evidência transitória.
6. `COOLDOWN -> IDLE`: recarga termina. Enquanto isso, nenhuma nova oportunidade pode ser armada para a mesma família.

O disparo do especial não é automático ao entrar em `READY`. O próximo ataque correspondente continua exigindo comando do jogador. Se um estilo exigir acerto, ataque sem alvo não pode ativar efeito de dano; definir por estilo se erro consome a oportunidade (recomendação: sim quando houve tentativa real de execução, não quando o jogador apenas troca de item).

### 1.3. Seleção adaptativa determinística

Cada família pode possuir de dois a três estilos; armazenar uma janela deslizante curta de eventos relevantes, associada à arma e, quando necessário, ao alvo. Critérios importantes:

- Pontuar **acertos válidos**, bloqueios realmente efetuados e contextos relevantes; não pontuar pressionamentos vazios de botão.
- Distinguir `MELEE_HIT`, `THRUST_HIT`, `OFFHAND_COMBINED_HIT`, `HEAVY_FULL_HIT`, `HEAVY_BLOCKED`, `CHAIN_ENTITY_IMPACT`, `CHAIN_BLOCK_IMPACT`, `SPEAR_JAB`, `SPEAR_CHARGE`, `PERFECT_COUNTER`, `SHIELD_BLOCK_TIMED`, `SHIELD_INTERCEPT_ALLY`, `BOW_ARROW_HIT`, `BOW_CART_IGNITION`, `CROSSBOW_FIREWORK_SPLASH`, `TRIDENT_THROW_HIT`, `TRIDENT_LOYALTY_RETURN`, `RIPTIDE_HIT`, `CHANNELING_HIT`.
- Diferenciar **evento de ação** e **evento de dano**: múltiplos alvos do mesmo golpe / explosão / rajada devem compartilhar um `actionId`. Cada estilo define explicitamente como múltiplos alvos contribuem (em geral uma contribuição por ação com atributo `affectedTargets`).
- Estilos que pedem *mesmo alvo* utilizam o UUID do alvo; troca de alvo pode enfraquecer evidência, mas não necessariamente zerar todas as famílias.
- Preferir padrões recentes; evidência antiga decai/expira; não construir especiais fora de combate por ações repetitivas no vazio.
- Em empate entre estilos, manter o estilo anterior enquanto ele for elegível; caso contrário, usar uma ordem de prioridade **documentada**, nunca seleção aleatória.
- Uma vez em `READY`, **congelar o estilo** até resolução/expiração; novos eventos não o substituem silenciosamente.
- Para combater exploração, exigir diversidade/contexto apropriado (ex.: um único evento de shotgun que gera vários projéteis não satisfaz os critérios de vários acertos).

**Parâmetros configuráveis, ainda sem números finais:** tamanho da janela temporal, evidência mínima, pesos por contexto, prazo da oportunidade, perda por erro, janela de resposta, recarga por família, limite de knockback extra e limites de dano PvP. Manter parâmetros em uma camada de configuração, não enterrados em dezenas de mixins. Os valores finais serão decididos após testes.

### 1.4. Modelos de dados sugeridos (pseudocódigo, não copiar cegamente)

```java
record CombatActionEvent(
    UUID actionId,
    UUID playerId,
    WeaponFamily family,
    ActionType type,
    long serverTick,
    @Nullable UUID targetId,
    Set<ContextTag> context,
    int affectedTargets,
    boolean hitValid,
    boolean blocked,
    boolean perfectTiming
) {}

record FinisherOpportunity(
    WeaponFamily family,
    AdaptiveStyle style,
    long expiresAt,
    @Nullable UUID targetId,
    UUID sourceItemIdentity,
    Set<ContextTag> requirements
) {}

class AdaptiveCombatState {
    Map<WeaponFamily, RecentActionWindow> history;
    Map<WeaponFamily, FinisherOpportunity> opportunities;
    Map<WeaponFamily, Long> familyCooldowns;
}
```

`sourceItemIdentity` é **conceitual**: definir identificador seguro da origem ou validar apenas classe/enchantments/slot quando não houver UUID persistente no item; não inventar NBT obrigatório sem necessidade. Esclarecer o comportamento na troca de duas armas idênticas para impedir duplicação.

### 1.5. Prioridades, conflitos e segurança de estado

- `READY` da mão principal e `READY` do escudo **não** podem resolver no **mesmo hit**. O evento é consumido por uma única técnica por ordem: defesa ativa válida -> contra-ataque especializado -> ataque normal; documentação do código deve explicitar qualquer exceção.
- `Parry` e `Contra Golpe` da lança são mutuamente exclusivos conforme 1.2.2. `Choque de Escudo` não se soma a parry no mesmo evento.
- Um finalizador consumido não deve contaminar outro estilo com seu dano/efeito; marcar `isFinisherGeneratedDamage` / source e evitar recursão.
- Eventos causados por mobs, jogadores, projéteis ou explosões somente contam quando o estilo explicitamente aceita aquele tipo.
- Login, logout, morte, troca de dimensão/mundo e desconexão limpam oportunidades e buffs temporários, sem perder os unlocks permanentes.
- Mudanças na versão não devem apagar advancements antigos; criar critérios de migração para especiais e testar saves existentes.

### 1.6. Interface e feedback

Usar HUD, action bar e ícones já existentes no projeto; não adicionar animações cinematográficas. Mostrar: **família ativa**, estilo em desenvolvimento (com progresso discreto), estilo pronto, janela de execução, recarga. Exibir ao jogador mensagens curtas (`Corte Relâmpago pronto`, `Ruína da Guarda pronta`). O cliente não calcula dano nem decide estilo vencedor; recebe snapshot/alterações autorizadas do servidor. Evitar spam de packets e atualização de texto todo tick sem mudança.

### 1.7. PvP versus PvE

Separar curvas e condições parametrizáveis para PvP e PvE. Nunca atribuir stun completo de longa duração no PvP. Limitar empurrões e efeitos de área em grupos para evitar controle infinito. Chefes podem receber efeitos adaptados (menos controle), sem serem totalmente imobilizados.

---

## 2. Espada única — Maestria da Lâmina

**Controles preservados:** ataque esquerdo = ataque vanilla; `V` ou clique direito com espada = estocada; `X` = parry universal. Nenhum novo botão obrigatório.

### 2.1. Ritmo de Corte -> Corte Relâmpago

- **Evidência:** ataques carregados bem-sucedidos, em ritmo consistente; alternar corte e estocada pode fortalecer o estilo, mas não deve ser sequência rígida.
- **Oportunidade:** `RITMO_ATIVO`, criada após continuidade de golpes válidos dentro da janela.
- **Gatilho:** próximo ataque convencional carregado executado manualmente.
- **Resultado:** pequena vantagem de recuperação/velocidade do golpe e dano adicional moderado, **sem contornar invulnerabilidade entre hits** ou aplicar múltiplos hits automáticos.
- **Falha:** golpe executado fora da janela ou erro de mira consome a oportunidade quando caracterizado como tentativa real.
- **Contra-jogo:** sair do alcance, interromper sequência, bloquear ou forçar o jogador a errar.

### 2.2. Ponta de Precisão -> Perfuração Precisa

- **Evidência:** estocadas válidas e direcionadas, preferencialmente no mesmo alvo; considerar linha de visão e alcance da estocada.
- **Oportunidade:** `ALVO_MARCADO`, com UUID do alvo.
- **Gatilho:** nova estocada manual contra **aquele alvo**.
- **Resultado:** benefício moderado de eficiência em alvo protegido; não atravessa escudo completamente, não ignora armadura nem cria dano verdadeiro.
- **Falha:** alvo diferente, fora de alcance, sem linha de visão ou esquiva mantém nenhuma vantagem.
- **Contra-jogo:** reposicionar, bloquear, forçar mudança de alvo.

### 2.3. Resposta de Lâmina -> Contra-Corte

- **Evidência:** parry perfeito melee com espada principal, seguido de resposta ofensiva adequada.
- **Oportunidade:** `CONTRA_ATAQUE`, mais curta e com prioridade contextual sobre estilo ofensivo ainda **não pronto**.
- **Gatilho:** ataque carregado manual dentro da janela contra o agressor válido.
- **Resultado:** recuperação/iniciativa favorável e bônus ofensivo pequeno, **sem novo stun/reflexo**.
- **Contra-jogo:** recuar após parry, bloquear segundo golpe, controlar a distância.

**Encantamento Alcance:** respeitar atual alcance da espada e da estocada; não empilhar multiplicadores ilimitados no finalizador.

---

## 3. Duas espadas — Dança das Lâminas (Opção A obrigatória)

**Controles existentes preservados:** esquerdo = ataque principal; direito com espada em ambas as mãos = **ataque conjunto** (principal + secundária, com dano secundário reduzido). Não introduzir controle independente de offhand. A expressão *alternância de mãos* significa aqui **alternância de padrões entre ataque principal e ação conjunta**, não cliques para comandar isoladamente cada braço.

### 3.1. Alternância Perfeita -> Corte Cruzado

- **Evidência:** alternar acertos de ataque principal e acertos de ataque conjunto em sequência coerente; considerar estocada como evento distinto sem contabilizar dois hits por ação conjunta.
- **Oportunidade:** `SINCRONIA`.
- **Gatilho:** próximo ataque conjunto válido a curta distância.
- **Resultado:** um golpe combinado com dano agregado controlado; resolver como ação única do ponto de vista da janela de invulnerabilidade, evitando dano duplicado por mixins concorrentes.
- **Risco:** falha no ataque conjunto interrompe a sequência e abre recuperação moderada.

### 3.2. Pressão Alternada -> Rajada Dupla

- **Evidência:** sequência ofensiva válida focada em mesmo alvo, com ações conjuntas e principais, sem exigir alternância perfeita.
- **Oportunidade:** `OFENSIVA_CONTINUA`.
- **Gatilho:** ação conjunta manual.
- **Resultado:** dois contatos visuais/funcionais curtos, mas **não** dois danos completos sobrepostos ao mesmo alvo; segundo contato respeita as janelas de dano e pode falhar/esbarrar na defesa.
- **Diferença de Corte Cruzado:** recompensa ritmo de ofensiva, não dano concentrado máximo; implementar de forma comprovadamente distinguível no gameplay.

### 3.3. Abertura Cruzada -> Corte de Abertura

- **Evidência:** ataque principal bloqueado com validade defensiva seguido de ação conjunta que encontra abertura; jamais pontuar ataque no vazio.
- **Oportunidade:** `GUARDA_DESVIADA` associada ao defensor.
- **Gatilho:** próximo ataque principal carregado contra alvo válido.
- **Resultado:** pequena vantagem situacional contra alvo que acabou de defender; **não** atravessa escudo levantado nem substitui o papel anti-escudo do machado.

**Encantamento Golpe Duplo:** manter compatibilidade e reavaliar seu bônus de velocidade e dano da offhand para não duplicar recompensas do novo sistema.

---

## 4. Machado — Supremacia

**Identidade:** inspiração em Escanor = peso, risco, preparação, imponência; sem poderes solares. Preservar clique direito segurado para ataque pesado, barra de carga, varredura atual limitada e compromisso de recuperação. Dash/slide não cancelam livremente carga/recuperação já conforme 1.2.2.

### 4.1. Força Dominante -> Golpe Soberano

- **Evidência:** ataques pesados suficientemente carregados que conectam diretamente com um alvo; golpes bloqueados não pontuam preferencialmente este estilo.
- **Oportunidade:** `DOMINIO`.
- **Gatilho:** soltar ataque pesado bem carregado contra alvo válido.
- **Resultado:** ataque concentrado com dano extra moderado e empurrão controlado, sem stun longo. Valorizar som/partículas vanilla.
- **Risco:** carregamento anunciável e recuperação após erro; impedir cancelamento repetitivo para simular golpes instantâneos. Cancelar antecipadamente não conserva bônus de carga arbitrariamente.

### 4.2. Ruptura Defensiva -> Ruína da Guarda

- **Evidência:** ataques pesados efetivamente interceptados por escudos válidos; verificar se bloqueio realmente ocorreu, não somente se o alvo carrega escudo.
- **Oportunidade:** `GUARDA_COMPROMETIDA`, vinculada ao alvo quando aplicável.
- **Gatilho:** ataque pesado carregado contra escudo ativo.
- **Resultado:** maior eficiência de quebra/interrupção da guarda e abertura curta; **não** igualar o dano bruto do Golpe Soberano em alvo desprotegido.
- **Risco:** alvo que para de bloquear torna a técnica menos útil; adversário pode esquivar do golpe lento.

**Encantamento Corte:** verificar stacking com pesado e finisher antes de ajustar dano PvP; não permitir ataque pesado instantâneo.

---

## 5. Maça — Domínio da Corrente

Preservar a maça acorrentada (arremesso, choque ao acertar entidade/bloco, onda, autopropulsão pelo chão, retorno, durabilidade, cooldown, slot e comportamento ao morrer). Smashes, Wind Burst, Density e Breach vanilla devem continuar operando com suas condições normais.

### 5.1. Precisão da Corrente -> Impacto de Retorno

- **Evidência:** arremessos com acerto **direto** em entidade, seguidos do retorno normal ao jogador e recuperação legítima da arma; não contar múltiplos alvos de uma mesma onda como acertos diretos.
- **Oportunidade:** `CORRENTE_PREPARADA`.
- **Gatilho:** próximo arremesso direto validado.
- **Resultado:** acelera moderadamente o **retorno/recuperação após impacto válido**, permitindo retomar combate; empurrão extra, se houver, deve ser mínimo. **Não** disparar uma segunda maça nem duplicar dano.
- **Risco:** a arma permanece ausente enquanto está no ar; falha não gera retorno acelerado especial; garantir recuperação de slot consistente mesmo sob lag.

### 5.2. Controle de Impacto -> Repulsão

- **Evidência:** arremessos contra superfícies que efetivamente causam deslocamento útil em mobs/jogadores ou no usuário; não ganhar progresso atirando no vazio repetidamente.
- **Oportunidade:** `IMPACTO_PREPARADO`.
- **Gatilho:** impacto válido em **bloco** com maça acorrentada.
- **Resultado:** onda de empurrão contextual: perto do inimigo = afastamento; perto dos pés = impulso vertical controlado; entre inimigos = dispersão, sem multiplicar dano por alvo.
- **Risco:** mirar/posicionar; sem proteção gratuita contra queda. Subsequentemente um smash vanilla só é válido conforme altura/queda real.

**Técnicas PvP existentes:** spear-mace, elytra-mace e attribute swapping não devem receber buff direto de Breach/Density/dano. Validar separadamente a ordem de atributos no servidor e impedir que a habilidade reaplique encantamento de item anterior.

---

## 6. Tridente — Soberania dos Mares e Tempestades

Meta: útil em clima seco, chuva, água e tempestade. **Não invocar relâmpago real fora das condições vanilla.** Preservar incompatibilidades/condições de Lealdade, Correnteza e Condutividade. O efeito anterior de Lealdade (puxar alvo e deixá-lo Desequilibrado em PvP) precisa ser compatibilizado, não empilhado com outro controle ininterrupto.

### 6.1. Retorno Soberano -> Arremesso Soberano

- **Evidência:** arremessos diretos precisos + recuperação legítima por Lealdade.
- **Oportunidade:** `RETORNO_PREPARADO`.
- **Gatilho:** próximo arremesso em alvo válido.
- **Resultado:** deslocamento/empurrão controlado e retorno um pouco mais eficiente; controlar combinação com puxão de Lealdade atual para **não aplicar puxão e empurrão contraditórios ao mesmo tempo**. Definir precedência: preferir a mecânica existente e aplicar somente efeito compatível.
- **Ambiente:** qualquer clima quando encantamento adequado disponível.

### 6.2. Domínio das Marés -> Investida das Marés

- **Evidência:** golpes corpo a corpo e arremessos posicionados corretamente; com Correnteza, registrar investidas e acertos em movimento válidos.
- **Oportunidade:** `MARE_CRESCENTE`.
- **Gatilho/variante em terra:** golpe manual de tridente que favoreça o afastamento/controle da distância **sem novo dash ou Lunge de lança**.
- **Gatilho/variante em água/chuva:** impacto válido durante Correnteza, recompensado com recuperação/reposicionamento limitado.
- **Restrições:** Correnteza apenas onde vanilla permite. Não inventar água, onda ou mobilidade extra em clima seco.

### 6.3. Domínio da Tempestade -> Julgamento do Trovão

- **Evidência:** arremessos diretos contra alvo sob pressão; quando Condutividade é realmente acionada, registrar contexto `CHANNELING_VALID`.
- **Oportunidade:** `JULGAMENTO_PREPARADO`.
- **Gatilho/sem tempestade:** arremesso válido gera benefício de impacto físico concentrado, sem raio, stun completo ou dano elétrico inventado.
- **Gatilho/com tempestade + Condutividade:** raio **vanilla** no alvo se todas as condições forem satisfeitas. Possível empurrão secundário discreto; sem criar múltiplos raios ou dano elétrico duplicado.
- **Conflito com demais estilos:** usar recarga compartilhada do tridente, e não acumular três cargas meteorológicas.

---

## 7. Arco — Precisão e Cart PvP

Preservar flechas de vários tipos em bundle, seleção por `G`, aceleração com offhand e Flecha Dupla com dano secundário mitigado em PvP. O especial atual de disparos instantâneos deve migrar para duas oportunidades contextuais sem garantir todos os tiros instantâneos.

### 7.1. Mira Perfeita -> Disparo Certeiro

- **Evidência:** acertos válidos com flechas carregadas, preferencialmente em alvo móvel; evitar farm de alvos invulneráveis ou dano zero.
- **Oportunidade:** `MIRA_ESTAVEL` com referência ao alvo se relevante.
- **Gatilho:** próximo disparo carregado manual.
- **Resultado:** recompensa moderada de velocidade/consistência balística e impacto, sem mira automática; limitar alterações de velocidade para não criar interação explosiva involuntária com TNT minecarts.
- **Exclusão explícita:** bônus balístico de Disparo Certeiro **não** se aplica quando a flecha detona TNT minecart ou outro explosivo.

### 7.2. Detonação Precisa -> Ignição Perfeita

- **Evidência:** o **servidor** confirma ignição/detonação de TNT minecart realmente causada por flecha flamejante do jogador; não basta acertar carrinho inativo sem ignição, nem contar repetidamente explosão de um único carrinho.
- **Oportunidade:** `IGNICAO_PREPARADA`.
- **Gatilho:** nova detonação válida, pelo jogador, dentro da janela.
- **Resultado:** **janela de retomada ofensiva** — uma redução controlada do tempo de preparação do **próximo disparo convencional**, ou feedback de precisão. Sem alteração de dano, velocidade da flecha de ignição, potência de carrinho, raio ou timing da explosão.
- **Mira de Ignição (opcional):** indicação discreta quando flecha com Chama pode atingir diretamente TNT minecart; não há mira automática, simulação de trajetória, posicionamento automático de trilhos/carrinho ou previsão de área de explosão.
- **Segurança PvP:** não fazer flecha acelerada da técnica de precisão influenciar explosão do carrinho. Distinguir `arrowHitEntity`, `cartIgnited` e `explosionDamage` via causalidade confiável, sem conceder progresso por explosão de outro jogador.

---

## 8. Besta — Artilharia Explosiva

Priorizar **foguetes**, mantendo disparo pré-carregado, flechas, Escopeta, Grappling Hook e incompatibilidades atuais. Criar diferenciação entre foco de grupos e foco em alvo. Não duplicar dano das estrelas dos foguetes nem permitir múltiplos efeitos especiais por tiro Multishot/Escopeta.

### 8.1. Bombardeio -> Barragem Explosiva

- **Evidência:** ações de disparo de foguete que **efetivamente afetaram** um ou mais adversários com explosão válida; para múltiplos alvos, registrar contagem agregada de alvos afetados com **um actionId**.
- **Oportunidade:** `BOMBARDEIO_PREPARADO`.
- **Gatilho:** próximo disparo de foguete com explosão válida.
- **Resultado:** empurrão **de dispersão**, moderado e sujeito ao limite por alvo/evento; **não** aumentar dano, raio, estrelas, número de foguetes, explosões ou ignorar obstáculos.
- **Contra-jogo:** cobertura, espalhar o grupo, interromper recarga e evitar agrupamento.

### 8.2. Precisão explosiva -> Descarga Concentrada

- **Evidência:** explosões de foguetes com acerto próximo/direto no **mesmo alvo** em diferentes ações válidas.
- **Oportunidade:** `DESCARGA_PREPARADA` com UUID do alvo.
- **Gatilho:** foguete direcionado a esse alvo, com impacto válido.
- **Resultado:** empurrão mais direcionado/previsível no alvo principal, sem segundo dano explosivo, sem stun longo e sem raio de dano extra.
- **Contra-jogo:** esquiva, escudo/cobertura conforme vanilla, troca de distância/posição.

### 8.3. Compatibilidades

- **Escopeta:** cada tiro carregado pode consumir múltiplas munições, mas seus projéteis compartilham **uma ação** de progressão; bônus de dano adicional em mesmo jogador continua reduzido.
- **Tiro Múltiplo:** projéteis da mesma execução não multiplicam progresso/finalizador. Garantir uma aplicação agregada de efeito por alvo por ação.
- **Grappling Hook:** mantém mobilidade e interrupção por dano/obstáculo; **não** alimenta estilos explosivos.
- **Carga Rápida:** não deve permitir recarga instantânea ilimitada por sinergia com outra habilidade.

---

## 9. Lança — Domínio da Distância

**Preservar arquitetura e funcionamento existente:** `SpearCombat`, `SpearPressure`, `SpearRules`, Contra Golpe, Pressão par atacante-alvo, Defesa Rompida, limites PvP/PvE, imunidade após ruptura, Romper, Reação Total (`Z`) e Colapso Circular (jab). O `jab`, `charge`, `Lunge` e alcance mínimo vanilla continuam intactos.

### 9.1. Leitura de estilo por contribuição à mesma Pressão

A única barra de Pressão permanece. Registrar **metadados dos eventos que a alimentaram**, por par atacante-alvo, para identificar o perfil recente:

- `COUNTER`: Contra Golpe perfeito e riposta válida -> estilo reativo.
- `KINETIC`: Charge que realmente acerta com velocidade relativa válida -> estilo cinético.
- `FORMATION`: jab em alcance adequado, controle de distância e interações consistentes com alvo -> estilo de formação.

**Importante:** não criar três barras, três rupturas, três buffs prontos ou três aberturas. Pressão ao máximo aplica **uma** Defesa Rompida. A classificação adaptativa modifica a indicação/preferência de execução, mas não autoriza três finalizadores empilhados.

### 9.2. Contra Golpe -> Reação Total

- Preservar postura frontal, linha de visão, janela perfeita, riposta controlada em PvP/PvE e distinção em relação ao parry.
- Se o perfil dominante for reativo e houver Defesa Rompida elegível, `Z` pode executar Reação Total contra o alvo marcado conforme regras existentes.
- Manter limites de dano PvP, recarga, amplificação condicional de Romper e vulnerabilidade de chefes; não duplicar estocadas automáticas.

### 9.3. Investida Cinética -> Investida Implacável (nova)

- **Evidência:** impactos válidos de Charge com velocidade relativa relevante, incluindo combinações **manuais** com Lunge que respeitem o tempo real de ativação da Charge.
- **Oportunidade:** **Defesa Rompida compartilhada** com os outros finalizadores, após Pressão acumulada; perfil `KINETIC` sugere a nova opção.
- **Gatilho:** Charge manual válida contra o alvo marcado durante a janela de Defesa Rompida; se não houver Charge válida, **não** converter jab comum em Investida Implacável.
- **Resultado:** deslocamento direcional/knockback mais controlado após impacto cinético; **não** multiplicar novamente o dano com a velocidade, não somar Lunge como multiplicador de dano separado e não criar aceleração artificial.
- **Consumo:** usa e consome exatamente a mesma Defesa Rompida e recarga compartilhada de Reação Total / Colapso Circular.
- **Risco:** errar investida ou desacelerar pode perder a abertura; defesa e esquiva do alvo ainda funcionam.
- **Implementação crítica:** rastrear começo de jab, Lunge, ativação de Charge e efetiva velocidade relativa **servidor-side**. Validar que o início da Charge ocorreu depois de condições vanilla; não presumir que Lunge automaticamente ativa Charge.

### 9.4. Formação Rompida -> Colapso Circular

- Preservar gatilho jab com Defesa Rompida, efeito em área com dano secundário limitado a jogadores e proibição da riposta automática disparar giro.
- Estilo `FORMATION` pode indicar a opção; execução ainda precisa de jab real e alvo marcado.
- Uma única ruptura é consumida; não permitir que a onda de Colapso Circular gere nova pressão artificial.

### 9.5. Resolução de escolha e conflito de botões

- `Z` mantém a semântica atual: Contra Golpe quando não existe finalização elegível, Reação Total quando há abertura e as condições atuais.
- **Investida Implacável** dispara pela Charge **natural** durante uma abertura elegível; não criar tecla adicional.
- **Colapso Circular** dispara pelo jab **natural** na abertura elegível.
- A precedência deve impedir duplo gatilho se uma mesma ação passar por hooks de jab/charge/finisher; identificar `actionId`, categoria de evento e consumo atômico da `Opening`.
- O estilo dominante é uma sugestão/forma de preparação, não um bloqueio invisível que impeça o jogador de escolher manualmente outra execução válida durante a única abertura. Essa decisão mantém versatilidade e controles vanilla.

---

## 10. Escudo — Defesa Ativa e Interceptação

**Identidade principal:** interceptar ataques e criar oportunidades de resposta com a arma principal, mantendo proteção solo e em equipe. Controles vanilla: levantar escudo e bloquear. Sem botão extra obrigatório e sem escudo arremessável. Preservar a remoção do empurrão passivo contínuo da 1.2.2.

### 10.1. Guarda Inabalável -> Repulsão

- **Evidência:** bloqueios reais e eficazes em sequência; segurar escudo sem receber hit não pontua.
- **Oportunidade:** `GUARDA_FIRME`.
- **Gatilho:** próximo bloqueio válido contra um atacante na área frontal.
- **Resultado:** empurrão aprimorado e controlado para recuperar distância; **sem dano de escudo alto**, sem empurrão contínuo por tick.

### 10.2. Interceptação -> Choque de Escudo

- **Evidência:** levantar escudo dentro de uma janela de timing antes de um ataque **melee** que é de fato bloqueado. O servidor deve medir início real do uso de escudo e tick do dano, não confiar em timestamp do cliente.
- **Oportunidade:** `INTERCEPTACAO_PREPARADA`.
- **Gatilho:** bloqueio melee cronometrado corretamente.
- **Resultado:** interrupção curta da continuidade ofensiva + leve empurrão, **sem stun completo e sem multiplicar dano refletido**.
- **Exclusões:** não ativa contra explosão, projétil ou ataque não bloqueável; não soma Choque de Escudo e parry no mesmo evento.
- **Contra-jogo:** ataque pesado de machado, finta, recuo, ataque pelas costas, quebra de postura.

### 10.3. Guarda de Proteção -> Intervenção

- **Evidência:** bloqueio válido de projétil/melee cuja trajetória de fato ameaçava um **aliado**, com jogador entre ameaça e alvo. Não inventar hitbox ampliada nem identificar qualquer pessoa próxima como protegida.
- **Oportunidade:** `GUARDA_PROTETORA`.
- **Gatilho:** próxima interceptação real com escudo.
- **Resultado:** recuperação defensiva/reposicionamento curto para manter a proteção, não invulnerabilidade de área e não reflexão automática.
- **Requisitos técnicos:** definição de aliado via time/party configurável (scoreboard teams quando viável); fallback solo: essa técnica não progride sem aliado validamente ameaçado, mas as outras continuam disponíveis.

### 10.4. Resposta Protegida — elo futuro com outras armas

- **Geração:** após bloqueio **preciso** que aciona Choque de Escudo, conceder uma **breve oportunidade de resposta** ao usuário.
- **Uso agora:** permite um ataque convencional da mão principal com recuperação defensiva razoável, **sem dano extra de combo entre armas**.
- **Limites:** não autoativar Contra-Corte da espada, Contra Golpe da lança, Golpe Soberano, nem outro finisher. Cada um possui sua própria condição.
- **Futuro 1.3.x:** esse evento pode servir de gatilho para combos de arma mista, em documento separado.

### 10.5. Exclusividade de defesa

Prioridade para um mesmo evento: defesa pela mecânica que efetivamente recebeu a ação (`parry`, `spear guard` ou `shield block`), nunca dupla resolução. `SHIELD_BLOCK_TIMED` exige shield efetivamente levantado e hit bloqueado. `Interrompido`, `Desequilibrado`, `Guarda Exposta` da 1.2.2 e resistência curta a controle permanecem válidos.

---

## 11. Desbloqueios e progressão

- Manter a aba **Especiais** dos advancements e o progresso permanentemente desbloqueado na migração 1.2.2 -> 1.3.0.
- Quem já concluiu o advancement base de uma arma pode utilizar suas novas variantes adaptativas sem repetir kills/objetivos por variante. **Recomendação padrão**; caso o design mude, exigir migração compatível e informar ao jogador.
- `unlockSpecials=true` da configuração do servidor segue válido para arenas, sem conceder progresso persistente indevido.
- Exibir novos nomes e descrições por arquivos de tradução (`pt_br`, `en_us` se existentes), sem strings fixas no código.
- Não criar grind obrigatório separado para cada finalizador; a habilidade deve estar na execução, não no farm.

---

## 12. Caminho de implementação no código existente

Arquivos/classes identificados no ZIP fornecido, apenas como **pontos de integração para análise**, não como instrução para reescrever tudo:

| Área | Código existente relevante | Direção |
|---|---|---|
| Combos/unlocks | `combat/ComboSpecials.java`, `combat/ComboData.java` | Extrair detector adaptativo; manter advancements/cooldowns e migrar dados |
| Ataques, parry e pesado | `combat/CombatActions.java`, `combat/CombatantState.java`, `combat/CombatRules.java` | Emitir eventos tipados nos pontos já validados |
| Lança e pressão | `combat/SpearCombat.java`, `combat/SpearPressure.java`, `combat/SpearRules.java` | Anexar tags de estilo à pressão e adicionar execução cinética sem duplicar abertura |
| Maça acorrentada | `combat/MaceChain.java` | Detectar impacto direto/bloco, retorno e resolução segura do slot |
| Escudo | `combat/ShieldFeatures.java`, mixins de block/use | Medir timing de bloqueio e separar interceptações |
| Rede e cliente | `network/CombatActionPayload.java`, `network/SpearStatePayload.java`, `client/CombatClient.java`, `client/SpearHud.java` | Estender sincronização mínima de oportunidade e feedback visual |

### Organização sugerida (aditiva, não substitutiva)

```text
combat/adaptive/
  AdaptiveCombatManager.java
  AdaptiveCombatState.java
  CombatActionEvent.java
  CombatActionType.java
  AdaptiveStyle.java
  WeaponFamily.java
  EvidenceEvaluator.java
  OpportunityResolver.java
  AdaptiveBalanceConfig.java
  handlers/
    SwordAdaptiveHandler.java
    DualSwordAdaptiveHandler.java
    AxeAdaptiveHandler.java
    MaceAdaptiveHandler.java
    TridentAdaptiveHandler.java
    BowAdaptiveHandler.java
    CrossbowAdaptiveHandler.java
    SpearAdaptiveHandler.java
    ShieldAdaptiveHandler.java
```

**Observação:** nomes e diretórios são sugestão, não obrigação. Priorizar integração com os padrões Fabric/Minecraft já usados no repositório e evitar alteração ampla de mixins de terceiros. Preferir eventos/encapsulamentos nos módulos atuais a múltiplos mixins concorrendo pelo mesmo método.

### Migração incremental — ordem recomendada

1. Criar branch `feature/adaptive-combat-1.3.0` e preservar tag/artefato 1.2.2.
2. Escrever infraestrutura: actionId, eventos normalizados, estado, TTL/cooldown, sync server->client e log de debug.
3. Migrar **espada única e dupla (Opção A)** e validar comportamento de ataque combinado.
4. Migrar machado, testando carregamento, bloqueio, cancelamento e armadura.
5. Migrar maça, testando entidade/bloco, retorno, morte, slot e smash vanilla.
6. Migrar tridente, arco e besta; checar explosões, multishot, Lealdade e Correnteza.
7. Integrar lança à Pressão já existente e implementar Investida Implacável com abertura compartilhada.
8. Migrar escudo e testar prioridade contra parry/Contra Golpe.
9. Fazer revisão PvP, PvE, teste multiplayer e compatibilidade de saves; só então remover caminhos antigos que se tornaram obsoletos.

**Não aplicar um refactor total de uma vez.** Cada migração deverá compilar, preservar os controles e possuir testes de regressão antes da próxima arma.

---

## 13. Critérios de aceitação e testes

### Testes do motor adaptativo

- [ ] Dois jogadores com o mesmo histórico de eventos recebem o mesmo estilo (determinismo).
- [ ] Estilo travado em READY não muda após outro golpe até consumir/expirar.
- [ ] Evento duplicado com o mesmo `actionId` é ignorado.
- [ ] Uma rajada Multishot/Escopeta alimenta progressão como **uma ação**.
- [ ] Recargas continuam por família e não podem ser contornadas por troca de slot, logout/login, troca de dimensão ou itens idênticos.
- [ ] Estado temporário some em morte/logout/mudança de mundo; advancements continuam.
- [ ] O cliente não consegue afirmar um hit, forjar estilo ou acionar dano só enviando um payload.
- [ ] Finalizador expirado não aplica dano/controle extra e HUD não mostra READY incorretamente.

### Testes por arma

- [ ] **Espada:** corte, estocada e parry geram as três técnicas sem dupla ativação; defesa e alcance funcionam.
- [ ] **Duas espadas:** clique direito continua golpe combinado; clique esquerdo principal; nenhum botão de offhand independente; golpes de duas mãos não duplicam dano/contagem.
- [ ] **Machado:** golpe pesado mantém tempo de carregamento e recuperação; Ruína da Guarda depende de bloqueio real; Golpe Soberano não substitui quebra de guarda.
- [ ] **Maça:** acerto em entidade versus bloco identifica estilos corretos; retorno ao mesmo slot; morte com maça lançada; queda e Wind Burst vanilla não são falsificados.
- [ ] **Tridente:** clima seco com estilo físico; Condutividade somente nas condições vanilla; Lealdade não aplica puxão e empurrão contraditórios; Correnteza permanece restrita.
- [ ] **Arco:** flechas convencionais, offhand, bundle e tecla `G` preservados; TNT minecart não ganha explosão extra ou velocidade de flecha indevida; detonação creditada ao atirador correto.
- [ ] **Besta:** foguetes em área contam uma única ação; empurrão máximo por alvo/tick controlado; Escopeta e Grappling permanecem incompatíveis; múltiplas estrelas não são duplicadas.
- [ ] **Lança:** Reação Total, Colapso Circular e Investida Implacável competem por **uma** Defesa Rompida; `Z` mantém função anterior; jab/charge/Lunge preservados.
- [ ] **Escudo:** levantar no momento correto é registrado pelo servidor; bloqueio passivo não ativa interceptação; não empilha parry + escudo; proteção de aliados exige trajetória real.

### Balanceamento exploratório (sem fixar valores aqui)

- [ ] Duelo 1x1 com itens equivalentes: nenhum finisher deve decidir sozinho a luta sem oportunidade de reação.
- [ ] PvP em grupo: empurrões/controle têm limite de aplicação e resistência a controle consecutivo.
- [ ] PvE: mobs comuns e chefes continuam funcionalmente diferentes conforme regras 1.2.2.
- [ ] Lag/ping diferentes não geram dano duplicado, hits fantasmas ou retorno perdido da maça.
- [ ] Som e partículas vanilla identificam finalizador, mas não obscurecem visão ou geram efeitos de tela excessivos.

---

## 14. Escopo explicitamente adiado

**Não implementar neste pacote:** combos entre armas diferentes (ex.: escudo -> espada; lança -> maça; arco/cart -> espada), novos combos cooperativos de dois/três jogadores, reformulação do dash/slide/pulo duplo, árvores complexas de habilidades, novas animações de corpo inteiro, sistema global de stamina, poderes elementais independentes do vanilla, novos multiplicadores de TNT minecart ou foguetes. Esses temas serão planejados **após** validar este núcleo adaptativo.

## 15. Entregáveis solicitados ao Cursor

1. Plano de arquivos afetados e riscos de regressão antes de editar o código.
2. Implementação incremental sem remover as funcionalidades 1.2.2 até os equivalentes 1.3.0 estarem validados.
3. Configuração central de ajustes PvP/PvE (valores a calibrar), logs opcionais de estilo e feedback minimalista.
4. Migração segura de progresso/advancements; documentação breve de mudanças no README e changelog 1.3.0.
5. Checklist de testes manuais e, quando viável, testes automatizados dos detectores e resolvers.
6. Build e verificação das incompatibilidades; apresentar limitações concretas detectadas e **não fingir** que efeitos não implementados foram concluídos.

### Instrução operacional para o Cursor

> **Versão alvo: 1.3.0 — Sistema de Combate Adaptativo.** Leia esta especificação e inspecione os arquivos Java existentes antes de implementar. Preserve o comportamento 1.2.2 como baseline. Entregue primeiro a infraestrutura de eventos/estados e implemente uma família de armas por etapa, com compilação e testes a cada etapa. Não criar atalhos que causem dano no cliente, não automatizar golpes nem adicionar animações extravagantes. Se alguma proposta depender de hooks inviáveis na versão Minecraft/Fabric usada, documente o obstáculo e apresente um fallback vanilla+ antes de alterar a mecânica.

---

**Estado deste documento:** especificação para discussão e posterior uso no Cursor. Nenhum arquivo-fonte do mod foi modificado na elaboração deste Markdown.
