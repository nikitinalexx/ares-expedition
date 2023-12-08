package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class InterplanetaryTrade implements ExperimentExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public InterplanetaryTrade(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Interplanetary Trade")
                .description("During the production phase, this produces 1 MC per Every unique tag you have.")
                .cardAction(CardAction.MC_UNIQUE_TAG_INCOME)
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        player.setMc(player.getMc() + cardService.countUniquePlayedTags(player));
    }

    @Override
    public boolean canPayAgain() {
        return true;
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> input) {
        final Player player = marsContext.getPlayer();

        CardService cardService = marsContext.getCardService();

        Map<Tag, Long> playedTagsMap = cardService.countPlayedTagsAsMap(player);

        Set<Tag> newTags = new HashSet<>(project.getTags());

        int incomeAdjustment = 0;
        if (input != null && !input.containsKey(InputFlag.RESEARCH_GRANT.getId()) && input.containsKey(InputFlag.TAG_INPUT.getId())) {
            List<Integer> tagInput = input.get(InputFlag.TAG_INPUT.getId());

            Tag playedTag = Tag.byIndex(tagInput.get(0));
            newTags.add(playedTag);
        } else if (input != null && input.containsKey(InputFlag.RESEARCH_GRANT.getId())) {
            List<Integer> tagInput = input.get(InputFlag.TAG_INPUT.getId());
            Tag playedTag = Tag.byIndex(tagInput.get(0));

            if (playedTagsMap.get(playedTag) == 1L) {
                incomeAdjustment++;
            }
        }

        for (Tag newTag : newTags) {
            if (newTag != Tag.DYNAMIC && (!playedTagsMap.containsKey(newTag) || playedTagsMap.get(newTag) <= 0)) {
                incomeAdjustment++;
            }
        }

        player.setMcIncome(player.getMcIncome() + incomeAdjustment);
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        CardService cardService = marsContext.getCardService();
        Player player = marsContext.getPlayer();

        Map<Tag, Long> playedTagsMap = cardService.countPlayedTagsAsMap(player);

        int extraIncome = cardService.countUniquePlayedTags(player);
        if (playedTagsMap.getOrDefault(Tag.SPACE, 0L) <= 0) {
            extraIncome++;
        }

        player.setMcIncome(player.getMcIncome() + extraIncome);

        return null;
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE);
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public int getPrice() {
        return 27;
    }
}
