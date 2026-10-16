# Modified PvP

Movimentação e armas extras para PvP no Minecraft **1.21.11**, feito para Fabric.

O cliente prevê o movimento. O dano, o empurrão e os projéteis só valem no servidor.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-62b47a?style=flat-square)](https://fabricmc.net/develop/)
[![Fabric Loader](https://img.shields.io/badge/Fabric_Loader-0.19.5+-dbd0b4?style=flat-square)](https://fabricmc.net/)
[![Fabric API](https://img.shields.io/badge/Fabric_API-0.141.6-c8a2c8?style=flat-square)](https://modrinth.com/mod/fabric-api)
[![Java](https://img.shields.io/badge/Java-21-f89820?style=flat-square)](https://adoptium.net/)
[![License](https://img.shields.io/badge/license-CC0-lightgrey?style=flat-square)](LICENSE)

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
| Estocada | `V` ou clique direito com espada | Avança e atinge o primeiro alvo à frente. Gasta a carga do ataque. |
| Ataque pesado | Segure o clique direito com machado | Solte para golpear. A barra aparece na hotbar de ação. Olhar para um bloco continua descascando madeira. |
| Trocar flecha | `G` com o arco na mão | Passa para a próxima flecha do bundle. |

Escudo, comida ou lança na mão secundária continuam usando o clique direito. A estocada ainda sai na tecla `V`.

## Combate

### Movimento

Dash, pulo duplo e deslize funcionam sem arma na mão. Não saem dentro da água, na lava, montado ou planando.

### Espada

Uma espada na mão principal estoca. Com outra na mão secundária, o clique acerta com as duas. A segunda mão causa uma fração do dano.

### Machado

Segure o clique direito para carregar o golpe pesado. Quanto mais a barra enche, mais forte o acerto. Ele atravessa até três alvos e pode derrubar bloqueio.

### Arco

Flechas na mão secundária carregam o arco mais rápido, e a animação acompanha. Um bundle de flechas na mão secundária também acelera a carga.

Flechas diferentes (normal, espectral, com efeito) podem ficar no mesmo bundle. Com o arco na mão, `G` escolhe qual será disparada. Flechas soltas na mão secundária têm prioridade sobre o bundle.

### Tridente

**Lealdade** puxa o alvo até você e aplica um stun: lentidão, fraqueza, sem pulo e sem andar. Nível mais alto puxa mais rápido e segura por mais tempo.

### Escudo

Com o escudo erguido, quem estiver na frente é empurrado a cada meio segundo.

### Besta

Uma carga da **Escopeta** coloca várias munições de uma vez e dispara todas em leque. Cada uma é gasta. Vale flecha, fogo de artifício, ou os dois no mesmo carregamento.

O **Grappling Hook** puxa você até o ponto em que a flecha ou o fogo de artifício acerta.

## Encantamentos

Todos entram na mesa de encantamentos, em saque e em trocas com aldeões.

| Encantamento | Item | Nível | Efeito |
| --- | --- | --- | --- |
| Alcance | Espada | III | Aumenta o alcance da estocada. |
| Golpe Duplo | Espada | III | Com duas espadas, ataca mais rápido e a mão secundária causa um pouco mais de dano. |
| Corte | Machado | III | Aumenta o dano do ataque pesado. |
| Flecha Dupla | Arco | I | Dispara duas flechas. A segunda também causa dano. Gasta uma flecha. |
| Drenar | Tridente | I | O tridente lançado apaga a água e as colunas de bolhas que atravessa. Não combina com Impulso. |
| Empurrão | Escudo | III | Aumenta a força do empurrão com o escudo erguido. |
| Grappling Hook | Besta | I | Puxa você até o impacto do tiro. |
| Escopeta | Besta | III | Nível I, II e III carregam 2, 3 e 4 munições. |

Escopeta e Grappling Hook não combinam entre si, nem com Tiro Múltiplo ou Perfuração.

Lealdade continua sendo o encantamento vanilla. O mod só muda o que acontece no acerto.


