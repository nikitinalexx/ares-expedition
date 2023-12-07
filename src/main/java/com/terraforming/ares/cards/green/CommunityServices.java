package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class CommunityServices implements ExperimentExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public CommunityServices(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Community Services")
                .description("During the production phase, this produces 1 me per Card without tag, including this.")
                .cardAction(CardAction.ME_NO_TAG_INCOME)
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        int cardsWithoutTags = cardService.countCardsWithoutTags(player);

        player.setMc(player.getMc() + cardsWithoutTags);
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
        final Player player = marsContext.getPlayer();

        if (CollectionUtils.isEmpty(project.getTags())) {
            player.setMcIncome(player.getMcIncome() + 1);
        }
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        final Player player = marsContext.getPlayer();

        int cardsWithoutTags = marsContext.getCardService().countCardsWithoutTags(player);

        player.setMcIncome(player.getMcIncome() + cardsWithoutTags + 1);

        return null;
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public int getPrice() {
        return 13;
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }
}
