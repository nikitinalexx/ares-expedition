package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.SymbioticFungus;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class SymbioticFungudActionProcessor implements BlueActionCardProcessor<SymbioticFungus> {
    private final CardService cardService;

    @Override
    public Class<SymbioticFungus> getType() {
        return SymbioticFungus.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard, List<Integer> inputParameters) {
        Integer cardIdToPutMicrobeOn = inputParameters.get(0);

        Card projectCard = cardService.getCard(cardIdToPutMicrobeOn);

        player.addResources(projectCard, 1);

        return null;
    }

}
