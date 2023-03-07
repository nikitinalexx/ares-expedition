package com.terraforming.ares.dto;

import com.terraforming.ares.model.CardTier;
import com.terraforming.ares.model.CrysisActiveCardAction;
import com.terraforming.ares.model.CrysisCard;
import com.terraforming.ares.model.CrysisCardAction;
import lombok.Value;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Value
public class CrysisCardDto {
    int id;
    CardTier cardTier;
    int playerCount;
    String name;
    CrysisCardAction cardAction;
    String immediateEffect;
    List<String> immediateOptions;
    List<String> persistentOptions;
    int initialTokens;
    boolean persistentEffectRequiresChoice;
    CrysisActiveCardAction crysisActiveCardAction;


    public static CrysisCardDto from(CrysisCard card) {
        return new CrysisCardDto(
                card.getId(),
                card.tier(),
                card.playerCount(),
                card.name(),
                card.cardAction(),
                card.immediateEffect(),
                card.immediateOptions(),
                card.persistentOptions(),
                card.initialTokens(),
                card.persistentEffectRequiresChoice(),
                card.getActiveCardAction()
        );
    }

}
