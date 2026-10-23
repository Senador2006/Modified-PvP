package com.example.modifiedpvp.combat.adaptive;

public enum AdaptiveStyle {
	RITMO_CORTE(WeaponFamily.SWORD, 3, "combat.modifiedpvp.style.ritmo", "combat.modifiedpvp.ready.relampago"),
	PONTA_PRECISAO(WeaponFamily.SWORD, 2, "combat.modifiedpvp.style.ponta", "combat.modifiedpvp.ready.perfuracao"),
	RESPOSTA_LAMINA(WeaponFamily.SWORD, 1, "combat.modifiedpvp.style.resposta", "combat.modifiedpvp.ready.contra_corte"),
	ALTERNANCIA_PERFEITA(WeaponFamily.DUAL_SWORD, 2, "combat.modifiedpvp.style.alternancia", "combat.modifiedpvp.ready.cruzado"),
	PRESSAO_ALTERNADA(WeaponFamily.DUAL_SWORD, 3, "combat.modifiedpvp.style.pressao", "combat.modifiedpvp.ready.rajada"),
	ABERTURA_CRUZADA(WeaponFamily.DUAL_SWORD, 1, "combat.modifiedpvp.style.abertura", "combat.modifiedpvp.ready.abertura"),
	FORCA_DOMINANTE(WeaponFamily.AXE, 2, "combat.modifiedpvp.style.dominante", "combat.modifiedpvp.ready.soberano"),
	RUPTURA_DEFENSIVA(WeaponFamily.AXE, 1, "combat.modifiedpvp.style.ruptura", "combat.modifiedpvp.ready.ruina"),
	PRECISAO_CORRENTE(WeaponFamily.MACE, 1, "combat.modifiedpvp.style.corrente", "combat.modifiedpvp.ready.retorno"),
	CONTROLE_IMPACTO(WeaponFamily.MACE, 2, "combat.modifiedpvp.style.impacto", "combat.modifiedpvp.ready.repulsao"),
	RETORNO_SOBERANO(WeaponFamily.TRIDENT, 2, "combat.modifiedpvp.style.retorno", "combat.modifiedpvp.ready.arremesso"),
	DOMINIO_MARES(WeaponFamily.TRIDENT, 3, "combat.modifiedpvp.style.mares", "combat.modifiedpvp.ready.investida"),
	DOMINIO_TEMPESTADE(WeaponFamily.TRIDENT, 1, "combat.modifiedpvp.style.tempestade", "combat.modifiedpvp.ready.julgamento"),
	MIRA_PERFEITA(WeaponFamily.BOW, 1, "combat.modifiedpvp.style.mira", "combat.modifiedpvp.ready.certeiro"),
	DETONACAO_PRECISA(WeaponFamily.BOW, 2, "combat.modifiedpvp.style.detonacao", "combat.modifiedpvp.ready.ignicao"),
	BOMBARDEIO(WeaponFamily.CROSSBOW, 2, "combat.modifiedpvp.style.bombardeio", "combat.modifiedpvp.ready.barragem"),
	PRECISAO_EXPLOSIVA(WeaponFamily.CROSSBOW, 1, "combat.modifiedpvp.style.explosiva", "combat.modifiedpvp.ready.descarga"),
	CONTRA_GOLPE(WeaponFamily.SPEAR, 1, "combat.modifiedpvp.style.spear_counter", "combat.modifiedpvp.ready.reacao"),
	INVESTIDA_CINETICA(WeaponFamily.SPEAR, 2, "combat.modifiedpvp.style.cinetica", "combat.modifiedpvp.ready.implacavel"),
	FORMACAO_ROMPIDA(WeaponFamily.SPEAR, 3, "combat.modifiedpvp.style.formacao", "combat.modifiedpvp.ready.circular"),
	GUARDA_INABALAVEL(WeaponFamily.SHIELD, 3, "combat.modifiedpvp.style.guarda", "combat.modifiedpvp.ready.repulsao_escudo"),
	INTERCEPTACAO(WeaponFamily.SHIELD, 1, "combat.modifiedpvp.style.interceptacao", "combat.modifiedpvp.ready.choque"),
	GUARDA_PROTECAO(WeaponFamily.SHIELD, 2, "combat.modifiedpvp.style.protecao", "combat.modifiedpvp.ready.intervencao");

	private final WeaponFamily family;
	private final int priority;
	private final String styleKey;
	private final String readyKey;

	AdaptiveStyle(WeaponFamily family, int priority, String styleKey, String readyKey) {
		this.family = family;
		this.priority = priority;
		this.styleKey = styleKey;
		this.readyKey = readyKey;
	}

	public WeaponFamily family() {
		return this.family;
	}

	public int priority() {
		return this.priority;
	}

	public String styleKey() {
		return this.styleKey;
	}

	public String readyKey() {
		return this.readyKey;
	}

	public boolean requiresSameTarget() {
		return this == PONTA_PRECISAO
				|| this == ABERTURA_CRUZADA
				|| this == PRESSAO_ALTERNADA
				|| this == PRECISAO_EXPLOSIVA
				|| this == RESPOSTA_LAMINA;
	}

	public static AdaptiveStyle[] of(WeaponFamily family) {
		return switch (family) {
			case SWORD -> new AdaptiveStyle[] {RESPOSTA_LAMINA, PONTA_PRECISAO, RITMO_CORTE};
			case DUAL_SWORD -> new AdaptiveStyle[] {ABERTURA_CRUZADA, ALTERNANCIA_PERFEITA, PRESSAO_ALTERNADA};
			case AXE -> new AdaptiveStyle[] {RUPTURA_DEFENSIVA, FORCA_DOMINANTE};
			case MACE -> new AdaptiveStyle[] {PRECISAO_CORRENTE, CONTROLE_IMPACTO};
			case TRIDENT -> new AdaptiveStyle[] {DOMINIO_TEMPESTADE, RETORNO_SOBERANO, DOMINIO_MARES};
			case BOW -> new AdaptiveStyle[] {MIRA_PERFEITA, DETONACAO_PRECISA};
			case CROSSBOW -> new AdaptiveStyle[] {PRECISAO_EXPLOSIVA, BOMBARDEIO};
			case SPEAR -> new AdaptiveStyle[] {CONTRA_GOLPE, INVESTIDA_CINETICA, FORMACAO_ROMPIDA};
			case SHIELD -> new AdaptiveStyle[] {INTERCEPTACAO, GUARDA_PROTECAO, GUARDA_INABALAVEL};
		};
	}
}
