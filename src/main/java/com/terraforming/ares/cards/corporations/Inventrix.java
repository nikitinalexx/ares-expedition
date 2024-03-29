package com.terraforming.ares.cards.corporations;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@RequiredArgsConstructor
@Getter
public class Inventrix implements CorporationCard {
    private final int id;
    private final CardMetadata cardMetadata;

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(SpecialEffect.AMPLIFY_GLOBAL_REQUIREMENT);
    }

    public Inventrix(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Inventrix")
                .description("33 Mc. Take 3 cards. When playing a card with requirements, you may consider the oxygen or temperature one color higher or lower. This cannot be modified futher by other effects.")
                .cardAction(CardAction.INVENTRIX_CORPORATION)
                .build();
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setMc(33);
        marsContext.dealCards(3);
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

    @Override
    public int getPrice() {
        return 33;
    }

    @Override
    public boolean isSupportedByExpansionSet(Set<Expansion> expansions) {
        return CollectionUtils.isEmpty(expansions) || !expansions.contains(Expansion.CRYSIS);
    }

}
