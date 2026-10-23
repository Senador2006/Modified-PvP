# Modified PvP 1.3.0 — checklist de testes

Valores de evidência, janela e recarga ainda são calibráveis em `config/modifiedpvp.properties`. Este checklist cobre regressão 1.2.2 e o motor adaptativo.

## Motor

- [ ] Dois jogadores com o mesmo histórico recebem o mesmo estilo (sem aleatoriedade).
- [ ] Estilo em READY não muda até consumir ou expirar.
- [ ] Evento com o mesmo `actionId` não conta duas vezes (Multishot/Escopeta/foguete).
- [ ] Recarga por família sobrevive a troca de slot, item idêntico, logout/login e mudança de dimensão.
- [ ] Morte, logout e troca de mundo limpam oportunidade e evidência; advancements permanecem.
- [ ] Cliente não cria dano nem escolhe estilo: só mostra snapshot do servidor.
- [ ] Finalizador expirado não aplica efeito e o HUD some de READY.

## Armas

- [ ] Espada: corte, estocada e parry abrem as três técnicas sem dupla ativação. Alcance e defesa continuam.
- [ ] Duas espadas: esquerdo = principal; direito = conjunto; `V` = estocada. Sem botão isolado de offhand. Conjunto não duplica dano da janela de invulnerabilidade no Corte Cruzado.
- [ ] Machado: carga e recuperação iguais. Ruína depende de bloqueio real. Soberano não substitui quebra de guarda.
- [ ] Maça: entidade vs bloco separam estilos. Retorno no mesmo slot. Morte com maça no ar. Smash/Wind Burst vanilla.
- [ ] Tridente: clima seco com impacto físico. Condutividade só na tempestade vanilla. Lealdade não puxa e empurra ao mesmo tempo. Correnteza só onde o vanilla permite.
- [ ] Arco: `G`, offhand e bundle iguais. TNT minecart sem explosão/velocidade extra. Detonação creditada ao atirador.
- [ ] Besta: foguete em área = uma ação. Empurrão limitado. Escopeta e Grappling iguais. Estrelas não duplicam.
- [ ] Lança: Reação Total, Colapso Circular e Investida Implacável competem por uma Defesa Rompida. `Z`, jab, charge e Lunge iguais.
- [ ] Escudo: timing medido no servidor. Bloqueio passivo não é interceptação. Sem parry + Choque no mesmo hit. Aliado exige time e trajetória.

## Balanço

- [ ] Duelo 1x1: nenhum finisher decide a luta sozinho.
- [ ] Grupo: empurrões respeitam resistência a controle.
- [ ] PvE: mobs e chefes continuam diferentes.
- [ ] Lag não duplica dano nem perde o retorno da maça.

## Detector (sem mundo)

`EvidenceEvaluator.score` e `EvidenceEvaluator.select` são determinísticos: mesma lista de `CombatActionEvent` produz o mesmo estilo. Empate mantém o estilo anterior ou a prioridade documentada em `AdaptiveStyle.priority()`.
