package com.terraforming.ares.services.policyai.service;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.blue.BacterialAggregates;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.AiDiscoveryDecisionService;
import com.terraforming.ares.services.ai.network2.Network2PaymentService;
import com.terraforming.ares.services.ai.turnProcessors.AiUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.terraforming.ares.model.InputFlag.DECOMPOSERS_TAKE_MICROBE;

@Component
@RequiredArgsConstructor
public class PolicyCardBuildParamsService {
    private static final int PLANT_FLAG = -1;
    private final AiDiscoveryDecisionService aiDiscoveryDecisionService;
    private final CardService cardService;
    private final AiUtility aiUtility;
    private final Network2PaymentService network2PaymentService;
    private final CardValidationService cardValidationService;

    public static final class TagEffectContext {
        boolean marsUniversity;
        boolean viralEnhancers;
        boolean decomposers;
    }

    public List<Card> getMinimalBuildParamVariations(MarsGame game, Player player, List<Card> cards) {
        TagEffectContext tagEffectContext = new TagEffectContext();
        List<Card> playedCards = new ArrayList<>();

        for (Integer playedCardId : player.getPlayed().getCards()) {
            Card playedCard = cardService.getCard(playedCardId);
            CardAction playedCardAction = playedCard.getCardMetadata().getCardAction();
            if (playedCardAction == CardAction.MARS_UNIVERSITY) {
                tagEffectContext.marsUniversity = true;
            } else if (playedCardAction == CardAction.VIRAL_ENHANCERS) {
                tagEffectContext.viralEnhancers = true;
            } else if (playedCardAction == CardAction.DECOMPOSERS) {
                tagEffectContext.decomposers = true;
            }
            playedCards.add(playedCard);
        }


        List<Card> result = new ArrayList<>();

        for (Card card : cards) {

            List<Map<Integer, List<Integer>>> inputParamsVariations = getInputParamsVariations(player, card, tagEffectContext, playedCards);

            // если null — проблема с инпутом, скипаем карту
            if (inputParamsVariations == null) {
                continue;
            }

            boolean cardIsValid = false;

            for (Map<Integer, List<Integer>> inputParamsVariation : inputParamsVariations) {

                List<List<Payment>> allPossibleCardPayments = network2PaymentService.getAllPossibleCardPayments(
                        game,
                        player,
                        card,
                        inputParamsVariation
                );

                for (List<Payment> possiblePayments : allPossibleCardPayments) {

                    if (cardValidationService.validateCard(player, game, card.getId(), possiblePayments, inputParamsVariation) == null) {

                        result.add(card);

                        cardIsValid = true;
                        break;
                    }
                }

                if (cardIsValid) {
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Creates minimal input to be able to pass validator
     */
    public List<Map<Integer, List<Integer>>> getInputParamsVariations(Player player, Card card, TagEffectContext tagEffectContext, List<Card> playedCards) {
        CardMetadata cardMetadata = card.getCardMetadata();
        CardAction cardAction = cardMetadata.getCardAction();

        if (cardAction == CardAction.CRYOGENIC_SHIPMENT) {
            Map<Integer, List<Integer>> input = new HashMap<>();
            input.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(Constants.PHASE_1_UPGRADE_DISCOUNT));
            input.put(InputFlag.CRYOGENIC_SHIPMENT_PUT_RESOURCE.getId(), List.of(InputFlag.SKIP_ACTION.getId()));
            input.putAll(getActiveInputFromCards(player, tagEffectContext, card, input));
            return List.of(input);
        }
        if (cardAction == CardAction.UPDATE_PHASE_CARD_TWICE) {
            return List.of(
                    Map.of(
                            InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(Constants.PHASE_1_UPGRADE_DISCOUNT, Constants.PHASE_2_UPGRADE_PROJECT_AND_MC)
                    )
            );
        }
        if (cardAction == CardAction.TOPOGRAPHIC_MAPPING) {
            List<Map<Integer, List<Integer>>> variations = new ArrayList<>();
            for (Tag tag : Tag.values()) {
                if (tag == Tag.DYNAMIC) {
                    continue;
                }
                Map<Integer, List<Integer>> tagInput = new HashMap<>();
                tagInput.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(Constants.PHASE_1_UPGRADE_DISCOUNT));
                tagInput.put(InputFlag.TAG_INPUT.getId(), List.of(tag.ordinal()));
                tagInput.putAll(getActiveInputFromCards(player, tagEffectContext, card, tagInput));
                variations.add(tagInput);
            }
            return variations;
        }
        if (cardAction == CardAction.UPDATE_PHASE_2_CARD) {
            return List.of(Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(Constants.PHASE_2_UPGRADE_PROJECT_AND_MC)));
        }
        if (cardAction == CardAction.CHOOSE_TAG) {
            List<Map<Integer, List<Integer>>> variations = new ArrayList<>();
            for (Tag tag : Tag.values()) {
                if (tag == Tag.DYNAMIC) {
                    continue;
                }
                Map<Integer, List<Integer>> tagInput = new HashMap<>();
                tagInput.put(InputFlag.TAG_INPUT.getId(), List.of(tag.ordinal()));
                tagInput.putAll(getActiveInputFromCards(player, tagEffectContext, card, tagInput));
                variations.add(tagInput);
            }
            return variations;
        }
        if (cardAction == CardAction.UPDATE_PHASE_1_CARD) {
            return List.of(Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(Constants.PHASE_1_UPGRADE_DISCOUNT)));
        }
        if (cardAction == CardAction.COMMUNICATIONS_STREAMLINING) {
            Map<Integer, List<Integer>> input = new HashMap<>();
            input.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(Constants.PHASE_3_UPGRADE_REVEAL_CARDS));
            input.putAll(getActiveInputFromCards(player, tagEffectContext, card, input));
            return List.of(input);
        }
        if (cardAction == CardAction.UPDATE_PHASE_4_CARD) {
            Map<Integer, List<Integer>> input = new HashMap<>();
            input.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(Constants.PHASE_4_UPGRADE_EXTRA_MC));
            input.putAll(getActiveInputFromCards(player, tagEffectContext, card, input));
            return List.of(input);
        }
        if (cardAction == CardAction.BIOMEDICAL_IMPORTS || cardAction != null && AiConstants.CARD_ACTIONS_WITH_PHASE_UPGRADE_EFFECT.contains(cardAction)) {
            Map<Integer, List<Integer>> input = new HashMap<>();
            input.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(Constants.PHASE_1_UPGRADE_DISCOUNT));
            input.putAll(getActiveInputFromCards(player, tagEffectContext, card, input));
            return List.of(input);
        }
        if (cardAction == CardAction.ASTROFARM) {
            return List.of(Map.of(InputFlag.ASTROFARM_PUT_RESOURCE.getId(), List.of(InputFlag.SKIP_ACTION.getId())));
        }
        if (cardAction == CardAction.EOS_CHASMA) {
            Map<Integer, List<Integer>> input = new HashMap<>();
            input.put(InputFlag.EOS_CHASMA_PUT_RESOURCE.getId(), List.of(InputFlag.SKIP_ACTION.getId()));
            input.putAll(getActiveInputFromCards(player, tagEffectContext, card, input));
            return List.of(input);
        }
        if (cardAction == CardAction.IMPORTED_HYDROGEN) {
            return List.of(Map.of(InputFlag.IMPORTED_HYDROGEN_PICK_PLANT.getId(), List.of()));
        }
        if (cardAction == CardAction.IMPORTED_NITROGEN) {
            return List.of(
                    Map.of(
                            InputFlag.IMPORTED_NITROGEN_ADD_ANIMALS.getId(), List.of(InputFlag.SKIP_ACTION.getId()),
                            InputFlag.IMPORTED_NITROGEN_ADD_MICROBES.getId(), List.of(InputFlag.SKIP_ACTION.getId())
                    )

            );
        }
        if (cardAction == CardAction.LARGE_CONVOY) {
            return List.of(
                    Map.of(InputFlag.LARGE_CONVOY_PICK_PLANT.getId(), List.of())
            );
        }
        if (cardAction == CardAction.LOCAL_HEAT_TRAPPING) {
            return List.of(
                    Map.of(InputFlag.LOCAL_HEAT_TRAPPING_PUT_RESOURCE.getId(), List.of(InputFlag.SKIP_ACTION.getId()))
            );
        }

        if (!cardMetadata.getResourcesOnBuild().isEmpty() && cardMetadata.getResourcesOnBuild().getFirst().getType() == CardCollectableResource.ANY) {
            List<Card> cardsToPickFrom = aiUtility.getPlayerCardsWithResource(player,
                    Set.of(CardCollectableResource.MICROBE, CardCollectableResource.ANIMAL, CardCollectableResource.SCIENCE));
            if (cardsToPickFrom.isEmpty()) {
                return null;
            }
            if (cardsToPickFrom.size() == 1 && cardsToPickFrom.getFirst().getClass() == BacterialAggregates.class && player.getCardResourcesCount().getOrDefault(BacterialAggregates.class, 0) > AiConstants.BACTERIAL_AGGREGATES_COUNT_FOR_MICROBE_PUT) {
                return null;
            }
            return List.of(
                    Map.of(
                            InputFlag.CEOS_FAVORITE_PUT_RESOURCES.getId(), List.of(cardsToPickFrom.getFirst().getId())
                    )
            );
        }


        if (cardAction == CardAction.SYNTHETIC_CATASTROPHE) {
            List<Card> playedRedCards = playedCards.stream()
                    .filter(c -> c.getColor() == CardColor.RED)
                    .toList();
            if (playedRedCards.isEmpty()) {
                return null;
            }
            return List.of(Map.of(InputFlag.SYNTHETIC_CATASTROPHE_CARD.getId(), List.of(playedRedCards.getFirst().getId())));
        }

        return List.of(Map.of());
    }

    public Map<Integer, List<Integer>> getActiveInputFromCards(Player player, TagEffectContext tagEffectContext,
                                                               Card card, Map<Integer, List<Integer>> input) {
        Map<Integer, List<Integer>> result = new HashMap<>();
        result.putAll(getDecomposersInputIfApplicable(player, tagEffectContext, card, input));
        result.putAll(getViralEnhancersInputIfApplicable(tagEffectContext, card, input));
        result.putAll(getMarsUniversityInputIfApplicable(player, tagEffectContext, card, input));
        return result;
    }

    private Map<Integer, List<Integer>> getMarsUniversityInputIfApplicable(Player player, TagEffectContext tagEffectContext,
                                                                           Card card, Map<Integer, List<Integer>> input) {
        if (!tagEffectContext.marsUniversity) {
            return Map.of();
        }

        int scienceTagsCount = cardService.countCardTags(card, Set.of(Tag.SCIENCE), input);
        if (scienceTagsCount == 0) {
            return Map.of();
        }

        return Map.of(InputFlag.MARS_UNIVERSITY_CARD.getId(), List.of(InputFlag.SKIP_ACTION.getId()));
    }

    private Map<Integer, List<Integer>> getDecomposersInputIfApplicable(Player player, TagEffectContext tagEffectContext,
                                                                        Card card, Map<Integer, List<Integer>> input) {
        if (!tagEffectContext.decomposers) {
            return Map.of();
        }
        int tagsCount = cardService.countCardTags(card, Set.of(Tag.ANIMAL, Tag.MICROBE, Tag.PLANT), input);
        if (tagsCount == 0) {
            return Map.of();
        }

        return Map.of(DECOMPOSERS_TAKE_MICROBE.getId(), List.of(tagsCount));
    }

    private Map<Integer, List<Integer>> getViralEnhancersInputIfApplicable(TagEffectContext tagEffectContext, Card card,
                                                                           Map<Integer, List<Integer>> input) {
        if (!tagEffectContext.viralEnhancers) {
            return Map.of();
        }
        int tagsCount = cardService.countCardTags(card, Set.of(Tag.ANIMAL, Tag.MICROBE, Tag.PLANT), input);
        if (tagsCount == 0) {
            return Map.of();
        }

        return Map.of(InputFlag.VIRAL_ENHANCERS_TAKE_PLANT.getId(), List.of(tagsCount));
    }

}