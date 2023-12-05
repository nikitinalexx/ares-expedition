package com.terraforming.ares.dto;

import com.terraforming.ares.model.*;
import com.terraforming.ares.model.action.ActionInputData;
import com.terraforming.ares.model.build.PutResourceOnBuild;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.parameters.OceanRequirement;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.model.winPoints.WinPointsInfo;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Value
public class CardDto {
    int id;
    String name;
    int price;
    int winPoints;
    List<Tag> tags;
    String description;
    String actionDescription;
    List<Gain> incomes;
    CardColor cardColor;
    List<SpecialEffect> specialEffects;
    CardAction cardAction;
    List<Tag> tagReq;
    List<ParameterColor> tempReq;
    List<ParameterColor> oxygenReq;
    List<ParameterColor> infrastructureReq;
    OceanRequirement oceanRequirement;
    List<Gain> bonuses;
    WinPointsInfo winPointsInfo;
    CardCollectableResource cardResource;
    List<PutResourceOnBuild> resourcesOnBuild;
    boolean isActive;
    List<ActionInputData> actionInputData;
    boolean corporation;
    Expansion expansion;

    public static CardDto from(Card card) {
        return new CardDto(
                card.getId(),
                card.getCardMetadata().getName(),
                card.getPrice(),
                card.getWinningPoints(),
                card.getTags(),
                card.getCardMetadata().getDescription(),
                card.getCardMetadata().getActionDescription(),
                card.getCardMetadata().getIncomes(),
                card.getColor(),
                card.getSpecialEffects().isEmpty() ? List.of() : new ArrayList<>(card.getSpecialEffects()),
                card.getCardMetadata().getCardAction(),
                card.getTagRequirements(),
                card.getTemperatureRequirement(),
                card.getOxygenRequirement(),
                card.getInfrastructureRequirement(),
                card.getOceanRequirement(),
                card.getCardMetadata().getBonuses(),
                card.getCardMetadata().getWinPointsInfo(),
                card.getCollectableResource() != CardCollectableResource.NONE ? card.getCollectableResource() : null,
                card.getCardMetadata().getResourcesOnBuild(),
                card.isActiveCard(),
                card.getCardMetadata().getActionsInputData(),
                card.isCorporation(),
                card.getExpansion()
        );
    }

}
