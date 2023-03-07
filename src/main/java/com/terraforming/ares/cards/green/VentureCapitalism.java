package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class VentureCapitalism implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public VentureCapitalism(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Venture Capitalism")
                .description("During the production phase, this produces 1 МС per Event you have.")
                .cardAction(CardAction.MC_EVENT_INCOME)
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        int eventTagCount = cardService.countPlayedTags(player, Set.of(Tag.EVENT));

        player.setMc(player.getMc() + eventTagCount);
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
        int eventTagCount = marsContext.getCardService().countCardTags(project, Set.of(Tag.EVENT), inputParams);

        final Player player = marsContext.getPlayer();

        player.setMcIncome(player.getMcIncome() + eventTagCount);
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        final Player player = marsContext.getPlayer();

        int eventTagCount = marsContext.getCardService().countPlayedTags(player, Set.of(Tag.EVENT));

        player.setMcIncome(player.getMcIncome() + eventTagCount);

        return null;
    }

    @Override
    public void revertPlayedTags(CardService cardService, Card card, Player player) {
        int eventTagCount = cardService.countCardTagsWithDynamic(card, player, Set.of(Tag.EVENT));

        player.setMcIncome(player.getMcIncome() - eventTagCount);
    }

    @Override
    public void revertCardIncome(MarsContext marsContext) {
        final Player player = marsContext.getPlayer();
        int eventTagCount = marsContext.getCardService().countPlayedTags(player, Set.of(Tag.EVENT));
        player.setMcIncome(player.getMcIncome() - eventTagCount);
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public int getPrice() {
        return 11;
    }
}
