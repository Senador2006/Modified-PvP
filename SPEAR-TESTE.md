# Guia de teste — Lança (1.2.1)

Como validar Contra Golpe, Pressão, Romper e Reação Total no Minecraft 1.21.11.

## Preparação

1. Coloque `releases/modifiedpvp-1.2.1.jar` e a Fabric API da 1.21.11 na pasta `mods`.
2. Em servidor, o jar precisa estar no servidor **e** em todos os clientes.
3. Crie um mundo criativo com `/gamemode creative`, depois teste o dano em sobrevivência ou aventura.
4. Itens úteis:

```
/give @s minecraft:iron_spear
/give @s minecraft:netherite_spear
/give @s minecraft:shield
/summon minecraft:zombie ~ ~ ~ {NoAI:0b}
/summon minecraft:husk ~2 ~ ~
```

5. Teclas: `Z` Contra Golpe / Reação Total. `X` é o Parry universal — não misture.
6. Ícone do Contra Golpe fica à esquerda do Parry. Os quatro ícones antigos não devem se mover.

## 1. Contra Golpe — janela perfeita (duas estocadas)

1. Lança na mão principal, no chão, sem água, lava, montaria ou elytra.
2. Enfrente um zumbi e aperte `Z` **no momento em que o braço dele desce**.
3. Esperado:
   - o golpe dele não te acerta;
   - duas estocadas saem sozinhas no zumbi;
   - partículas de crit + som de lança duas vezes;
   - ícone do `Z` pisca e entra em recarga (~6 s depois da postura).
4. Confira o progresso na actionbar: `Quebra de Formação 1/10` (só contra mobs hostis, até desbloquear).

**Falhas comuns:** `Z` cedo demais (ainda na postura, só 60% de redução) ou tarde demais (leva o hit inteiro). A janela perfeita são os primeiros 0,20 s.

## 2. Contra Golpe — fora do perfeito, frente e costas

1. Aperte `Z` e leve um melee **depois** dos 0,20 s, ainda dentro dos 0,60 s, olhando para o mob.
   - Esperado: você toma ~40% do dano (redução de 60%). Sem as duas estocadas.
2. Vire de costas e leve o hit na postura.
   - Esperado: dano cheio. Contra Golpe só vale na frente (cone 120°) com linha de visão.
3. Caia de alto ou entre na lava na postura.
   - Esperado: queda e lava passam. Não existe contra de dano ambiental.
4. Projéteis (esqueleto, flecha) nesta versão **passam**. Isso é proposital.

## 3. Recarga, spam e Parry

1. Acerte um Contra Golpe e aperte `Z` de novo na hora.
   - Esperado: nada. Recarga independente do Parry.
2. Segure `X` e `Z` no mesmo instante.
   - Esperado: um bloqueia o outro. O Parry continua devolvendo 2× e stun de 3 s; o Contra Golpe **não** faz isso.
3. Teste dash `R`, deslize `C`, pulo duplo e estocada `V` com espada depois do contra.
   - Esperado: 1.1.3 intacta.

## 4. Pressão — jab, charge e escudo

Use outro jogador ou um dummy com vida alta. Mire o alvo depois de acertar para ver a barra discreta sob a mira.

| Ação | Pressão |
| --- | --- |
| Jab válido | +8 |
| Jab na faixa longa da lança | +12 (não soma com +8) |
| Charge (segure o botão direito e acerte em movimento) | +18 |
| Acerto na lança contra escudo erguido | +20 |
| Primeira estocada do Contra Golpe perfeito | +35 |
| Romper I nessa riposta | +15 extra |
| Segunda estocada com Romper II | +15 extra |

1. Só jab perto: a barra sobe de 8 em 8.
2. Jab no limite do alcance: sobe 12, não 20.
3. Charge não deve somar jab no mesmo evento.
4. Pare de acertar por 3 s: a barra cai 5 pontos por segundo.
5. Em 100: a barra zera e aparece **Defesa Rompida** no alvo.

## 5. Defesa Rompida e Reação Total (3×)

1. Encha a pressão de um zumbi até 100.
2. Com a técnica **ainda bloqueada**, aperte `Z` perto dele.
   - Esperado: mensagem `Reação Total ainda não foi desbloqueada`. Contra Golpe e Pressão continuam funcionando.
3. Desbloqueie o advancement (10 contras perfeitos em hostis) ou:

```
/advancement grant @s only modifiedpvp:combat/spear
```

4. Com Defesa Rompida ativa, lança na mão, olhando para o alvo, aperte `Z`.
   - Esperado: um impacto forte, **3×** o dano base da lança, partículas em linha. A abertura some.
5. Aperte `Z` de novo na hora.
   - Esperado: não dispara de novo. Recarga do finalizador ~5 s.
6. Deixe a janela expirar (0,80 s em player, 1,50 s em mob) e tente `Z`.
   - Esperado: vira Contra Golpe normal, sem o 3×.
7. PvP: o hit 3× **não** pode matar de uma vez um player cheio de vida. Teto 18 (22 com Romper III) ou 55% da vida máxima.

## 5b. Colapso Circular (estocada)

Com a mesma Defesa Rompida, em vez de `Z`, dê uma **estocada/jab** (clique esquerdo da lança) no alvo marcado.

1. Técnica bloqueada: a jab é normal e aparece `Colapso Circular ainda não foi desbloqueado`.
2. Desbloqueada e na janela: o golpe vira um giro. O alvo marcado leva ~2×. Mobs ao redor (raio 3,5) levam menos e são empurrados. Você gira por uns instantes, com partículas em anel.
3. A abertura some. `Z` depois disso não dispara Reação Total.
4. Charge (botão direito) **não** vira Colapso Circular.
5. As duas estocadas automáticas do Contra Golpe perfeito **não** disparam o giro, mesmo que fechem os 100 de pressão no mesmo instante.
6. Recarga compartilhada com Reação Total (~5 s). Sem insta-kill. Jogadores secundários no giro levam pouco dano (teto 4).

## 6. Romper I–III

Livro no baú raro da Trial Chamber (15%) ou:

```
/enchant @s modifiedpvp:romper 1
/enchant @s modifiedpvp:romper 2
/enchant @s modifiedpvp:romper 3
```

1. **I:** contra perfeito. A primeira estocada automática deve subir +35 e +15.
2. **II:** a segunda estocada automática também soma +15 se acertar o mesmo alvo. Errar a segunda não dá o bônus.
3. **III:** se essa riposta automática fechar 100 de pressão, a próxima Reação Total sai aprimorada (3,5×, onda que empurra mobs perto). Jab/charge que romperem a defesa **não** liberam o aprimorado.

Ruptura da maça continua só na maça. Não encante a lança com ela nem a maça com Romper.

## 7. Limpeza de estado

1. Mate o alvo no meio da pressão: a barra some.
2. `/kill @s` ou relogue: pressão e postura zeram.
3. Entre no Nether e volte: o alvo antigo não deve manter a barra.
4. Dois alvos: pressão de um não vaza para o outro.

## 8. Vanilla da lança

Sem apertar `Z`, confirme que ainda existem:

- Jab no clique esquerdo, com alcance mínimo (colado demais não acerta).
- Charge no clique direito, com as três fases.
- Lunge, se a lança tiver o encantamento vanilla.

O mod não deve anular esses golpes.

## 9. Multiplayer rápido

1. Dois clientes, um servidor dedicado com o mesmo jar.
2. Player A dá `Z` perfeito no melee de B: B toma duas estocadas, A não toma o hit.
3. Encha pressão em B até romper e use Reação Total: B sente o 3×, sem morrer instantâneo de vida cheia.
4. Com ping alto, o `Z` pode parecer atrasado — o servidor é quem decide o timing. O cliente só mostra o ícone.

## Checklist

- [ ] Perfeito = duas estocadas automáticas
- [ ] Fora do perfeito = só 60% de redução
- [ ] Costas / queda / lava / flecha não entram no contra
- [ ] Recarga de 6 s e sem overlap com `X`
- [ ] Jab 8 / jab longa 12 / charge 18 / escudo 20
- [ ] 100 vira Defesa Rompida e zera a barra
- [ ] Reação Total bloqueada sem advancement
- [ ] Reação Total desbloqueada = 3×, uma vez por abertura
- [ ] Colapso Circular na estocada com Defesa Rompida
- [ ] Charge e contra automático não viram o giro
- [ ] Um finalizador consome a abertura do outro
- [ ] Romper I, II e III
- [ ] Morte, logout e dimensão limpam estado
- [ ] Dash, parry, estocada, maça e combos 1.1.3 intactos
