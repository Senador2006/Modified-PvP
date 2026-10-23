package com.example.modifiedpvp.combat.adaptive;

import java.util.EnumMap;
import java.util.UUID;

public final class OpportunityResolver {
	private OpportunityResolver() {
	}

	public static AdaptiveStyle evaluate(FamilyCombatState state, WeaponFamily family, AdaptiveBalanceConfig config, long now) {
		if (state.phase() == FamilyPhase.READY && state.opportunity() != null && !state.opportunity().expired(now)) {
			return state.opportunity().style();
		}
		if (state.phase() == FamilyPhase.COOLDOWN && now < state.cooldownUntil()) {
			return null;
		}
		state.window().trim(config.maxWindowEvents(), now - config.windowTicks(family));
		EnumMap<AdaptiveStyle, Float> scores = EvidenceEvaluator.score(family, state.window().events(), config);
		for (AdaptiveStyle style : AdaptiveStyle.of(family)) {
			state.setEvidence(style, scores.getOrDefault(style, 0.0F));
		}
		return EvidenceEvaluator.select(scores, state.lastStyle(), family, config);
	}

	public static FinisherOpportunity lock(WeaponFamily family, AdaptiveStyle style, long now, UUID targetId, int sourceItem, AdaptiveBalanceConfig config) {
		return new FinisherOpportunity(family, style, now + config.readyTicks(family), targetId, sourceItem);
	}

	public static boolean triggerMatches(AdaptiveStyle style, CombatActionEvent event, FinisherOpportunity opportunity) {
		if (opportunity == null || style != opportunity.style()) {
			return false;
		}
		if (!opportunity.matchesTarget(event.targetId())) {
			return false;
		}
		return switch (style) {
			case RITMO_CORTE, RESPOSTA_LAMINA -> event.type() == CombatActionType.MELEE_HIT;
			case PONTA_PRECISAO -> event.type() == CombatActionType.THRUST_HIT;
			case ALTERNANCIA_PERFEITA, PRESSAO_ALTERNADA -> event.type() == CombatActionType.OFFHAND_COMBINED_HIT;
			case ABERTURA_CRUZADA -> event.type() == CombatActionType.MELEE_HIT;
			case FORCA_DOMINANTE -> event.type() == CombatActionType.HEAVY_FULL_HIT;
			case RUPTURA_DEFENSIVA -> event.type() == CombatActionType.HEAVY_BLOCKED || event.type() == CombatActionType.HEAVY_FULL_HIT && event.blocked();
			case PRECISAO_CORRENTE -> event.type() == CombatActionType.CHAIN_ENTITY_IMPACT;
			case CONTROLE_IMPACTO -> event.type() == CombatActionType.CHAIN_BLOCK_IMPACT;
			case RETORNO_SOBERANO, DOMINIO_TEMPESTADE -> event.type() == CombatActionType.TRIDENT_THROW_HIT || event.type() == CombatActionType.CHANNELING_HIT;
			case DOMINIO_MARES -> event.type() == CombatActionType.TRIDENT_MELEE_HIT || event.type() == CombatActionType.RIPTIDE_HIT;
			case MIRA_PERFEITA -> event.type() == CombatActionType.BOW_ARROW_HIT;
			case DETONACAO_PRECISA -> event.type() == CombatActionType.BOW_CART_IGNITION;
			case BOMBARDEIO, PRECISAO_EXPLOSIVA -> event.type() == CombatActionType.CROSSBOW_FIREWORK_SPLASH;
			case CONTRA_GOLPE, INVESTIDA_CINETICA, FORMACAO_ROMPIDA -> false;
			case GUARDA_INABALAVEL -> event.type() == CombatActionType.SHIELD_BLOCK || event.type() == CombatActionType.SHIELD_BLOCK_TIMED;
			case INTERCEPTACAO -> event.type() == CombatActionType.SHIELD_BLOCK_TIMED;
			case GUARDA_PROTECAO -> event.type() == CombatActionType.SHIELD_INTERCEPT_ALLY;
		};
	}

	public static boolean consumeOnFailedAttempt(AdaptiveStyle style, CombatActionEvent event) {
		return switch (style) {
			case RITMO_CORTE, PONTA_PRECISAO, RESPOSTA_LAMINA, ALTERNANCIA_PERFEITA, PRESSAO_ALTERNADA,
					ABERTURA_CRUZADA, FORCA_DOMINANTE, RUPTURA_DEFENSIVA, PRECISAO_CORRENTE, CONTROLE_IMPACTO,
					RETORNO_SOBERANO, DOMINIO_MARES, DOMINIO_TEMPESTADE, MIRA_PERFEITA, DETONACAO_PRECISA,
					BOMBARDEIO, PRECISAO_EXPLOSIVA -> event.type() != CombatActionType.FINISHER_ATTEMPT || event.hitValid();
			default -> false;
		};
	}
}
