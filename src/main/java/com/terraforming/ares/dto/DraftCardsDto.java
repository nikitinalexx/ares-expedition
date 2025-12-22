package com.terraforming.ares.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DraftCardsDto {
    private int cardsToTake;
    private int cardsToSee;
}
