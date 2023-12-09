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
public class MicrobiologyPatents implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public MicrobiologyPatents(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Microbiology Patents")
                .description("During the production phase, this produces 1 MC per Microbe tag you have, including this.")
                .cardAction(CardAction.MC_MICROBE_INCOME)
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        int microbeTagCount = cardService.countPlayedTags(player, Set.of(Tag.MICROBE));

        player.setMc(player.getMc() + microbeTagCount);
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
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
        int microbeTags = marsContext.getCardService().countCardTags(project, Set.of(Tag.MICROBE), inputParams);

        final Player player = marsContext.getPlayer();

        player.setMcIncome(player.getMcIncome() + microbeTags);
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        int microbeTagCount = marsContext.getCardService().countPlayedTags(marsContext.getPlayer(), Set.of(Tag.MICROBE));

        marsContext.getPlayer().setMcIncome(marsContext.getPlayer().getMcIncome() + microbeTagCount + 1);

        return null;
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.MICROBE);
    }

    @Override
    public int getPrice() {
        return 6;
    }
}
