package com.terraforming.ares.services.ai.network2.dto;

import com.terraforming.ares.model.Card;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CardWithChanceModifier {
    private Card card;
    private double chance;
    private double modifier;

    public CardWithChanceModifier(Card card, double chance) {
        this.card = card;
        this.chance = chance;
        this.modifier = 1;
    }
}
