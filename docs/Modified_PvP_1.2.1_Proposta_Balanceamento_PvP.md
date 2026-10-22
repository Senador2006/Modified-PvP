# Modified PvP — Proposta de balanceamento para PvP

**Base analisada:** documentação da versão 1.2.1 — Minecraft 1.21.11 / Fabric.  
**Status:** P0 e P1 da proposta foram implementados na **1.2.2**. P2 (ataques contextuais, finalizadores novos por arma) ficou para depois dos testes. Os números desta etapa são valores de trabalho, não o balanceamento final.  
**Escopo:** combate entre jogadores: armas, movimentação, defesa, combos, finalizações, encantamentos, progressão e experiência de combate.  
**Limite desta etapa:** não fixar valores de dano, durações, multiplicadores ou recargas. Ajustar esses parâmetros somente depois de testes.  
**Preservação:** manter como referência a versão 1.2.1 e não substituir as mecânicas originais sem validação.

## 1. Objetivos e princípios

- Tornar as lutas competitivas e legíveis, sem eliminar a velocidade característica do mod.
- Recompensar precisão, posicionamento, timing e escolhas, em vez de sequências automáticas ou bônus de dano desproporcionais.
- Preservar a identidade de cada arma: espada fluida, duas espadas agressivas, machado de compromisso alto, lança de contra-ataque, maça de controle espacial, arco de precisão, besta de preparação, tridente de deslocamento e escudo de defesa ativa.
- Permitir contra-jogo: toda mecânica forte precisa de sinal visual, risco, alguma resposta possível e uma janela de recuperação.
- Evitar mortes inevitáveis originadas de um único stun, cadeia de empurrões ou finalizador.
- Balancear PvP e PvE separadamente sempre que os mesmos efeitos criarem problemas competitivos.
- Preferir regras claras e poucas exceções escondidas. Nenhuma habilidade deveria exigir conhecer o código para entender por que o combate foi decidido.

**Critério de revisão:** cada seção usa **Manter**, **Alterar**, **Adicionar** ou **Remover/evitar**. São recomendações, não descrição do estado atual.

## 2. Problemas transversais

### 2.1 Atordoamento e perda de controle

**Estado documentado:** parry aplica atordoamento completo e machado, tridente e outros especiais também podem impedir ação ou movimento.

- **Alterar:** em PvP, substituir parte dos stuns absolutos por efeitos mais específicos: interrupção breve, desequilíbrio, fraqueza de guarda, redução de mobilidade ou impedimento de usar apenas a habilidade que foi neutralizada.
- **Adicionar:** resistência temporária a controles consecutivos, para impedir que múltiplos jogadores mantenham um alvo permanentemente travado.
- **Adicionar:** estados com feedback claro: *Interrompido*, *Desequilibrado*, *Guarda Exposta*; cada um deve afetar algo diferente.
- **Remover/evitar:** acúmulo irrestrito de stun, knockback, puxões e impedimento de itens sobre o mesmo jogador.
- **Manter:** controle forte contra mobs comuns, se desejado, com regras distintas contra chefes.

### 2.2 Dano explosivo e finalizações

- **Alterar:** avaliar o dano combinado de encantamentos, Força, acertos críticos, finalizadores e buffs; não analisar cada ataque isoladamente.
- **Adicionar:** regras de dano PvP dedicadas aos especiais e às fontes de dano secundário, sem necessariamente reduzir os ataques básicos do Minecraft.
- **Adicionar:** consumo explícito de aberturas: um estado que libera finalizador não deve permitir várias execuções acumuladas por ataques simultâneos.
- **Remover/evitar:** finalizadores que eliminem automaticamente alvos saudáveis ou ignorem todas as ferramentas defensivas.
- **Adicionar:** testes contra jogadores com e sem armadura, escudo, efeitos e encantamentos.

### 2.3 Telegrafia e recuperação

- **Adicionar:** sinais distintos para preparação, ativação, acerto perfeito, falha e recuperação de habilidade.
- **Alterar:** ações de maior dano ou controle precisam comprometer o usuário com posicionamento ou recuperação previsível.
- **Adicionar:** feedback no defensor quando sua defesa foi rompida e indicação clara de qual atacante possui a oportunidade de finalizar.
- **Remover/evitar:** efeitos tão grandes que escondam o alvo, especialmente em duelos e lutas de equipe.

### 2.4 Mobilidade e perseguição

- **Alterar:** movimentos não devem anular sistematicamente a recuperação de ataques pesados, permitindo spam de aproximação, dano e fuga sem risco.
- **Adicionar:** regras consistentes para o que pode ser cancelado por dash, slide e pulo duplo.
- **Adicionar:** um custo de oportunidade para encadear mobilidade ofensiva e defensiva repetidamente, preferencialmente via recarga compartilhada ou redução de potência, não obrigatoriamente stamina.
- **Manter:** mobilidade responsiva e previsão no cliente com validação do servidor.

### 2.5 Combos, disponibilidade e progressão

- **Alterar:** combos que atualmente só contam golpes podem exigir variedade, posicionamento ou risco, mantendo uma curva de aprendizado razoável.
- **Alterar:** progressão por advancements deve liberar variedade e especialização, sem colocar iniciantes permanentemente em desvantagem competitiva.
- **Adicionar:** configuração de servidor para liberar todos os especiais em arenas/duelos, mantendo desbloqueios no mundo survival.
- **Adicionar:** indicadores de combo legíveis tanto para o atacante quanto para quem está se defendendo quando o golpe final está preparado.
- **Remover/evitar:** bônus de combate longos e muito fortes cuja vantagem persista por várias lutas após um único combo.

## 3. Movimentação

### 3.1 Dash

**Atual:** impulso na direção do olhar, em terra ou no ar.

- **Manter:** dash como ferramenta de reposicionamento universal, independente da arma.
- **Alterar:** impedir que atravesse hitboxes ou obstáculos de maneira desleal por dessincronização; conferir deslocamento e colisão no servidor.
- **Alterar:** limitar o cancelamento gratuito de recovery dos golpes mais fortes.
- **Adicionar:** esquiva perfeita opcional: se o jogador evita de fato um golpe com o dash, ganha vantagem curta de posicionamento ou de fluidez, não um aumento brutal de dano.
- **Adicionar:** ataques contextuais após dash com consequências específicas de cada arma, sem substituir automaticamente o ataque normal.
- **Remover/evitar:** invulnerabilidade durante todo o deslocamento e imunidade generalizada contra ataques de área.

### 3.2 Pulo duplo

- **Manter:** reinício após aterrissar e utilidade de combate vertical.
- **Alterar:** observar interações com critical hits, encantamento Jumping e maça para evitar dano vertical excessivo sem preparação.
- **Adicionar:** janelas distintas para golpes aéreos, em especial espada e maça, com resposta possível para o defensor.
- **Adicionar:** indicação clara quando o segundo pulo está disponível.
- **Remover/evitar:** uso repetitivo de microaterrissagens ou cancelamentos para criar altitude ilimitada.

### 3.3 Deslize

- **Manter:** passagem por espaços baixos e mobilidade no chão.
- **Alterar:** a pequena silhueta não deve causar falhas constantes na detecção de acerto por desync; manter colisão e alcance coerentes.
- **Alterar:** ponderar se deslize pode ser iniciado ou mantido enquanto prepara ataques pesados, bloqueia ou sofre controle.
- **Adicionar:** opção de ataque rasteiro contextual que privilegia desequilíbrio ou reposicionamento, sem acumular alto dano e stun.
- **Remover/evitar:** encadear slide-kick sem brecha defensiva.

### 3.4 Encantamentos de movimento

- **Dashing:** **Alterar** de ganho puramente linear de velocidade para um benefício de mobilidade controlável, evitando deslocamento imprevisível em rede.
- **Jumping:** **Alterar** para que altura extra não multiplique excessivamente acertos de maça ou críticos aéreos.
- **Kick:** **Alterar** o lançamento do alvo para ter contra-jogo e resistência a controle repetido; **adicionar** distinção entre acertar de frente, lateral e por trás se for tecnicamente estável.
- **Remover/evitar:** acumular os três encantamentos para produzir combo incontornável de aproximação, lançamento e finalização.

## 4. Defesas

### 4.1 Parry universal

**Atual:** anula dano com atacante, reflete dano aumentado, trava ambos e aplica stun completo; também alcança ataques não corpo a corpo, como projéteis e explosões.

- **Manter:** timing curto, recompensa elevada pela leitura correta e identidade de defesa de alto risco.
- **Alterar:** separar resultado por tipo de ataque: melee pode permitir riposta, projétil pode ser desviado/anulado, explosão pode ser mitigada em vez de sempre devolvida ao causador distante.
- **Alterar:** substituir, em PvP, a combinação de dano refletido muito alto com stun completo por uma escolha de recompensa principal: contra-ataque, abertura ou deslocamento.
- **Alterar:** reduzir a sensação de punição total do atacante; parry bem-sucedido deve inverter iniciativa, mas não garantir a vitória.
- **Adicionar:** defesa parcialmente bem-sucedida ou parry tardio que não recebe a recompensa completa, se isso não prejudicar clareza.
- **Adicionar:** telemetria de acertos por tipo de origem e proteção contra múltiplas reflexões/loops de dano.
- **Remover/evitar:** reflexão automática de grandes fontes de dano ambiental atribuídas a um atacante e reflexo de reflexos.

### 4.2 Escudo

**Atual:** empurra frontalmente de forma periódica e possui combo de bloqueios com contraefeito.

- **Manter:** papel como defesa contra aproximação e controle de espaço.
- **Alterar:** empurrão periódico não deve criar uma zona frontal impossível de atravessar; preferir empurrão reativo ao bloqueio ou ação defensiva deliberada.
- **Alterar:** diferenciar bloqueio normal de bloqueio perfeito para recompensar timing.
- **Adicionar:** investida defensiva curta após bloqueio bem-sucedido, com recuperação e vulnerabilidade se errar.
- **Adicionar:** resposta clara de armas próprias para romper guarda, como machado e lança em abertura adequada.
- **Remover/evitar:** devolução de dano, lançamento vertical e empurrão repetitivo funcionando simultaneamente a cada bloqueio.

### 4.3 Regras entre defesas

- **Manter:** parry universal e Contra Golpe da lança como técnicas distintas.
- **Adicionar:** regra explícita de precedência em ações simultâneas: parry contra pesado, Contra Golpe contra estocada, escudo contra projéteis etc.
- **Alterar:** exclusão/compatibilidade entre parry, escudo, Contra Golpe, ataque pesado e uso da mão secundária; resultados devem ser consistentes.
- **Adicionar:** atraso ou confirmação visual que torne falha e sucesso compreensíveis sob latência.

## 5. Armas corpo a corpo

### 5.1 Espada simples

**Atual:** estocada via tecla ou clique direito e combo de golpes que concede críticos automáticos temporários.

- **Manter:** espada como arma de ofensiva fluida e posicionamento.
- **Alterar — Estocada:** risco proporcional ao avanço, sem permitir atravessar o alvo ou anular recuperação de outros golpes.
- **Alterar — Combo:** substituir sequência só de acertos por variedade de golpes, por exemplo ataque normal, estocada e golpe após deslocamento, sem ordem fixa obrigatória.
- **Alterar — Especial:** trocar janela prolongada de críticos automáticos por uma oportunidade curta de execução técnica ou crítico condicionado a um golpe bem construído.
- **Adicionar:** opções de finalização: golpe focado em um alvo ou corte de reposicionamento, ambos com contrajogo.
- **Remover/evitar:** críticos garantidos em todos os acertos durante uma fase longa, especialmente com buffs de força.

### 5.2 Duas espadas

**Atual:** ataque secundário causa fração do dano; combo oferece Velocidade II e Força II por duração prolongada.

- **Manter:** estilo agressivo e de múltiplos golpes, com menor capacidade de defesa.
- **Alterar — Combo:** trocar contagem simples por alternância real entre mão principal, secundária e esquiva/posicionamento.
- **Alterar — Especial:** substituir bônus duradouros e amplos de Velocidade + Força por uma janela específica para ataques em sequência, com ganho de fluidez e compromisso ofensivo.
- **Adicionar:** abertura defensiva do usuário quando o segundo golpe erra ou é bloqueado.
- **Adicionar:** finalizador alternativo de pressão ou recuo, sem explosão de dano inevitável.
- **Remover/evitar:** acúmulo de múltiplas fontes de dano e velocidade que faça a configuração superar espada simples em todas as situações.

### 5.3 Machado

**Atual:** ataque pesado carregado, pode atravessar alvos e derrubar bloqueio; especial fortalece próximo pesado e atordoa.

- **Manter:** identidade de golpe lento, ameaça alta e quebra de guarda.
- **Alterar — Pesado:** preparação legível, compromisso com direção/alcance e vulnerabilidade ao errar.
- **Alterar — Alvos múltiplos:** dano e controle secundários devem ser menores que o acerto principal; evitar limpeza excessiva de jogadores agrupados.
- **Alterar — Combo:** considerar sequência baseada em carga bem executada e punição de defesa, não apenas repetição de pesados.
- **Alterar — Especial:** substituir combinação de dano multiplicado com stun completo por escolha entre dano maior, quebra de guarda ou desequilíbrio breve.
- **Adicionar:** interação com parry e dash: golpe muito carregado pode ser punido por bom posicionamento.
- **Remover/evitar:** carregar sem risco e cancelar instantaneamente em mobilidade total.

### 5.4 Lança

**Atual:** mantém jab, charge, Lunge e alcance vanilla; Contra Golpe perfeito anula melee frontal e aplica duas estocadas automáticas. Pressão gera Defesa Rompida, que pode ser consumida por Reação Total ou Colapso Circular. Encantamento Romper reforça a riposta.

- **Manter:** arma técnica de espaçamento, leitura e contra-ataque; preservar jab/charge/Lunge vanilla.
- **Alterar — Contra Golpe:** sucesso perfeito deve dar iniciativa, mas revisar o dano garantido das duas estocadas e o travamento de ambos para impedir combos sem resposta.
- **Alterar — Janela imperfeita:** bloqueio parcial frontal não deve ser tão seguro que compense ativar a técnica sem leitura; erro precisa criar risco.
- **Alterar — Pressão:** impedir farm excessivo de Pressão por bloqueios repetidos e ataques sem exposição; exigir contato válido e participação ativa.
- **Alterar — Defesa Rompida:** deixar estado visual inequívoco, impedir múltiplos finalizadores e limitar efeito de controles subsequentes.
- **Alterar — Reação Total:** priorizar precisão e punição de abertura; permitir alguma resposta anterior à execução, e não tratar o finalizador como kill garantida.
- **Alterar — Colapso Circular:** diminuir benefício contra jogadores secundários, mantendo função de criar espaço; não permitir dano em área excessivo em equipes.
- **Alterar — Romper:** enfatizar geração de oportunidades e identidade do contra-ataque em vez de multiplicadores acumulados de dano.
- **Adicionar:** opção de reposicionamento após Contra Golpe perfeito, escolhida em lugar de prolongar o dano automático.
- **Remover/evitar:** cadeia de Contra Golpe → Defesa Rompida → finalizador sem possibilidade de oposição desde o primeiro evento.

### 5.5 Maça acorrentada

**Atual:** lançamento com alcance, dano direto reduzido, onda de empurrão que afeta também o usuário, retorno automático e combo especial de puxão/lentidão.

- **Manter:** identidade de controle de espaço, verticalidade e risco ao lançar a arma.
- **Alterar — Corrente:** definir respostas quando o alvo se move, a maça encontra obstáculos ou o jogador troca de slot; impedir acerto múltiplo não intencional no retorno.
- **Alterar — Onda:** reduzir encadeamento de autoimpulso, queda e dano em área que deixe pouca oportunidade de reação em PvP.
- **Alterar — Especial:** puxão em área e lentidão não devem resultar em controle múltiplo absoluto; privilegiar agrupamento ou dano, não ambos em potência máxima.
- **Adicionar:** uso situacional da maça como ferramenta de interrupção de posicionamento, com fraqueza clara contra alvos móveis.
- **Remover/evitar:** mecanismos de altura que anulem risco de queda enquanto preservam toda a recompensa ofensiva.

## 6. Armas à distância e tridente

### 6.1 Arco

**Atual:** mão secundária e bundle aceleram carga; combo concede disparos com força máxima sem tempo de puxada; Flecha Dupla gera projétil adicional.

- **Manter:** seleção de flechas pelo bundle e especialização em munições.
- **Alterar — Carga:** evitar que acelerar por mão secundária elimine completamente a decisão entre mobilidade e potência.
- **Alterar — Especial:** remover ou limitar a capacidade de manter spam de disparos com força máxima; preferir precisão ou recarga reduzida condicionada a acertos bem executados.
- **Alterar — Flecha Dupla:** conferir interação com invulnerabilidade entre golpes, efeitos de flechas e dano duplicado no mesmo alvo.
- **Adicionar:** combo de precisão que valorize distância, movimento e variedade de alvos, não apenas quantidade de acertos.
- **Remover/evitar:** combinação de disparo instantâneo, projétil duplicado e efeitos fortes sem janela de resposta.

### 6.2 Besta

**Atual:** Escopeta carrega várias munições e dispara em leque; Grappling Hook puxa usuário até impacto; são incompatíveis entre si.

- **Manter:** separação entre besta de dano próximo e besta de mobilidade.
- **Alterar — Escopeta:** avaliar dano de múltiplos projéteis sobre mesmo jogador e impedir explosão desproporcional a curtíssima distância.
- **Alterar — Munições:** revisar fogos de artifício e flechas com efeitos quando usados juntos, para impedir amplificações pouco visíveis.
- **Alterar — Grappling Hook:** limitar abuso para fugir continuamente, penetrar obstáculos ou cancelar a recuperação de outras armas.
- **Adicionar:** sinais visuais de carregamento especial para o oponente distinguir disparo comum de Escopeta.
- **Adicionar:** contrajogo contra grappling, como interrupção por impacto ou perda de conexão em obstáculo sólido.
- **Remover/evitar:** dano simultâneo de todo leque sem redução ou regras claras de múltiplos acertos no mesmo alvo.

### 6.3 Tridente

**Atual:** Lealdade puxa o alvo e aplica controle; especial de puxadas aplica Lentidão III prolongada; Drenar altera água.

- **Manter:** identidade de captura, deslocamento e combate aquático.
- **Alterar — Lealdade:** puxão de jogadores precisa respeitar colisões, linha de visão e resistência a controle; não deve funcionar como imobilização total repetível.
- **Alterar — Combo:** considerar objetivos mais interessantes que repetir puxadas; valorizar posicionamento e recuperação do tridente.
- **Alterar — Especial:** substituir lentidão extrema e duradoura por vantagem tática breve, como exposição ou deslocamento controlado.
- **Adicionar:** janela para jogador atingido reagir antes de uma execução corpo a corpo subsequente.
- **Remover/evitar:** puxão + proibição de pular/andar + lentidão prolongada funcionando como aprisionamento sem resposta.
- **Manter — Drenar:** utilidade ambiental, mas validar impactos indevidos em arenas e água usada como mecanismo defensivo.

## 7. Encantamentos de armas e defesa

| Encantamento | Direção recomendada para PvP |
| --- | --- |
| **Alcance** (espada) | **Alterar:** ganho de alcance previsível, com limites de sinergia com estocada e validação de hitbox. Evitar hit sem oportunidade de contra-ataque. |
| **Golpe Duplo** (espada) | **Alterar:** dar identidade à alternância de mãos; impedir que velocidade e dano secundário escalem desproporcionalmente juntos. |
| **Corte** (machado) | **Alterar:** benefício ligado a carregamento bem-sucedido; não somar dano máximo a especial e stun sem compensação. |
| **Kick** (calça) | **Alterar:** efeito de interrupção/empurrão controlável, sem loops de lançamento. |
| **Dashing** (calça) | **Alterar:** privilegiar controle, não distância/velocidade sem limites. |
| **Jumping** (bota) | **Alterar:** verificar sinergia com maça e críticos aéreos. |
| **Empurrão** (escudo) | **Alterar:** empurrão ligado a bloqueio válido ou ação ativa, não domínio frontal contínuo. |
| **Chain** (maça) | **Alterar:** alcance maior exige sinalização clara e pode ter compromisso de retorno/recuperação. |
| **Flecha Dupla** (arco) | **Alterar:** limitar dano cumulativo e controlar efeitos de duas flechas no mesmo alvo. |
| **Grappling Hook** (besta) | **Alterar:** mobilidade com colisão/contrajogo, sem atravessar geometria. |
| **Escopeta** (besta) | **Alterar:** spread e dano de múltiplos acertos para que cada nível ofereça escolha sem burst incontornável. |
| **Drenar** (tridente) | **Manter:** característica ambiental; **adicionar:** opção de desligar destruição de água em servidores PvP. |
| **Romper** (lança) | **Alterar:** reforçar janela de Defesa Rompida e técnica, sem empilhar ganho excessivo de pressão e dano final. |

**Diretriz adicional:** novos encantamentos devem prioritariamente **mudar a decisão de combate** (alcance, padrão de golpe, reposicionamento, leitura de defesa), e não apenas aumentar todas as estatísticas. Considerar incompatibilidades quando duas mudanças anularem fraquezas necessárias de uma arma.

## 8. Revisão individual dos especiais de combo

| Arma | Especial documentado | Mudança proposta |
| --- | --- | --- |
| **Espada** | Sequência de golpes → críticos automáticos por período | **Alterar:** transformar em golpe de execução ou janela técnica condicionada à variação de ações; **remover/evitar** críticos automáticos em todo acerto. |
| **Duas espadas** | Sequência → Velocidade II e Força II prolongadas | **Alterar:** trocar buffs persistentes por ofensiva curta e interativa; recompensa por alternância entre mãos. |
| **Machado** | Pesados → próximo pesado com dano elevado e stun | **Alterar:** dar escolha entre quebra de guarda ou dano; reduzir acumulação simultânea de burst e imobilização. |
| **Arco** | Acertos → tiros instantâneos com força máxima | **Alterar:** janela de precisão/mobilidade em vez de spam constante no máximo. |
| **Tridente** | Puxadas → lentidão extrema no alvo final | **Alterar:** reduzir controle prolongado, acrescentar oportunidade de reação após o puxão. |
| **Escudo** | Bloqueios → reflexão e lançamento do atacante | **Alterar:** exigir bloqueio deliberado/perfeito; evitar soma de reflexão, empurrão automático e lançamento. |
| **Maça** | Acertos → puxão e lentidão em golpe seguinte | **Alterar:** escolha de controle ou dano espacial com menor capacidade de bloquear múltiplos jogadores. |
| **Lança** | Pressão → Reação Total ou Colapso Circular | **Manter:** duas finalizações exclusivas; **alterar:** abertura consumida uma vez, comunicação clara, dano secundário controlado, resposta ao início do combo. |

### Proposta de estrutura compartilhada de combo

- **Adicionar:** fase de *preparação* (ação que constrói o combo), fase de *abertura* (vantagem percebível) e *finalização* (efeito único que consome a oportunidade).
- **Adicionar:** alternativas por arma entre finalizador agressivo e finalizador de controle/reposicionamento.
- **Alterar:** receber dano pode interromper o progresso, mas não deve punir indiscriminadamente armas cujo estilo pede absorver ou bloquear ataques.
- **Adicionar:** diferença explícita entre combo cancelado, expirado, bloqueado e consumido.
- **Remover/evitar:** contadores invisíveis ou combos que carregam indefinidamente entre vários confrontos sem custo.

## 9. Finalizadores: regras propostas

### Reação Total (lança)

- **Manter:** finalização concentrada de alvo único, dependente de Defesa Rompida.
- **Alterar:** preservar impacto visual sem depender somente de um multiplicador alto; avaliar bônus por timing e posicionamento.
- **Adicionar:** sinal claro de execução iminente para ambos os jogadores.
- **Remover/evitar:** morte automática, múltiplas ativações no mesmo estado ou combinação inevitável com duas estocadas automáticas.

### Colapso Circular (lança)

- **Manter:** alternativa de controle em área, escolhida no lugar de Reação Total.
- **Alterar:** alvo principal recebe consequência significativa; jogadores secundários recebem controle e dano substancialmente menores.
- **Adicionar:** reação visual de deslocamento/onda, sem partículas que impeçam leitura de combate.
- **Remover/evitar:** vantagem simultânea de dano em alvo único equivalente à Reação Total e supremacia contra grupos.

### Finalizadores futuros (não existentes na 1.2.1)

- **Espada — Corte de Precisão:** recompensa movimento e alternância entre golpes; acerto rápido focado ou reposicionamento, não ambos.
- **Duas espadas — Investida Cruzada:** sequência de duas mãos com risco de exposição ao errar.
- **Machado — Ruína de Guarda:** remove temporariamente a capacidade defensiva específica do escudo, em vez de stun completo.
- **Maça — Impacto de Retorno:** onda localizada após recuperar a maça, exigindo leitura de espaço.
- **Arco — Disparo de Oportunidade:** projétil de precisão após sequência variada, em lugar de spam instantâneo.
- **Escudo — Resposta Defensiva:** empurrão direcionado após bloqueio preciso, abrindo oportunidade de fuga ou contra-ataque.
- **Besta — Disparo Calculado:** escolha entre dano concentrado ou dispersão, comunicada antes de atirar.
- **Tridente — Reposicionamento:** puxa ou desloca com controle reduzido, abrindo duelo de curta distância.

**Importante:** não implementar todos os novos finalizadores ao mesmo tempo; avaliar os reworks das habilidades atuais antes de ampliar o conjunto.

## 10. Interações perigosas a testar

| Combinação | Risco de PvP | Direção de correção |
| --- | --- | --- |
| Parry + dano refletido + stun | Vantagem total por único evento | Escolher recompensa principal e fornecer recuperação. |
| Machado pesado + especial + Corte | Burst e controle no mesmo golpe | Distinguir quebra de guarda de dano máximo. |
| Lança + Romper + riposta + Reação Total | Dano garantido em cascata | Revisar ganho de Pressão por reação automática; uma abertura, um finalizador. |
| Colapso Circular + vários inimigos | Dano secundário excessivo | Regras separadas para alvo marcado e alvos próximos. |
| Duas espadas + buffs + Golpe Duplo | Dano por segundo muito elevado | Buff curto e condicionado à execução. |
| Arco instantâneo + Flecha Dupla | Pressão à distância sem intervalo | Restringir spam e cumulatividade de projéteis. |
| Escopeta + fogos de artifício | Explosão de dano à queima-roupa | Tratar múltiplos impactos no mesmo jogador. |
| Tridente Lealdade + especial | Controle duradouro e sem resposta | Resistência a controle e saída possível. |
| Escudo Empurrão + especial | Recuo perpétuo | Empurrão reativo, recuperação e limite de cadeia. |
| Kick + slide + dash | Movimento/controle repetitivo | Não permitir reinício permanente do desequilíbrio. |
| Jumping + maça + autoimpulso | Dano vertical desproporcional | Respeitar risco, altura válida e regras de dano. |
| Grappling Hook + ataques pesados | Cancelamento de recovery | Regras claras para preparação/cancelamento. |

## 11. Regras de jogo e experiência competitiva

### 11.1 Separação PvP e PvE

- **Adicionar:** configuração para perfis PvP e PvE, principalmente em dano de especiais, duração de controle, puxões e empurrões.
- **Manter:** habilidades visualmente consistentes entre modos; quando variar o efeito, explicitar essa diferença ao jogador.
- **Adicionar:** opção de servidor para desabilitar habilidades específicas sem quebrar as restantes.

### 11.2 Morte, entrada em combate e trocas de item

- **Adicionar:** definição de quais combos e buffs expiram após morte, logout, troca de mundo, desconexão e entrada em arena.
- **Adicionar:** regras ao trocar de arma no meio de uma habilidade: não duplicar carga, munição, retorno de maça ou janela de finalização.
- **Alterar:** recuperação de ataques em andamento não deve ser ignorada ao trocar rapidamente de item.
- **Adicionar:** proteção contra progresso de combo com NPCs/entidades aliados quando isso gerar vantagem indevida contra jogadores.

### 11.3 Informação e acessibilidade

- **Adicionar:** HUD para estados que alteram a defesa, além de recargas de habilidades.
- **Adicionar:** efeitos sonoros discretos específicos para golpe bloqueado, parry perfeito, Contra Golpe, Defesa Rompida e finalizador preparado.
- **Adicionar:** diferenciação visual por formas/animação, não somente cores.
- **Alterar:** padronizar indicadores e evitar sobreposição excessiva de ícones na hotbar.

### 11.4 Integridade e rede

- **Manter:** servidor como autoridade para dano, projéteis, controles e especiais; previsão do movimento no cliente.
- **Adicionar:** validações para spam de pacotes, execução fora de janela, hit após morte, alcance impossível, reaproveitamento de estado e avanço simultâneo de combos.
- **Adicionar:** testes de latência e combate com jogadores conectados de redes diferentes.
- **Adicionar:** logs de depuração de habilidades e motivos de bloqueio, desativados em produção.

## 12. Prioridades de implementação

### P0 — Corrigir primeiro (alto impacto no equilíbrio)

1. Parry: reflexão, atordoamento e tratamento por tipo de ataque.
2. Dano explosivo dos especiais: duas espadas, machado, lança e arco.
3. Correntes de controle: tridente, escudo, Kick e maça.
4. Consumo único de Defesa Rompida e colisões entre finalizadores/ataques simultâneos.
5. Escopeta e Flecha Dupla: efeitos de múltiplos projéteis no mesmo alvo.

### P1 — Melhorar jogabilidade

1. Recuperação e cancelamento de golpes via movimento.
2. Combos de armas com mais variedade e menos dependência de contagem de acertos.
3. Indicadores de preparo, erro, defesa perfeita e exposição.
4. Perfis PvP/PvE e configurações do servidor.
5. Interação justa entre escudo, lança, parry e ataques pesados.

### P2 — Expandir depois dos testes

1. Ataques contextuais após dash, slide e pulo duplo.
2. Finalizadores alternativos por arma.
3. Encantamentos de transformação de estilo de luta.
4. Combos cooperativos com limites próprios para equipes.

## 13. Testes sugeridos antes de aceitar cada mudança

- **Duelo aberto:** espada vs espada; espada vs lança; lança vs machado; escudo vs machado.
- **Contra alcance:** arco vs dash; besta Escopeta vs escudo; tridente vs arma corpo a corpo.
- **Duelo vertical:** maça e Jumping contra alvos no solo, em degraus e em arenas fechadas.
- **Combate em grupo:** controle de área, múltiplos parries, Colapso Circular e projéteis simultâneos.
- **Controle de personagem:** medir se o jogador conserva opções reais após stun, puxão, empurrão ou quebra de guarda.
- **Rede:** latência baixa/alta e troca rápida de itens durante golpes e finalizadores.
- **Equipamento:** com e sem armadura, escudos, encantamentos, buffs e vários níveis de progressão.
- **Experiência do jogador:** identificar se a derrota ocorreu por erro reconhecível ou por combo inevitável.

### Critérios de aprovação (qualitativos)

Uma mudança é aprovada quando o especial **continua valioso**, possui **contra-jogo inteligível**, não cria **controle infinito**, não domina **todos os confrontos** e não depende de bugs de previsão/rede. Os números definitivos ficam para uma etapa posterior com testes de dano, taxas de acerto e resultados de duelos.

## 14. O que eu NÃO removeria nesta etapa

- Dash, pulo duplo e deslize: são a identidade de mobilidade do mod.
- Parry e Contra Golpe: manter ambos, mas com recompensas e funções distintas.
- Mecânica de Pressão e escolha entre Reação Total e Colapso Circular: é a parte mais estratégica do design da lança.
- Maça acorrentada, seleção de flechas em bundle e especializações da besta: acrescentam variedade real sem requerer novas armas.
- Sistema de advancements como progressão de survival: oferecer somente alternativa competitiva para arenas.

## 15. Resultado esperado

O Modified PvP deixaria de depender tanto de **buffs longos, multiplicadores, atordoamento completo e contagem simples de golpes** e passaria a recompensar **movimentação contextual, leitura do adversário, preparação de aberturas, escolha de finalizadores e recuperação após erros**.

**Decisão recomendada:** começar por um patch de balanceamento mantendo as armas e controles existentes. Somente depois avançar para reformulação de combos e novas finalizações. Nenhum valor numérico deve ser estabelecido antes dessa validação.

---

*Documento baseado no README 1.2.1 fornecido. Sem acesso ao código-fonte, todas as avaliações de risco são hipóteses de design a validar, e não bugs comprovados da implementação.*
