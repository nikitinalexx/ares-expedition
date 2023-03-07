package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.ResearchGrant;
import com.terraforming.ares.cards.corporations.DummyCard;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.MarsContextProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 21.02.2023
 */
@Component
@RequiredArgsConstructor
public class ResearchGrantActionProcessor implements BlueActionCardProcessor<ResearchGrant> {
    private final CardService cardService;
    private final MarsContextProvider marsContextProvider;

    @Override
    public Class<ResearchGrant> getType() {
        return ResearchGrant.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard, Map<Integer, List<Integer>> inputParameters) {
        final List<Tag> cardTags = player.getCardToTag().get(ResearchGrant.class);

        final Tag inputTag = Tag.byIndex(inputParameters.get(InputFlag.TAG_INPUT.getId()).get(0));

        assert inputTag != null;

        for (int i = 0; i < cardTags.size(); i++) {
            if (cardTags.get(i) == Tag.DYNAMIC) {
                cardTags.set(i, inputTag);
                break;
            }
        }

        final MarsContext marsContext = marsContextProvider.provide(game, player);

        player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(Card::onBuiltEffectApplicableToOther)
                .forEach(card -> card.postProjectBuiltEffect(marsContext, new DummyCard(), inputParameters));

        if (game.isCrysis()) {
            new ArrayList<>(game.getCrysisData()
                    .getOpenedCards())
                    .stream()
                    .map(cardService::getCrysisCard)
                    .forEach(c -> c.postProjectBuiltEffect(marsContext, new DummyCard(), inputParameters));
        }


        return null;
    }

}
