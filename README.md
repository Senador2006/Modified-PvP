# Modified PvP

Movimentação e armas extras para PvP no Minecraft **1.21.11**, feito para Fabric.

O cliente prevê o movimento. O dano, o empurrão, os projéteis e os especiais de combo só valem no servidor.

[![Versão](https://img.shields.io/badge/vers%C3%A3o-1.3.1-4c8bf5?style=flat-square)](releases/modifiedpvp-1.3.1.jar)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-62b47a?style=flat-square)](https://fabricmc.net/develop/)
[![Fabric Loader](https://img.shields.io/badge/Fabric_Loader-0.19.5+-dbd0b4?style=flat-square)](https://fabricmc.net/)
[![Fabric API](https://img.shields.io/badge/Fabric_API-0.141.6-c8a2c8?style=flat-square)](https://modrinth.com/mod/fabric-api)
[![Java](https://img.shields.io/badge/Java-21-f89820?style=flat-square)](https://adoptium.net/)
[![License](https://img.shields.io/badge/license-CC0-lightgrey?style=flat-square)](LICENSE)

## Download

**[modifiedpvp-1.3.1.jar](releases/modifiedpvp-1.3.1.jar)**

## Instalação

1. Instale o [Fabric Loader](https://fabricmc.net/use/installer/) para o Minecraft 1.21.11.
2. Baixe a [Fabric API](https://modrinth.com/mod/fabric-api) para a 1.21.11.
3. Coloque a Fabric API e o `modifiedpvp-1.3.1.jar` na pasta `mods` do Minecraft.
4. Em servidor, o mod precisa estar no servidor **e** em todos os clientes.

## Requisitos

| | |
| --- | --- |
| Minecraft | 1.21.11 |
| Fabric Loader | 0.19.5 ou mais novo |
| Fabric API | 0.141.6+1.21.11 |
| Java | 21 |

## Controles

As teclas ficam na categoria **Modified PvP** e podem ser trocadas em Controles.

| Ação | Padrão | Como funciona |
| --- | --- | --- |
| Dash | `R` | Impulso na direção do olhar, no chão ou no ar. Recarga curta. |
| Pulo duplo | `Espaço` no ar | Solte e aperte o pulo de novo depois de sair do chão. Recarrega ao aterrissar. |
| Deslize | `C` enquanto corre | Abaixa o corpo, acelera e passa em vãos de 1 bloco. |
| Parry | `X` | Abre uma janela curta de defesa. Melee perfeito inverte a iniciativa. Projétil é anulado. Explosão é mitigada. |
| Estocada | `V` ou clique direito com uma espada | Avança e atinge o primeiro alvo à frente, mesmo colado em você. Gasta a carga do ataque. Com duas espadas, o direito vira o golpe conjunto; a estocada fica na tecla `V`. |
| Ataque pesado | Segure o clique direito com machado | Carrega olhando para qualquer lugar, mesmo com escudo na outra mão. Solte para golpear. A barra aparece na hotbar de ação. |
| Maça acorrentada | Clique direito com a maça | Lança a maça presa a uma corrente. Ela volta sozinha para a mão. |
| Trocar flecha | `G` com o arco na mão | Passa para a próxima flecha do bundle. |
| Contra Golpe | `Z` com a lança na mão principal | Postura frontal curta. Na janela perfeita anula o melee e solta na hora duas estocadas no atacante. A mesma tecla dispara Reação Total se o alvo estiver com Defesa Rompida. A charge natural na mesma abertura dispara Investida Implacável. |
| Guarda do machado | `Alt esquerdo` com machado e escudo | Levanta o escudo da mão secundária. Se Fortaleza Avançada estiver pronta, o mesmo aperto faz o avanço curto. O clique direito continua carregando o ataque pesado. |

Comida ou lança na mão secundária continuam usando o clique direito. Com espada na principal, tridente ou besta na secundária também usam o direito; a estocada fica na tecla `V`. O escudo com espada usa o direito e a estocada fica no `V`. Com machado e escudo, o direito carrega o ataque pesado e a guarda nova fica no `Alt esquerdo`.

À esquerda da hotbar, os quatro ícones de parry, dash, deslize e pulo duplo continuam no mesmo lugar. O Contra Golpe aparece ao lado esquerdo do parry, sem deslocá-los. A barra híbrida fica acima da barra adaptativa.

## Combate

### Movimento

Dash, pulo duplo e deslize funcionam sem arma na mão. Não saem dentro da água, na lava, montado ou planando. Dash e deslize não cancelam um ataque pesado em carga ou em recuperação. O deslize não dá imunidade. O ícone do pulo duplo aparece no ar enquanto o segundo pulo está disponível.

### Parry

Aperte `X` logo antes de levar o golpe. A janela dura 0,4 s. A primeira metade é perfeita; a segunda é tardia. O resultado depende do tipo de ataque:

- **Melee perfeito:** anula o golpe e trava os dois por 0,5 s. Em PvP o atacante fica **Interrompido** (não usa habilidades) e é empurrado — não leva o dano refletido nem o atordoamento completo. Contra mobs, o reflexo com o dobro do dano e o stun de 3 s continuam.
- **Parry tardio:** o dano é só reduzido. Sem reflexo e sem interrupção.
- **Projétil:** o projétil é anulado. Nada volta para o atirador.
- **Explosão:** o dano é mitigado. Nada é devolvido ao causador distante.

Queda, lava e dano sem atacante não são aparados. Depois de um controle, o jogador ganha resistência curta a outro stun, puxão ou chute.

Se aparar algo, a recarga é de 60 s. Se a janela passar sem nada, a recarga é de 30 s.

O Contra Golpe da lança é outra habilidade. Não devolve dano e não aplica o stun do parry.

### Lança

A jab, a charge, o Lunge e o alcance mínimo vanilla continuam iguais. O mod só lê esses golpes no servidor para Pressão e Contra Golpe.

**Contra Golpe** (`Z`) exige lança na mão principal e os mesmos bloqueios de movimento (água, lava, montaria, elytra, stun). A postura dura 0,60 s. Os primeiros 0,20 s são a janela perfeita. Só melee frontal com atacante (cone de 120°) e linha de visão. Queda, lava e golpes pelas costas passam. Projéteis ainda não entram.

Na janela perfeita, aquele golpe é anulado, os dois param por 0,5 s e sai uma estocada automática no atacante em PvP (duas em PvE). A riposta automática não gera Pressão em jogadores. Fora da janela perfeita, o melee frontal perde 60% do dano e a postura acaba. Recarga de 6 s depois da postura. Não ativa junto do parry.

**Pressão** vai de 0 a 100 por par atacante + alvo, só no servidor. Jab +8, jab na faixa longa +12 (não soma os dois), charge +18, acerto no escudo +20, riposta perfeita +35. Depois de 3 s sem acréscimo, cai 5 pontos/s. Em 100, zera e aplica **Defesa Rompida**: 0,80 s no PvP (imunidade 8 s), 1,50 s em mobs comuns (imunidade 4 s). Chefes só ficam vulneráveis por pouco tempo, sem trava total. A barra aparece ao mirar o alvo recente.

**Reação Total** é o finalizador perfurante na tecla `Z`, só com o alvo marcado e a técnica desbloqueada. Dano **3×** do base (3,5× se Romper III causou a ruptura; 2× em chefes). No PvP o teto é 18 (22 no aprimorado) ou 55% da vida máxima.

**Investida Implacável** é o finalizador cinético da mesma abertura: uma Charge vanilla válida contra o alvo marcado empurra com mais direção. Não multiplica dano pela velocidade e não transforma jab comum em investida.

**Colapso Circular** é o outro finalizador da mesma abertura: uma estocada (jab) no alvo com Defesa Rompida vira um giro em área. O alvo marcado leva 2× (2,4× se a ruptura veio de Romper III). Mobs perto tomam uma fração e são empurrados; jogadores secundários levam pouco dano. A estocada automática do Contra Golpe perfeito **não** dispara o giro.

Os três finalizadores compartilham a abertura e a recarga de 5 s. Quem usar um consome a Defesa Rompida. Sem insta-kill. O perfil recente da Pressão só sugere o estilo; o jogador ainda escolhe o gatilho.

### Espada

Uma espada na mão principal estoca com `V` ou clique direito. Com outra na mão secundária, o esquerdo ataca só com a principal e o direito faz o golpe conjunto. A segunda mão causa uma fração do dano.

### Machado

Segure o clique direito para carregar o golpe pesado, mesmo com um escudo na outra mão. Nesse caso o escudo não sobe. Quanto mais a barra enche, mais forte o acerto. Ele atravessa até três alvos e pode derrubar bloqueio.

### Maça

O clique direito lança a maça presa a uma corrente, com alcance de 10 blocos. Ao acertar um bloco ou uma criatura, ela solta uma mini onda de impacto que empurra para longe mobs e players por perto, **inclusive você**. Acertar o chão perto dos pés te lança para cima, e o dano de queda conta a partir da altura do impacto, como na carga de vento.

Quem leva o acerto direto recebe metade do dano da maça. Cada acerto gasta 1 de durabilidade.

Depois do impacto, ou ao chegar no fim da corrente, a maça volta para o mesmo slot da hotbar e fica 1,5 s em recarga. Se você morrer com ela no ar, ela cai onde estava.

### Arco

Flechas na mão secundária carregam o arco mais rápido, e a animação acompanha. Um bundle de flechas na mão secundária também acelera a carga.

Flechas diferentes (normal, espectral, com efeito) podem ficar no mesmo bundle. Com o arco na mão, `G` escolhe qual será disparada. Flechas soltas na mão secundária têm prioridade sobre o bundle.

### Tridente

**Lealdade** só faz o tridente voltar, como no vanilla. O puxão e o atordoamento antigos saíram dela.

**Pull** I–III puxa o alvo por poucos ticks, com força limitada e sem stun. Exige Lealdade no mesmo tridente. Sem linha de visão, o puxão não começa e para no meio. O mesmo alvo não pode ser puxado de novo na hora. Não combina na prática com Impulso, porque Lealdade e Impulso continuam exclusivos no vanilla. Não adiciona raio.

### Escudo

Com o escudo erguido, um bloqueio válido empurra o atacante. Não há mais empurrão automático contínuo na frente.

### Besta

Uma carga da **Escopeta** coloca várias munições de uma vez e dispara todas em leque. Cada uma é gasta. Vale flecha, fogo de artifício, ou os dois no mesmo carregamento. Em PvP, pelotas extras no mesmo jogador causam cada vez menos dano.

O **Grappling Hook** puxa você até o ponto em que a flecha ou o fogo de artifício acerta. Obstáculo sólido ou levar dano corta a conexão.

## Especiais

Cada arma tem um desbloqueio na aba **Especiais**. Depois do advancement, o servidor lê **como** você usa a arma e abre **uma** oportunidade contextual. O próximo comando manual correspondente executa o finalizador. Nada dispara sozinho.

O golpe precisa acertar um mob ou um jogador. Pressionar botão no vazio não conta. Levar dano zera os estilos da espada, das duas espadas, do arco, da besta e do escudo. Machado, maça e tridente não caem por dano. A recarga é por família e não some ao trocar de slot, item idêntico, logout ou dimensão.

Oportunidades e buffs temporários zeram na morte, no logout e ao trocar de mundo. O progresso de desbloqueio permanece. Quem já tinha o advancement da 1.2.2 usa as variantes novas sem farm extra.

| Arma | Desbloqueio | Estilos | Finalizadores | Recarga | Interrompe |
| --- | --- | --- | --- | --- | --- |
| Espada | Mate 50 mobs com a espada | Ritmo, estocada, parry | Corte Relâmpago, Perfuração Precisa, Contra-Corte | 60 s | Sim |
| Duas espadas | Mate 50 mobs com as duas na mão | Alternância, pressão, abertura | Corte Cruzado, Rajada Dupla, Corte de Abertura | 120 s | Sim |
| Machado | Acerte 20 ataques pesados | Pesado direto ou bloqueado | Golpe Soberano, Ruína da Guarda | 50 s | Não |
| Arco | Acerte 10 mobs diferentes | Acerto carregado ou ignição de carrinho | Disparo Certeiro, Ignição Perfeita | 30 s | Sim |
| Besta | Mesmo advancement do arco | Foguete em grupo ou no mesmo alvo | Barragem Explosiva, Descarga Concentrada | 40 s | Sim |
| Tridente | Acerte um afogado com um tridente lançado | Retorno, maré, tempestade | Arremesso Soberano, Investida das Marés, Julgamento do Trovão | 60 s | Não |
| Escudo | Bloqueie 30 golpes | Guarda, timing, aliado | Repulsão, Choque de Escudo, Intervenção | 45 s | Sim |
| Maça | Acerte 10 arremessos da corrente | Entidade ou bloco | Impacto de Retorno, Repulsão | 70 s | Não |
| Lança | Acerte 10 Contra Golpes perfeitos em mobs hostis | Perfil da Pressão | Reação Total (`Z`), Investida Implacável (charge) ou Colapso Circular (estocada) | 5 s | Não |

## Combos híbridos

Com as duas famílias desbloqueadas e as duas armas disponíveis, o servidor observa uma sequência e abre **uma** oportunidade híbrida. O evento que completa a preparação não entrega o bônus. O próximo comando manual correspondente executa. Se a híbrida estiver pronta, ela tem prioridade sobre o finalizador individual da mesma ação. As duas não se somam.

| Par | Técnicas ativas |
| --- | --- |
| Lança + maça | Impacto de Investida, Reação Aérea. Golpe Estendido só reconhece a troca; o alcance não muda. |
| Espada + tridente | Caçada Implacável (exige Pull) e Retorno Ofensivo. Duelo Coordenado fica desligado. |
| Maça + tridente | Âncora das Marés (exige Pull) e Queda Guiada. Eco do Trovão fica desligado. |
| Machado + escudo | Retaliação Soberana, Fortaleza Avançada (`Alt esquerdo`) e Ruptura Defensiva. |
| Espada + escudo | Contracorte, Ponta Protegida e Desvio de Guarda. O bloqueio cronometrado é o do escudo, não o parry universal. |
| Espada + besta | Finta Armada e Tiro Repentino. Último Recurso fica desligado. |
| Machado + besta | Abertura da Coroa, Pressão do Rei e Última Palavra. |

**Technoblade Never Dies** (aba Especiais, moldura desafio) abre ao completar as três técnicas da Coroa Carmesim contra alvos hostis válidos. O título fica em inglês em todos os idiomas. As técnicas funcionam antes do advancement.

Tridente + escudo, arco + espada, maça + escudo e lança + besta não foram implementados.

## Encantamentos

| Encantamento | Item | Nível | Efeito |
| --- | --- | --- | --- |
| Alcance | Espada | III | Aumenta o alcance de todos os golpes da espada em 0,75 bloco por nível, e ainda mais o da estocada. |
| Golpe Duplo | Espada | III | Com duas espadas, ataca mais rápido e a mão secundária causa um pouco mais de dano. |
| Corte | Machado | III | Aumenta o dano do ataque pesado. |
| Kick | Calça | III | O deslize chuta para cima e para frente quem estiver no caminho. |
| Dashing | Calça | I | Reduz a recarga do dash. |
| Jumping | Bota | III | Aumenta a altura do pulo duplo. |
| Empurrão | Escudo | III | Aumenta a força do empurrão com o escudo erguido. |
| Chain | Maça | III | Aumenta o alcance da corrente em 4 blocos por nível (até 22 blocos). |
| Flecha Dupla | Arco | I | Dispara duas flechas. Em PvP a segunda causa uma fração do dano. Gasta uma flecha. |
| Grappling Hook | Besta | I | Puxa você até o impacto do tiro. |
| Escopeta | Besta | III | Nível I, II e III carregam 2, 3 e 4 munições. |
| Drenar | Tridente | I | O tridente lançado apaga a água e as colunas de bolhas que atravessa. Não combina com Impulso. |
| Pull | Tridente | III | Puxão curto e limitado. Exige Lealdade. Não atordoa e não combina com Impulso. |
| Romper | Lança | III | I Fissura: a primeira estocada do Contra Golpe perfeito +15 de pressão. II Fratura: a segunda estocada automática +15 de pressão. III Colapso: se essa riposta causar Defesa Rompida, a Reação Total sai aprimorada, com empurrão leve em mobs próximos. Não é o encantamento Ruptura da maça. |

Escopeta e Grappling Hook não combinam entre si, nem com Tiro Múltiplo ou Perfuração.

Lealdade volta a ser só o retorno vanilla. O puxão ficou no Pull.

### Onde conseguir

| Encantamentos | Onde |
| --- | --- |
| Alcance, Golpe Duplo, Corte, Kick, Dashing, Jumping, Empurrão | Mesa de encantamentos e aldeões bibliotecários. Também aparecem em baús com saque encantado aleatório, como os do vanilla. |
| Chain | Só nível I, como livro, nos cofres ominosos da Trial Chamber. Tem a mesma chance do livro de Wind Burst. Os níveis II e III saem juntando livros na bigorna. |
| Flecha Dupla | Baús da biblioteca das Strongholds. Todo baú tem um livro garantido. |
| Grappling Hook, Escopeta | Baú dos Postos Avançados de Saqueadores, 50% de chance de um livro. |
| Drenar | Baús das Ruínas Oceânicas: 35% nas grandes, 15% nas pequenas. |
| Pull | Livro nas Ruínas Oceânicas (10% nas pequenas, 20% nas grandes) e no tesouro de naufrágio (12%). Não entra sozinho em tridentes que já tinham Lealdade. |
| Romper | Livro nível I nos baús raros das Trial Chambers (`chests/trial_chambers/reward_rare`), 15% de chance por baú. II e III saem na bigorna. Não sai na mesa nem com aldeão. |

Os encantamentos de maça, arco, besta, tridente e Romper da lança não saem na mesa nem com aldeões.

Em servidor, o arquivo `config/modifiedpvp.properties` aceita `unlockSpecials=true` para liberar todos os especiais sem advancements (arenas), `drainWater=false` para impedir o Drenar de apagar água e `adaptiveDebug=true` para logar estilo/finalizador. Os pesos e recargas adaptativos também podem ser calibrados nesse arquivo (`adaptive.window.sword`, `adaptive.evidence.axe`, etc.).

Chaves novas da 1.3.1, com valores provisórios: `hybridCombosEnabled=true`, `hybridRequireAdvancements=true`, `hybridOptionalStyles=false`, `hybridAllowFinisherOnMiss=false`, `hybridEvidenceWindowTicks=80`, `hybridOpportunityWindowTicks=50`, `hybridPrimeWindowTicks=30`, `hybridSharedCooldownTicks=200`, `hybridSwapWindowTicks=5`, `hybridRecoveryTicks=4`, `hybridRecoverySpeed=0.20`, `hybridRecoveryCap=0.40`, `hybridMaxKnockbackBonus=0.30`, `hybridChargeFavorTicks=4`, `hybridGuardTicks=8`, `hybridAdvanceSpeed=0.40`, `spearMaceExtendedReachEnabled=false`, `pullEnabled=true`, `pullTargetResistanceTicks=40`, `pullStrengthLevel1=0.16`, `pullStrengthLevel2=0.22`, `pullStrengthLevel3=0.28`, `pullTicksLevel1=5`, `pullTicksLevel2=7`, `pullTicksLevel3=9`. `pullRequireLoyalty=false` é ignorado: Pull sem Lealdade não ativa. O histórico do motor guarda no máximo 16 fatos. Um servidor que já tem o arquivo mantém as chaves antigas; as novas usam esses padrões até serem escritas.

### Afogados

Afogados que nascem com tridente deixam cair o tridente em 70% das mortes (no vanilla são 8,5%).

## Compilando

```powershell
.\gradlew.bat build
```

O jar sai em `build/libs/modifiedpvp-<versão>.jar`.

## Novidades

### 1.3.1

- Combos híbridos entre lança e maça, espada e tridente, maça e tridente, machado e escudo, espada e escudo, espada e besta, machado e besta. Uma ação executa um finalizador: híbrido pronto primeiro, senão o individual.
- Encantamento Pull I–III. Lealdade volta a só retornar o tridente. Tridentes antigos com Lealdade não ganham Pull.
- O desbloqueio do tridente passa a ser acertar um afogado com o tridente lançado.
- Guarda do machado no `Alt esquerdo`. A barra híbrida fica acima da barra adaptativa; os ícones de movimento não mudam de lugar.
- Advancement **Technoblade Never Dies** ao completar Abertura da Coroa, Pressão do Rei e Última Palavra contra alvos hostis.
- Duelo Coordenado, Eco do Trovão, Último Recurso e os pares adiados ficam de fora. O alcance do Golpe Estendido não muda.
- Checklist: [docs/Modified_PvP_1.3.1_Testes.md](docs/Modified_PvP_1.3.1_Testes.md).

### 1.3.0

- Especiais passam a ler o **estilo de uso** da arma, não só a contagem linear de golpes. Uma oportunidade contextual aparece por tempo limitado e some depois da tentativa válida.
- Espada: Corte Relâmpago, Perfuração Precisa e Contra-Corte. Duas espadas (Opção A): esquerdo = principal, direito = golpe conjunto; Corte Cruzado, Rajada Dupla e Corte de Abertura.
- Machado: Golpe Soberano e Ruína da Guarda. Maça acorrentada: Impacto de Retorno e Repulsão. Smash e Wind Burst vanilla iguais.
- Tridente útil em qualquer clima: Arremesso Soberano, Investida das Marés e Julgamento do Trovão. Relâmpago só com Condutividade vanilla em tempestade.
- Arco: Disparo Certeiro e Ignição Perfeita. Sem bônus balístico em TNT minecart e sem explosão extra. Besta: Barragem Explosiva e Descarga Concentrada nos foguetes.
- Lança: Investida Implacável na Charge durante a mesma Defesa Rompida. Escudo: Repulsão, Choque de Escudo e Intervenção, sem empilhar com parry.
- HUD discreto de família/estilo/recarga. Advancements antigos continuam válidos. Checklist: [docs/Modified_PvP_1.3.0_Testes.md](docs/Modified_PvP_1.3.0_Testes.md).

### 1.2.2

- Parry separado por tipo de ataque: melee inverte iniciativa em PvP, projétil é anulado, explosão é mitigada. Parry tardio só reduz dano.
- Stun completo, reflexo 2× e empurrão contínuo do escudo deixam de decidir o duelo sozinhos.
- Especiais de espada, duas espadas, machado, arco, tridente e escudo passam a ser janelas curtas, não buffs longos.
- Resistência curta a controles consecutivos. Estados **Interrompido**, **Desequilibrado** e **Guarda Exposta**.
- Escopeta e Flecha Dupla reduzem dano extra no mesmo jogador. Grappling corta em obstáculo ou ao levar dano.
- Dash não cancela o compromisso do ataque pesado. Dashing reduz recarga em vez de aumentar velocidade.
- Configuração de servidor em `config/modifiedpvp.properties`: `unlockSpecials` e `drainWater`.

### 1.2.1

- Ícones de dash, deslize, pulo duplo, parry e Contra Golpe atualizados.
- Parry e Contra Golpe perfeito travam defensor e atacante por 0,5 s antes do golpe refletido ou das estocadas.
- O parry devolve o dobro do dano que sobra depois da armadura e dos encantamentos.
- Reação Total e Colapso Circular passam a acertar.
- O movimento previsto pelo cliente deixa de ser aplicado de novo no servidor.

### 1.2.0

- Sistema da lança: **Contra Golpe** (`Z`), Pressão, Defesa Rompida, encantamento **Romper** I–III e finalizador **Reação Total**.
- Contra Golpe perfeito anula o melee e solta duas estocadas automáticas. Reação Total causa 3×. Colapso Circular é o giro em área na estocada com Defesa Rompida.
- Combo **Quebra de Formação** na aba Especiais: 10 Contra Golpes perfeitos em mobs hostis desbloqueiam a finalização.
- Jab, charge, Lunge e o alcance vanilla da lança permanecem. Parry, dash, deslize e o restante da 1.1.3 não mudam.
- Romper não altera o encantamento Ruptura da maça.
- Guia de teste: [SPEAR-TESTE.md](SPEAR-TESTE.md).

### 1.1.3

- Especiais de combo para espada, duas espadas, machado, arco, tridente, escudo e maça. Cada um abre na aba Especiais dos advancements.
- Novo movimento **Parry** (`X`): devolve o ataque com o dobro do dano e atordoa o atacante por 3 s. Recarga de 60 s ao acertar e 30 s ao errar.
- A estocada conta no combo da espada e das duas espadas.
- O combo das duas espadas agora é de 7 golpes.
- **Alcance** aumenta o alcance de todos os golpes da espada, não só da estocada.
- A estocada funciona com o alvo colado em você e com o clique direito mirando em uma entidade.
- O ataque pesado carrega olhando para qualquer lugar, inclusive blocos, e também com um escudo na outra mão.

### 1.1.2

- Dash, deslize e pulo duplo aparecem como ícones à esquerda da hotbar, com a recarga preenchendo o ícone.

### 1.1.1

- Cada encantamento agora tem um lugar próprio para ser encontrado (veja "Onde conseguir").
- Afogados deixam cair o tridente em 70% das mortes.

### 1.1.0

- Maça acorrentada: o clique direito lança a maça presa a uma corrente, com onda de impacto que empurra mobs, players e você mesmo.
- Novo encantamento **Chain** para a maça.

### 1.0.1

- Encantamentos **Kick**, **Dashing** e **Jumping**.
- Barras de recarga de dash, deslize e pulo duplo na tela.
