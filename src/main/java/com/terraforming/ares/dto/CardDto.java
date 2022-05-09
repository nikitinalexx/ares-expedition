package com.terraforming.ares.dto;

import com.terraforming.ares.model.GenericCard;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.income.Income;
import lombok.Value;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Value
public class CardDto {
    String name;
    int price;
    List<Tag> tags;
    String description;
    List<Income> incomes;

    public static CardDto from(GenericCard card) {
        return new CardDto(
                card.getClass().getSimpleName(),
                card.getPrice(),
                card.getTags(),
                card.description(),
                card.getIncomes()
        );
    }
}
