package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class PowerGrid implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public PowerGrid(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Power Grid")
                .description("During the production phase, this produces 1 МС per Energy you have, including this.")
                .cardAction(CardAction.MC_ENERGY_INCOME)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void postProjectBuiltEffect(CardService cardService, MarsGame game, Player player, Card project, Map<Integer, List<Integer>> inputParams) {
        int energyTagsCount = (int) project.getTags().stream()
                .filter(Tag.ENERGY::equals)
                .count();

        player.setMcIncome(player.getMcIncome() + energyTagsCount);
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        int energyTagsCount = (int) player.getPlayed().getCards().stream().map(marsContext.getCardService()::getCard)
                .flatMap(card -> card.getTags().stream())
                .filter(Tag.ENERGY::equals)
                .count();

        player.setMcIncome(player.getMcIncome() + energyTagsCount + 1);

        return null;
    }

    @Override
    public void revertPlayedTags(CardService cardService, List<Tag> tags, Player player) {
        int energyTagCount = (int) tags.stream().filter(Tag.ENERGY::equals).count();
        player.setMcIncome(player.getMcIncome() - energyTagCount);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING, Tag.ENERGY);
    }

    @Override
    public int getPrice() {
        return 8;
    }
}
