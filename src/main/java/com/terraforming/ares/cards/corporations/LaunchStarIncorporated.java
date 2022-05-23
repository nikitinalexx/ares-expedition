package com.terraforming.ares.cards.corporations;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.terraforming.ares.model.SpecialEffect.LAUNCH_STAR_DISCOUNT;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@RequiredArgsConstructor
@Getter
public class LaunchStarIncorporated implements CorporationCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public LaunchStarIncorporated(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("LaunchStar Incorporated")
                .description("36 Mc. Draw cards until Blue is found. Take it and discard other. Blue cards cost 3 MC less.")
                .cardAction(CardAction.LAUNCH_STAR_CORPORATION)
                .build();
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(LAUNCH_STAR_DISCOUNT);
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setMc(36);

        while (true) {
            List<Integer> cards = marsContext.getCardService().dealCards(marsContext.getGame(), 1);
            if (CollectionUtils.isEmpty(cards)) {
                break;
            }
            Integer card = cards.get(0);

            if (marsContext.getCardService().getCard(card).getColor() == CardColor.BLUE) {
                player.getHand().addCard(card);
                break;
            }
        }

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.SCIENCE);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }
}
