package com.terraforming.ares.cards.blue;

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
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class InterplanetaryConference implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public InterplanetaryConference(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Interplanetary Conference")
                .description("When you play an Earth or Jupiter tag, excluding this, you pay 3 MC less and draw a card.")
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(SpecialEffect.INTERPLANETARY_CONFERENCE);
    }

    @Override
    public void onProjectBuiltEffect(CardService cardService, MarsGame game, Player player, ProjectCard project, Map<Integer, List<Integer>> inputParams) {
        int cardsToGiveCount = 0;

        if (project.getTags().contains(Tag.EARTH)) {
            cardsToGiveCount++;
        }

        if (project.getTags().contains(Tag.JUPITER)) {
            cardsToGiveCount++;
        }

        if (cardsToGiveCount == 0) {
            return;
        }

        for (Integer card : game.dealCards(cardsToGiveCount)) {
            player.getHand().addCard(card);
        }
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EARTH);
    }

    @Override
    public int getPrice() {
        return 6;
    }
}
