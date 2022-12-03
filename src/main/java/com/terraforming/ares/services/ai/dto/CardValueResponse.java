package com.terraforming.ares.services.ai.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CardValueResponse {
    int cardId;
    double worth;

    public static CardValueResponse of(int cardId, double worth) {
        return CardValueResponse.builder().cardId(cardId).worth(worth).build();
    }
}
