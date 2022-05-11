package com.terraforming.ares.cards;

import com.terraforming.ares.model.income.Gain;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 10.05.2022
 */
@Builder
@Value
public class CardMetadata {
    String name;
    String description;
    @Builder.Default
    List<Gain> incomes = List.of();
    @Builder.Default
    List<Gain> bonuses = List.of();
}
