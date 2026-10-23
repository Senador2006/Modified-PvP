package com.example.modifiedpvp.combat.adaptive;

import java.util.EnumMap;
import java.util.List;
import java.util.UUID;

public final class EvidenceEvaluator {
	private EvidenceEvaluator() {
	}

	public static EnumMap<AdaptiveStyle, Float> score(WeaponFamily family, List<CombatActionEvent> events, AdaptiveBalanceConfig config) {
		EnumMap<AdaptiveStyle, Float> scores = new EnumMap<>(AdaptiveStyle.class);
		for (AdaptiveStyle style : AdaptiveStyle.of(family)) {
			scores.put(style, 0.0F);
		}
		if (events.isEmpty()) {
			return scores;
		}

		UUID focus = events.getLast().targetId();
		CombatActionType lastType = null;
		boolean sawMelee = false;
		boolean sawThrust = false;
		boolean sawMain = false;
		boolean sawCombined = false;
		int alternations = 0;

		for (CombatActionEvent event : events) {
			if (event.has(ContextTag.FINISHER_DAMAGE) || !event.hitValid() && event.type() != CombatActionType.HEAVY_BLOCKED) {
				continue;
			}
			if (event.type() == CombatActionType.MELEE_HIT) {
				sawMelee = true;
				sawMain = true;
			} else if (event.type() == CombatActionType.THRUST_HIT) {
				sawThrust = true;
			} else if (event.type() == CombatActionType.OFFHAND_COMBINED_HIT) {
				sawCombined = true;
			}
			if (lastType != null && lastType != event.type() && isDualPair(lastType, event.type())) {
				alternations++;
			}
			lastType = event.type();

			float splash = 1.0F + Math.min(3, Math.max(0, event.affectedTargets() - 1)) * config.splashTargetBonus();
			for (AdaptiveStyle style : AdaptiveStyle.of(family)) {
				float contribution = contribution(style, event, focus, config) * splash;
				if (contribution > 0.0F) {
					scores.merge(style, contribution, Float::sum);
				}
			}
		}

		if (family == WeaponFamily.SWORD && sawMelee && sawThrust) {
			scores.merge(AdaptiveStyle.RITMO_CORTE, config.varietyBonus(), Float::sum);
		}
		if (family == WeaponFamily.DUAL_SWORD && sawMain && sawCombined) {
			scores.merge(AdaptiveStyle.ALTERNANCIA_PERFEITA, config.varietyBonus() + alternations * 0.35F, Float::sum);
			scores.merge(AdaptiveStyle.PRESSAO_ALTERNADA, config.varietyBonus() * 0.6F, Float::sum);
		}
		return scores;
	}

	public static AdaptiveStyle select(EnumMap<AdaptiveStyle, Float> scores, AdaptiveStyle previous, WeaponFamily family, AdaptiveBalanceConfig config) {
		AdaptiveStyle best = null;
		float bestScore = 0.0F;
		float required = config.minEvidence(family);
		for (AdaptiveStyle style : AdaptiveStyle.of(family)) {
			float score = scores.getOrDefault(style, 0.0F);
			if (score + 1.0E-4F < required) {
				continue;
			}
			if (best == null || score > bestScore + 1.0E-4F || (Math.abs(score - bestScore) <= 1.0E-4F && keep(previous, style, best))) {
				best = style;
				bestScore = score;
			}
		}
		return best;
	}

	public static float contribution(AdaptiveStyle style, CombatActionEvent event, UUID focus, AdaptiveBalanceConfig config) {
		float base = event.blocked() && styleWantsBlock(style) ? config.blockedWeight() : event.hitValid() ? config.hitWeight() : 0.0F;
		if (base <= 0.0F) {
			return 0.0F;
		}
		if (event.perfectTiming()) {
			base += config.perfectWeight();
		}
		if (focus != null && event.sameTarget(focus)) {
			base += config.sameTargetBonus();
		}
		return base * styleWeight(style, event);
	}

	private static boolean keep(AdaptiveStyle previous, AdaptiveStyle candidate, AdaptiveStyle currentBest) {
		if (previous != null && previous == candidate) {
			return true;
		}
		if (previous != null && previous == currentBest) {
			return false;
		}
		return candidate.priority() < currentBest.priority();
	}

	private static boolean isDualPair(CombatActionType a, CombatActionType b) {
		return a == CombatActionType.MELEE_HIT && b == CombatActionType.OFFHAND_COMBINED_HIT
				|| a == CombatActionType.OFFHAND_COMBINED_HIT && b == CombatActionType.MELEE_HIT;
	}

	private static boolean styleWantsBlock(AdaptiveStyle style) {
		return style == AdaptiveStyle.RUPTURA_DEFENSIVA
				|| style == AdaptiveStyle.ABERTURA_CRUZADA
				|| style == AdaptiveStyle.GUARDA_INABALAVEL
				|| style == AdaptiveStyle.INTERCEPTACAO
				|| style == AdaptiveStyle.GUARDA_PROTECAO
				|| style == AdaptiveStyle.CONTROLE_IMPACTO;
	}

	private static float styleWeight(AdaptiveStyle style, CombatActionEvent event) {
		return switch (style) {
			case RITMO_CORTE -> event.type() == CombatActionType.MELEE_HIT || event.type() == CombatActionType.THRUST_HIT ? 1.0F : 0.0F;
			case PONTA_PRECISAO -> event.type() == CombatActionType.THRUST_HIT ? 1.35F : 0.0F;
			case RESPOSTA_LAMINA -> event.type() == CombatActionType.PERFECT_COUNTER ? 1.8F : event.type() == CombatActionType.MELEE_HIT && event.has(ContextTag.COUNTER) ? 0.8F : 0.0F;
			case ALTERNANCIA_PERFEITA, PRESSAO_ALTERNADA -> event.type() == CombatActionType.MELEE_HIT || event.type() == CombatActionType.OFFHAND_COMBINED_HIT ? 1.0F : 0.0F;
			case ABERTURA_CRUZADA -> event.blocked() && event.type() == CombatActionType.MELEE_HIT || event.type() == CombatActionType.OFFHAND_COMBINED_HIT ? 1.1F : 0.0F;
			case FORCA_DOMINANTE -> event.type() == CombatActionType.HEAVY_FULL_HIT && !event.blocked() ? 1.2F : 0.0F;
			case RUPTURA_DEFENSIVA -> event.type() == CombatActionType.HEAVY_BLOCKED || event.blocked() && event.type() == CombatActionType.HEAVY_FULL_HIT ? 1.3F : 0.0F;
			case PRECISAO_CORRENTE -> event.type() == CombatActionType.CHAIN_ENTITY_IMPACT || event.type() == CombatActionType.CHAIN_RETURN ? 1.2F : 0.0F;
			case CONTROLE_IMPACTO -> event.type() == CombatActionType.CHAIN_BLOCK_IMPACT ? 1.3F : 0.0F;
			case RETORNO_SOBERANO -> event.type() == CombatActionType.TRIDENT_THROW_HIT || event.type() == CombatActionType.TRIDENT_LOYALTY_RETURN ? 1.15F : 0.0F;
			case DOMINIO_MARES -> event.type() == CombatActionType.TRIDENT_MELEE_HIT || event.type() == CombatActionType.RIPTIDE_HIT ? 1.2F : 0.0F;
			case DOMINIO_TEMPESTADE -> event.type() == CombatActionType.TRIDENT_THROW_HIT || event.type() == CombatActionType.CHANNELING_HIT ? 1.1F : 0.0F;
			case MIRA_PERFEITA -> event.type() == CombatActionType.BOW_ARROW_HIT ? 1.0F : 0.0F;
			case DETONACAO_PRECISA -> event.type() == CombatActionType.BOW_CART_IGNITION ? 1.8F : 0.0F;
			case BOMBARDEIO -> event.type() == CombatActionType.CROSSBOW_FIREWORK_SPLASH && event.affectedTargets() >= 1 ? 1.0F : 0.0F;
			case PRECISAO_EXPLOSIVA -> event.type() == CombatActionType.CROSSBOW_FIREWORK_SPLASH ? 1.15F : 0.0F;
			case CONTRA_GOLPE -> event.type() == CombatActionType.PERFECT_COUNTER || event.has(ContextTag.COUNTER) ? 1.4F : 0.0F;
			case INVESTIDA_CINETICA -> event.type() == CombatActionType.SPEAR_CHARGE || event.has(ContextTag.KINETIC) ? 1.3F : 0.0F;
			case FORMACAO_ROMPIDA -> event.type() == CombatActionType.SPEAR_JAB || event.has(ContextTag.FORMATION) ? 1.1F : 0.0F;
			case GUARDA_INABALAVEL -> event.type() == CombatActionType.SHIELD_BLOCK || event.type() == CombatActionType.SHIELD_BLOCK_TIMED ? 1.0F : 0.0F;
			case INTERCEPTACAO -> event.type() == CombatActionType.SHIELD_BLOCK_TIMED ? 1.6F : 0.0F;
			case GUARDA_PROTECAO -> event.type() == CombatActionType.SHIELD_INTERCEPT_ALLY ? 1.7F : 0.0F;
		};
	}
}
