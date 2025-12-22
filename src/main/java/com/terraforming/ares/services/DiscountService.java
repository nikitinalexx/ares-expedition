package com.terraforming.ares.services;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 27.02.2023
 */
@Service
@RequiredArgsConstructor
public class DiscountService {
    private final CardService cardService;
    private final SpecialEffectsService specialEffectsService;
    private final CrisisDetrimentService crisisDetrimentService;

    public int getDiscount(MarsGame game, Card card, Player player, Map<Integer, List<Integer>> inputParameters) {
        int discount = 0;

        List<Tag> tags = cardService.getCardTags(card, inputParameters);
        Set<SpecialEffect> playerSpecialEffects = specialEffectsService.getPlayerSpecialEffects(player);

        boolean playerOwnsAdvancedAlloys = playerSpecialEffects.contains(SpecialEffect.ADVANCED_ALLOYS);
        boolean playerOwnsPhobolog = playerSpecialEffects.contains(SpecialEffect.PHOBOLOG);

        if (tags.contains(Tag.BUILDING) && player.getSteelIncome() != 0) {
            discount += player.getSteelIncome() * (2 + (playerOwnsAdvancedAlloys ? 1 : 0));
        }

        if (tags.contains(Tag.SPACE) && player.getTitaniumIncome() != 0) {
            discount += player.getTitaniumIncome() * (3 + (playerOwnsAdvancedAlloys ? 1 : 0) + (playerOwnsPhobolog ? 1 : 0));
        }

        if (playerSpecialEffects.contains(SpecialEffect.EARTH_CATAPULT_DISCOUNT_2)) {
            discount += 2;
        }

        if (playerSpecialEffects.contains(SpecialEffect.ORBITAL_OUTPOST_DISCOUNT)
                && tags.size() <= 1) {
            discount += 3;
        }

        if (tags.contains(Tag.ENERGY) &&
                playerSpecialEffects.contains(SpecialEffect.ENERGY_SUBSIDIES_DISCOUNT_4)) {
            discount += 4;
        }

        if ((tags.contains(Tag.ANIMAL) || tags.contains(Tag.MICROBE) || tags.contains(Tag.PLANT)) &&
                playerSpecialEffects.contains(SpecialEffect.GMO_CONTRACT_DISCOUNT_3)) {
            discount += 3;
        }

        if (playerSpecialEffects.contains(SpecialEffect.INTERPLANETARY_CONFERENCE)) {
            if (tags.contains(Tag.EARTH)) {
                discount += 3;
            }
            if (tags.contains(Tag.JUPITER)) {
                discount += 3;
            }
        }

        if (tags.contains(Tag.PLANT) && !tags.contains(Tag.BUILDING) && playerSpecialEffects.contains(SpecialEffect.MARTIAN_LUMBER)) {
            discount += player.getSteelIncome() * 2;
        }

        if (tags.contains(Tag.EVENT) &&
                playerSpecialEffects.contains(SpecialEffect.MEDIA_GROUP)) {
            discount += 5;
        }

        if (playerSpecialEffects.contains(SpecialEffect.RESEARCH_OUTPOST_DISCOUNT_1)) {
            discount += 1;
        }

        if (playerSpecialEffects.contains(SpecialEffect.HOHMANN_DISCOUNT_1)) {
            discount += 1;
        }

        if (playerSpecialEffects.contains(SpecialEffect.DEV_TECHS_DISCOUNT) && card.getColor() == CardColor.GREEN) {
            discount += 2;
        }

        if (playerSpecialEffects.contains(SpecialEffect.LAUNCH_STAR_DISCOUNT) && card.getColor() == CardColor.BLUE) {
            discount += 3;
        }

        if (playerSpecialEffects.contains(SpecialEffect.TORGATE_ENERGY_DISCOUNT) && tags.contains(Tag.ENERGY)) {
            discount += 3;
        }

        if (playerSpecialEffects.contains(SpecialEffect.TERACTOR_EARTH_DISCOUNT) && tags.contains(Tag.EARTH)) {
            discount += 3;
        }

        if (playerSpecialEffects.contains(SpecialEffect.INTERPLANETARY_CINEMATICS_DISCOUNT) && tags.contains(Tag.EVENT)) {
            discount += 2;
        }

        if (playerSpecialEffects.contains(SpecialEffect.CREDICOR_DISCOUNT) && card.getPrice() >= 20) {
            discount += 4;
        }

        if (crisisDetrimentService.detrimentForPriceIncrease1(game)) {
            discount -= 1;
        }

        if (crisisDetrimentService.detrimentForPriceIncrease3(game)) {
            discount -= 3;
        }

        return discount;
    }

}
