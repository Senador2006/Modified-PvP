# Modified PvP 1.3.1 — roteiro de teste

Servidor dedicado com o mod no servidor e no cliente. `unlockSpecials=true` libera as famílias sem farm. Os números abaixo são os padrões provisórios.

## Motor (automático)

`.\gradlew.bat test` cobre o classificador puro: prioridade do Golpe Estendido, lunge no ar sem preparação, segunda trajetória do Impacto de Investida, ação repetida, Caçada Implacável, Retorno Ofensivo, Duelo desligado, recarga, Coroa, foguete único e Ruptura Defensiva.

## Pull

1. Tridente só com Lealdade: o acerto não puxa e não atordoa. O tridente volta.
2. Pull sem Lealdade, inclusive item dado por comando: o puxão não acontece.
3. Pull I, II e III com Lealdade: o alvo anda poucos ticks em sua direção (5, 7 e 9) e recupera o controle. Sem stun e sem cancelar pulo durante toda a duração.
4. Parede entre os dois: o puxão não começa. Se a linha de visão some no meio, o puxão para.
5. Dois tridentes no mesmo alvo: o segundo puxão espera a resistência de 40 ticks.
6. Afogado acertado por tridente lançado desbloqueia a família. Acertar o chão não desbloqueia.

## Híbridos

1. Lança com Lunge no ar, sem carga de vento e sem troca para a maça: nenhum estilo fica pronto.
2. Carga de vento, Lunge aéreo e smash válido: Impacto de Investida fica pronto. O smash seguinte, em outra trajetória, dá o deslize horizontal. O mesmo voo não executa de novo.
3. Troca lança → maça dentro de 5 ticks e smash: Golpe Estendido fica pronto. O alcance da maça continua o vanilla.
4. Espada, acerto de tridente com a distância aumentando, Pull, depois um Pull novo e uma estocada no `V`: Caçada Implacável. A estocada do mesmo instante do Pull não executa.
5. Arremesso com Lealdade, golpe de espada enquanto o tridente está fora, retorno do mesmo projétil, depois um arremesso manual: Retorno Ofensivo. Nada dispara sozinho.
6. Machado e escudo: bloqueios e um pesado completo abrem Retaliação. Dois bloqueios avançando abrem Fortaleza; `Alt esquerdo` levanta o escudo e avança. A proteção frontal acaba antes do golpe. Pesado completo em bloqueio ativo abre Ruptura e não soma o atordoamento da Ruína da Guarda.
7. Espada e escudo: bloqueio cronometrado do escudo, não o parry do `X`, abre Contracorte. Baixar o escudo e estocar abre Ponta Protegida; o escudo não cobre a estocada inteira.
8. Espada e besta: tiro manual e estocada no `V` abrem Finta. Flecha contra alvo que avança abre Tiro Repentino. Vários projéteis do mesmo disparo contam como uma ação.
9. Machado e besta: tiro e pesado completo no mesmo alvo, depois a repetição, completam Abertura da Coroa. As outras duas técnicas da coroa seguem o mesmo alvo hostil. No fim, **Technoblade Never Dies** aparece com esse título em inglês. Aliado, tiro no ar e besta pré-carregada sem o disparo da sequência não contam.
10. Morrer, sair ou trocar de dimensão limpa a oportunidade. O progresso da coroa e dos advancements continua.

## Fora desta versão

Duelo Coordenado, Eco do Trovão e Último Recurso permanecem desligados. Tridente + escudo, arco + espada, maça + escudo e lança + besta não existem.
