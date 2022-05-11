package com.terraforming.ares.dto;

import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.ProjectCard;
import com.terraforming.ares.model.SpecialEffect;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.income.Gain;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Value
public class ProjectCardDto {
    int id;
    String name;
    int price;
    int winPoints;
    List<Tag> tags;
    String description;
    List<Gain> incomes;
    CardColor cardColor;
    List<SpecialEffect> specialEffects;

    public static ProjectCardDto from(ProjectCard card) {
        return new ProjectCardDto(
                card.getId(),
                card.getCardMetadata().getName(),
                card.getPrice(),
                card.getWinningPoints(),
                card.getTags(),
                card.getCardMetadata().getDescription(),
                card.getIncomes(),
                card.getColor(),
                card.getSpecialEffects().isEmpty() ? List.of() : new ArrayList<>(card.getSpecialEffects())
        );
    }
}
