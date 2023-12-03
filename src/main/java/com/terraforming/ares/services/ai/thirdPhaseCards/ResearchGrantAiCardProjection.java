package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.ResearchGrant;
import com.terraforming.ares.cards.corporations.DummyCard;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.ai.DeepNetwork;
import com.terraforming.ares.services.ai.helpers.AiCardBuildParamsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ResearchGrantAiCardProjection<T extends Card> implements AiCardProjection<ResearchGrant> {
    private final MarsContextProvider marsContextProvider;
    private final AiCardBuildParamsService aiCardBuildParamsService;
    private final DeepNetwork deepNetwork;

    @Override
    public Class<ResearchGrant> getType() {
        return ResearchGrant.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card, int network) {
        List<Tag> originalTags = player.getCardToTag().get(ResearchGrant.class);
        //TODO should project while building

        if (originalTags.stream().noneMatch(tag -> tag == Tag.DYNAMIC)) {
            return new MarsGameRowDifference();
        }

        float bestChance = Float.MIN_VALUE;
        Tag targetTag = Tag.SPACE;

        for (Tag tag : Tag.values()) {
            if (tag == Tag.DYNAMIC || originalTags.contains(tag)) {
                continue;
            }

            MarsGame gameCopy = new MarsGame(game);
            Player playerCopy = gameCopy.getPlayerByUuid(player.getUuid());

            float projectedChance = projectPuttingTag(gameCopy, playerCopy, card, tag, network);

            if (projectedChance > bestChance) {
                bestChance = projectedChance;
                targetTag = tag;
            }
        }

        projectPuttingTag(game, player, card, targetTag, network);

        return new MarsGameRowDifference();
    }

    private float projectPuttingTag(MarsGame game, Player player, Card card, Tag tag, int network) {
        List<Tag> cardTags = player.getCardToTag().get(ResearchGrant.class);

        //put tag
        for (int i = 0; i < cardTags.size(); i++) {
            if (cardTags.get(i) == Tag.DYNAMIC) {
                cardTags.set(i, tag);
                break;
            }
        }

        final MarsContext marsContext = marsContextProvider.provide(game, player);

        CardService cardService = marsContext.getCardService();


        List<Card> playedCards = player.getPlayed().getCards().stream().map(cardService::getCard).collect(Collectors.toList());

        Map<Integer, List<Integer>> inputParameters = new HashMap<>();
        inputParameters.put(InputFlag.TAG_INPUT.getId(), List.of(tag.ordinal()));
        inputParameters.putAll(aiCardBuildParamsService.getActiveInputFromCards(game, player, playedCards, card, Map.of(InputFlag.TAG_INPUT.getId(), List.of(tag.ordinal())), false));

        playedCards
                .stream()
                .filter(Card::onBuiltEffectApplicableToOther)
                .forEach(c -> c.postProjectBuiltEffect(marsContext, new DummyCard(), inputParameters));

        return deepNetwork.testState(game, player, network);
    }



}
