package com.example.modifiedpvp.combat.hybrid;

import java.util.UUID;

public record HybridPrep(HybridStyleId style, UUID targetId, int aerialId, HybridAction followup) {
}
