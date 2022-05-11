package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.SymbioticFungus;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.ProjectCard;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class SymbioticFungudActionValidator implements ActionValidator<SymbioticFungus> {
    private final CardService cardService;

    @Override
    public Class<SymbioticFungus> getType() {
        return SymbioticFungus.class;
    }

    @Override
    public String validate(MarsGame game, Player player, List<Integer> inputParameters) {
        if (CollectionUtils.isEmpty(inputParameters)) {
            return "SymbioticFungud requires a card to put a microbe on";
        }

        Integer selectedCardId = inputParameters.get(0);

        if (!player.getPlayed().containsCard(selectedCardId)) {
            return "SymbioticFungud - player doesn't have the selected card";
        }

        ProjectCard card = cardService.getProjectCard(selectedCardId);
        if (card.getCollectableResource() != CardCollectableResource.MICROBE) {
            return "SymbioticFungud may only place a microbe on a microbe collecting card";
        }

        return null;
    }
}
