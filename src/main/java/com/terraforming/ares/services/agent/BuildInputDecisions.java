package com.terraforming.ares.services.agent;

import com.terraforming.ares.cards.blue.Decomposers;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.AiConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * On-built inputs for a card that was already chosen. Filled as a sequence of {@link DecisionType#BUILD_INPUT}
 * decisions (one atomic answer each): Viral Enhancers, Mars University and Decomposers multiply past the 255-candidate
 * limit when combined with phase/tag choices, so a joint menu is impossible.
 * <p>
 * Order: dynamic tag → phase upgrade(s) → card-native resource/biomedical/cargo choices → triggered blues that depend
 * on the tag (Decomposers, Viral Enhancers, Mars University). Payment comes after this map is complete, because discounts
 * can depend on {@link InputFlag#TAG_INPUT}.
 */
@Service
@RequiredArgsConstructor
public class BuildInputDecisions {
    public static final int KIND_PHASE = 0;
    public static final int KIND_TAG = 1;
    public static final int KIND_TARGET = 2;
    public static final int KIND_PLANT = 3;
    public static final int KIND_SKIP = 4;
    public static final int KIND_OXYGEN = 5;
    public static final int KIND_DECOMP_MICROBE = 6;
    public static final int KIND_DECOMP_CARD = 7;
    public static final int KIND_CARGO_HEAT = 8;
    public static final int KIND_CARGO_PLANT = 9;
    public static final int KIND_MU_STOP = 10;
    public static final int KINDS = 11;

    public static final int ID_PLANT = 0xFFFD;
    public static final int ID_SKIP = 0xFFFE;
    public static final int ID_OXYGEN = 0xFFFC;
    public static final int ID_DECOMP_MICROBE = 0xFFFB;
    public static final int ID_DECOMP_CARD = 0xFFFA;
    public static final int ID_CARGO_HEAT = 0xFFF9;
    public static final int ID_CARGO_PLANT = 0xFFF8;
    public static final int ID_MU_STOP = 0xFFF7;

    public static final int PHASE_UPGRADES = 10;
    public static final int MAX_CANDIDATES = 200;
    public static final int CANDIDATE_FEATURES = KINDS + CardFeatures.LENGTH + PHASE_UPGRADES + CardFeatures.TAGS;
    public static final String OBSERVATION_LAYOUT = ObservationBuilder.LAYOUT
            + "; candidate=[kind(" + KINDS + "), cardFeatures(" + CardFeatures.LENGTH + "), phase(" + PHASE_UPGRADES
            + "), tag(" + CardFeatures.TAGS + ")]; sequential slots for on-built InputFlags";

    private final ObservationBuilder observationBuilder;
    private final CardService cardService;

    public static DecisionFile.Schema schema() {
        return new DecisionFile.Schema(DecisionType.BUILD_INPUT, ObservationBuilder.size(),
                DecisionType.BUILD_INPUT.maxCandidates(), CANDIDATE_FEATURES, OBSERVATION_LAYOUT);
    }

    /**
     * Fills every on-built slot, recording one decision per slot. Empty map when the card needs no input.
     */
    public Map<Integer, List<Integer>> fill(MarsGame game, Player player, Card card,
                                            ToIntFunction<Decision> choose) {
        Map<Integer, List<Integer>> input = new HashMap<>();
        while (true) {
            Optional<Slot> slot = nextSlot(game, player, card, input);
            if (slot.isEmpty()) {
                return input;
            }
            List<BuildInputChoice> choices = slot.get().choices();
            if (choices.size() == 1 && choices.getFirst().kind() == KIND_MU_STOP) {
                // Hand has nothing left to discard; finish Mars University without a recorded decision.
                apply(input, choices.getFirst(), card);
                continue;
            }
            Decision decision = decision(game, player, choices);
            int index = choose.applyAsInt(decision);
            apply(input, choices.get(index), card);
        }
    }

    public Decision decision(MarsGame game, Player player, List<BuildInputChoice> choices) {
        List<Candidate> candidates = new ArrayList<>(choices.size());
        for (BuildInputChoice choice : choices) {
            candidates.add(new Candidate(choice.id(), features(choice)));
        }
        if (candidates.size() > MAX_CANDIDATES) {
            throw new IllegalStateException("Build input menu has " + candidates.size() + " options");
        }
        return new Decision(DecisionType.BUILD_INPUT, player.getUuid(),
                observationBuilder.build(game, player), candidates);
    }

    public Optional<Slot> nextSlot(MarsGame game, Player player, Card card, Map<Integer, List<Integer>> input) {
        CardAction action = card.getCardMetadata() == null ? null : card.getCardMetadata().getCardAction();

        if (needsTag(action) && !input.containsKey(InputFlag.TAG_INPUT.getId())) {
            return Optional.of(new Slot(tagChoices()));
        }
        if (needsTwoPhases(action)) {
            List<Integer> upgrades = input.get(InputFlag.PHASE_UPGRADE_CARD.getId());
            if (upgrades == null || upgrades.isEmpty()) {
                return Optional.of(new Slot(phaseChoices(null)));
            }
            if (upgrades.size() == 1) {
                return Optional.of(new Slot(phaseChoices(upgrades.getFirst() / 2)));
            }
        } else if (needsSinglePhase(action) && !input.containsKey(InputFlag.PHASE_UPGRADE_CARD.getId())
                && action != CardAction.BIOMEDICAL_IMPORTS) {
            return Optional.of(new Slot(phaseChoicesFor(action)));
        }
        if (action == CardAction.BIOMEDICAL_IMPORTS
                && !input.containsKey(InputFlag.BIOMEDICAL_IMPORTS_RAISE_OXYGEN.getId())
                && !input.containsKey(InputFlag.BIOMEDICAL_IMPORTS_UPGRADE_PHASE.getId())) {
            return Optional.of(new Slot(biomedicalChoices(game)));
        }
        if (action == CardAction.CRYOGENIC_SHIPMENT
                && !input.containsKey(InputFlag.CRYOGENIC_SHIPMENT_PUT_RESOURCE.getId())) {
            return Optional.of(new Slot(resourceTargetChoices(player, card,
                    InputFlag.CRYOGENIC_SHIPMENT_PUT_RESOURCE.getId(),
                    Set.of(CardCollectableResource.MICROBE, CardCollectableResource.ANIMAL), true)));
        }
        if (action == CardAction.ASTROFARM && !input.containsKey(InputFlag.ASTROFARM_PUT_RESOURCE.getId())) {
            return Optional.of(new Slot(resourceTargetChoices(player, card,
                    InputFlag.ASTROFARM_PUT_RESOURCE.getId(), Set.of(CardCollectableResource.MICROBE), true)));
        }
        if (action == CardAction.EOS_CHASMA && !input.containsKey(InputFlag.EOS_CHASMA_PUT_RESOURCE.getId())) {
            return Optional.of(new Slot(resourceTargetChoices(player, card,
                    InputFlag.EOS_CHASMA_PUT_RESOURCE.getId(), Set.of(CardCollectableResource.ANIMAL), true)));
        }
        if (action == CardAction.IMPORTED_HYDROGEN
                && !input.containsKey(InputFlag.IMPORTED_HYDROGEN_PICK_PLANT.getId())
                && !input.containsKey(InputFlag.IMPORTED_HYDROGEN_PUT_RESOURCE.getId())) {
            return Optional.of(new Slot(importedHydrogenChoices(player, card)));
        }
        if (action == CardAction.IMPORTED_NITROGEN) {
            if (!input.containsKey(InputFlag.IMPORTED_NITROGEN_ADD_ANIMALS.getId())) {
                return Optional.of(new Slot(resourceTargetChoices(player, card,
                        InputFlag.IMPORTED_NITROGEN_ADD_ANIMALS.getId(),
                        Set.of(CardCollectableResource.ANIMAL), true)));
            }
            if (!input.containsKey(InputFlag.IMPORTED_NITROGEN_ADD_MICROBES.getId())) {
                return Optional.of(new Slot(resourceTargetChoices(player, card,
                        InputFlag.IMPORTED_NITROGEN_ADD_MICROBES.getId(),
                        Set.of(CardCollectableResource.MICROBE), true)));
            }
        }
        if (action == CardAction.LARGE_CONVOY
                && !input.containsKey(InputFlag.LARGE_CONVOY_PICK_PLANT.getId())
                && !input.containsKey(InputFlag.LARGE_CONVOY_ADD_ANIMAL.getId())) {
            return Optional.of(new Slot(largeConvoyChoices(player, card)));
        }
        if (action == CardAction.LOCAL_HEAT_TRAPPING
                && !input.containsKey(InputFlag.LOCAL_HEAT_TRAPPING_PUT_RESOURCE.getId())) {
            return Optional.of(new Slot(resourceTargetChoices(player, card,
                    InputFlag.LOCAL_HEAT_TRAPPING_PUT_RESOURCE.getId(),
                    Set.of(CardCollectableResource.MICROBE, CardCollectableResource.ANIMAL), true)));
        }
        if ((action == CardAction.CONTROLLED_BLOOM || action == CardAction.IMPORTED_NUTRIENTS
                || action == CardAction.CYANOBACTERIA)
                && !input.containsKey(InputFlag.ADD_MICROBE.getId())) {
            return Optional.of(new Slot(resourceTargetChoices(player, card,
                    InputFlag.ADD_MICROBE.getId(), Set.of(CardCollectableResource.MICROBE), true)));
        }
        if (isCeosFavorite(card) && !input.containsKey(InputFlag.CEOS_FAVORITE_PUT_RESOURCES.getId())) {
            List<BuildInputChoice> targets = resourceTargetChoices(player, card,
                    InputFlag.CEOS_FAVORITE_PUT_RESOURCES.getId(),
                    Set.of(CardCollectableResource.ANIMAL, CardCollectableResource.MICROBE, CardCollectableResource.SCIENCE),
                    false);
            if (targets.isEmpty()) {
                throw new IllegalStateException("CEO's Favorite Project has no resource card to boost");
            }
            return Optional.of(new Slot(targets));
        }
        if (action == CardAction.SYNTHETIC_CATASTROPHE
                && !input.containsKey(InputFlag.SYNTHETIC_CATASTROPHE_CARD.getId())) {
            List<BuildInputChoice> reds = syntheticCatastropheChoices(player);
            if (reds.isEmpty()) {
                throw new IllegalStateException("Synthetic Catastrophe has no red card to target");
            }
            return Optional.of(new Slot(reds));
        }
        if (needsCargoChoice(player, card) && !input.containsKey(InputFlag.CARGO_SHIPS.getId())) {
            return Optional.of(new Slot(List.of(
                    new BuildInputChoice(ID_CARGO_PLANT, KIND_CARGO_PLANT, InputFlag.CARGO_SHIPS.getId()),
                    new BuildInputChoice(ID_CARGO_HEAT, KIND_CARGO_HEAT, InputFlag.CARGO_SHIPS.getId())
            )));
        }

        Optional<Slot> decomposers = decomposersSlot(player, card, input);
        if (decomposers.isPresent()) {
            return decomposers;
        }
        Optional<Slot> viral = viralEnhancersSlot(player, card, input);
        if (viral.isPresent()) {
            return viral;
        }
        return marsUniversitySlot(player, card, input);
    }

    void apply(Map<Integer, List<Integer>> input, BuildInputChoice choice, Card card) {
        switch (choice.kind()) {
            case KIND_PHASE -> {
                List<Integer> upgrades = new ArrayList<>(input.getOrDefault(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of()));
                upgrades.add(choice.id());
                input.put(InputFlag.PHASE_UPGRADE_CARD.getId(), upgrades);
                if (card.getCardMetadata() != null
                        && card.getCardMetadata().getCardAction() == CardAction.BIOMEDICAL_IMPORTS) {
                    input.put(InputFlag.BIOMEDICAL_IMPORTS_UPGRADE_PHASE.getId(), List.of(-1));
                }
            }
            case KIND_TAG -> input.put(InputFlag.TAG_INPUT.getId(), List.of(choice.id()));
            case KIND_TARGET -> {
                if (choice.flag() == InputFlag.VIRAL_ENHANCERS_PUT_RESOURCE.getId()
                        || choice.flag() == InputFlag.MARS_UNIVERSITY_CARD.getId()) {
                    List<Integer> list = new ArrayList<>(input.getOrDefault(choice.flag(), List.of()));
                    list.add(choice.id());
                    input.put(choice.flag(), list);
                } else {
                    input.put(choice.flag(), List.of(choice.id()));
                }
            }
            case KIND_PLANT -> {
                if (choice.flag() == InputFlag.IMPORTED_HYDROGEN_PICK_PLANT.getId()) {
                    input.put(InputFlag.IMPORTED_HYDROGEN_PICK_PLANT.getId(), List.of());
                } else if (choice.flag() == InputFlag.LARGE_CONVOY_PICK_PLANT.getId()) {
                    input.put(InputFlag.LARGE_CONVOY_PICK_PLANT.getId(), List.of());
                } else {
                    int plants = input.getOrDefault(InputFlag.VIRAL_ENHANCERS_TAKE_PLANT.getId(), List.of(0)).getFirst();
                    input.put(InputFlag.VIRAL_ENHANCERS_TAKE_PLANT.getId(), List.of(plants + 1));
                }
            }
            case KIND_SKIP -> input.put(choice.flag(), List.of(InputFlag.SKIP_ACTION.getId()));
            case KIND_OXYGEN -> input.put(InputFlag.BIOMEDICAL_IMPORTS_RAISE_OXYGEN.getId(), List.of(0));
            case KIND_DECOMP_MICROBE -> {
                int microbes = input.getOrDefault(InputFlag.DECOMPOSERS_TAKE_MICROBE.getId(), List.of(0)).getFirst();
                input.put(InputFlag.DECOMPOSERS_TAKE_MICROBE.getId(), List.of(microbes + 1));
            }
            case KIND_DECOMP_CARD -> {
                int cards = input.getOrDefault(InputFlag.DECOMPOSERS_TAKE_CARD.getId(), List.of(0)).getFirst();
                input.put(InputFlag.DECOMPOSERS_TAKE_CARD.getId(), List.of(cards + 1));
            }
            case KIND_CARGO_HEAT -> input.put(InputFlag.CARGO_SHIPS.getId(),
                    List.of(InputFlag.CARGO_SHIPS_HEAT.getId()));
            case KIND_CARGO_PLANT -> input.put(InputFlag.CARGO_SHIPS.getId(), List.of());
            case KIND_MU_STOP -> {
                if (!input.containsKey(InputFlag.MARS_UNIVERSITY_CARD.getId())) {
                    input.put(InputFlag.MARS_UNIVERSITY_CARD.getId(), List.of(InputFlag.SKIP_ACTION.getId()));
                }
                input.put(InputFlag.MARS_UNIVERSITY_DUMMY_INPUT.getId(), List.of(1));
            }
            default -> throw new IllegalStateException("Unknown build-input kind " + choice.kind());
        }
    }

    float[] features(BuildInputChoice choice) {
        float[] features = new float[CANDIDATE_FEATURES];
        features[choice.kind()] = 1f;
        int pos = KINDS;
        if (choice.kind() == KIND_TARGET && choice.id() < 10000) {
            Card card = cardService.getCard(choice.id());
            if (card != null) {
                float[] cardFeatures = CardFeatures.of(card, false);
                System.arraycopy(cardFeatures, 0, features, pos, CardFeatures.LENGTH);
            }
        }
        pos += CardFeatures.LENGTH;
        if (choice.kind() == KIND_PHASE && choice.id() >= 0 && choice.id() < PHASE_UPGRADES) {
            features[pos + choice.id()] = 1f;
        }
        pos += PHASE_UPGRADES;
        if (choice.kind() == KIND_TAG && choice.id() >= 0 && choice.id() < CardFeatures.TAGS) {
            features[pos + choice.id()] = 1f;
        }
        return features;
    }

    private List<BuildInputChoice> tagChoices() {
        List<BuildInputChoice> choices = new ArrayList<>();
        for (Tag tag : Tag.values()) {
            if (tag == Tag.DYNAMIC) {
                continue;
            }
            choices.add(new BuildInputChoice(tag.ordinal(), KIND_TAG, InputFlag.TAG_INPUT.getId()));
        }
        return choices;
    }

    private List<BuildInputChoice> phaseChoices(Integer forbiddenPhaseIndex) {
        List<BuildInputChoice> choices = new ArrayList<>();
        for (int i = 0; i < PHASE_UPGRADES; i++) {
            if (forbiddenPhaseIndex != null && i / 2 == forbiddenPhaseIndex) {
                continue;
            }
            choices.add(new BuildInputChoice(i, KIND_PHASE, InputFlag.PHASE_UPGRADE_CARD.getId()));
        }
        return choices;
    }

    private List<BuildInputChoice> phaseChoicesFor(CardAction action) {
        if (action == CardAction.UPDATE_PHASE_1_CARD) {
            return phasePair(Constants.BUILD_GREEN_PROJECTS_PHASE);
        }
        if (action == CardAction.UPDATE_PHASE_2_CARD) {
            return phasePair(Constants.BUILD_BLUE_RED_PROJECTS_PHASE);
        }
        if (action == CardAction.COMMUNICATIONS_STREAMLINING) {
            return phasePair(Constants.PERFORM_BLUE_ACTION_PHASE);
        }
        if (action == CardAction.UPDATE_PHASE_4_CARD) {
            return phasePair(Constants.COLLECT_INCOME_PHASE);
        }
        return phaseChoices(null);
    }

    private List<BuildInputChoice> phasePair(int phase) {
        int base = (phase - 1) * 2;
        return List.of(
                new BuildInputChoice(base, KIND_PHASE, InputFlag.PHASE_UPGRADE_CARD.getId()),
                new BuildInputChoice(base + 1, KIND_PHASE, InputFlag.PHASE_UPGRADE_CARD.getId())
        );
    }

    private List<BuildInputChoice> biomedicalChoices(MarsGame game) {
        List<BuildInputChoice> choices = new ArrayList<>(phaseChoices(null));
        if (!game.getPlanetAtTheStartOfThePhase().isOxygenMax()) {
            choices.add(new BuildInputChoice(ID_OXYGEN, KIND_OXYGEN, InputFlag.BIOMEDICAL_IMPORTS_RAISE_OXYGEN.getId()));
        }
        return choices;
    }

    private List<BuildInputChoice> resourceTargetChoices(Player player, Card building,
                                                         int flag, Set<CardCollectableResource> types,
                                                         boolean allowSkip) {
        List<BuildInputChoice> choices = new ArrayList<>();
        for (Card target : resourceCards(player, building, types, true)) {
            choices.add(new BuildInputChoice(target.getId(), KIND_TARGET, flag));
        }
        if (allowSkip) {
            choices.add(new BuildInputChoice(ID_SKIP, KIND_SKIP, flag));
        }
        return choices;
    }

    private List<BuildInputChoice> importedHydrogenChoices(Player player, Card building) {
        List<BuildInputChoice> choices = resourceTargetChoices(player, building,
                InputFlag.IMPORTED_HYDROGEN_PUT_RESOURCE.getId(),
                Set.of(CardCollectableResource.MICROBE, CardCollectableResource.ANIMAL), false);
        choices.add(new BuildInputChoice(ID_PLANT, KIND_PLANT, InputFlag.IMPORTED_HYDROGEN_PICK_PLANT.getId()));
        return choices;
    }

    private List<BuildInputChoice> largeConvoyChoices(Player player, Card building) {
        List<BuildInputChoice> choices = resourceTargetChoices(player, building,
                InputFlag.LARGE_CONVOY_ADD_ANIMAL.getId(), Set.of(CardCollectableResource.ANIMAL), false);
        choices.add(new BuildInputChoice(ID_PLANT, KIND_PLANT, InputFlag.LARGE_CONVOY_PICK_PLANT.getId()));
        return choices;
    }

    private List<BuildInputChoice> syntheticCatastropheChoices(Player player) {
        List<BuildInputChoice> choices = new ArrayList<>();
        for (Integer id : player.getPlayed().getCards()) {
            Card played = cardService.getCard(id);
            if (played.getColor() == CardColor.RED) {
                choices.add(new BuildInputChoice(id, KIND_TARGET, InputFlag.SYNTHETIC_CATASTROPHE_CARD.getId()));
            }
        }
        return choices;
    }

    private Optional<Slot> decomposersSlot(Player player, Card card, Map<Integer, List<Integer>> input) {
        if (!triggers(player, card, CardAction.DECOMPOSERS)) {
            return Optional.empty();
        }
        int tags = cardService.countCardTags(card, Set.of(Tag.ANIMAL, Tag.MICROBE, Tag.PLANT), input);
        if (tags == 0) {
            return Optional.empty();
        }
        int microbes = input.getOrDefault(InputFlag.DECOMPOSERS_TAKE_MICROBE.getId(), List.of(0)).getFirst();
        int cardsTaken = input.getOrDefault(InputFlag.DECOMPOSERS_TAKE_CARD.getId(), List.of(0)).getFirst();
        if (microbes + cardsTaken >= tags) {
            return Optional.empty();
        }
        List<BuildInputChoice> choices = new ArrayList<>();
        choices.add(new BuildInputChoice(ID_DECOMP_MICROBE, KIND_DECOMP_MICROBE, InputFlag.DECOMPOSERS_TAKE_MICROBE.getId()));
        int onCard = player.getCardResourcesCount().getOrDefault(Decomposers.class, 0) + microbes;
        if (onCard > cardsTaken) {
            choices.add(new BuildInputChoice(ID_DECOMP_CARD, KIND_DECOMP_CARD, InputFlag.DECOMPOSERS_TAKE_CARD.getId()));
        }
        return Optional.of(new Slot(choices));
    }

    private Optional<Slot> viralEnhancersSlot(Player player, Card card, Map<Integer, List<Integer>> input) {
        if (!triggers(player, card, CardAction.VIRAL_ENHANCERS)) {
            return Optional.empty();
        }
        int tags = cardService.countCardTags(card, Set.of(Tag.ANIMAL, Tag.MICROBE, Tag.PLANT), input);
        if (tags == 0) {
            return Optional.empty();
        }
        int plants = input.getOrDefault(InputFlag.VIRAL_ENHANCERS_TAKE_PLANT.getId(), List.of(0)).getFirst();
        int resources = input.getOrDefault(InputFlag.VIRAL_ENHANCERS_PUT_RESOURCE.getId(), List.of()).size();
        if (plants + resources >= tags) {
            return Optional.empty();
        }
        List<BuildInputChoice> choices = new ArrayList<>();
        choices.add(new BuildInputChoice(ID_PLANT, KIND_PLANT, InputFlag.VIRAL_ENHANCERS_TAKE_PLANT.getId()));
        for (Card target : resourceCards(player, card,
                Set.of(CardCollectableResource.ANIMAL, CardCollectableResource.MICROBE), true)) {
            choices.add(new BuildInputChoice(target.getId(), KIND_TARGET, InputFlag.VIRAL_ENHANCERS_PUT_RESOURCE.getId()));
        }
        return Optional.of(new Slot(choices));
    }

    private Optional<Slot> marsUniversitySlot(Player player, Card card, Map<Integer, List<Integer>> input) {
        if (!triggers(player, card, CardAction.MARS_UNIVERSITY)) {
            return Optional.empty();
        }
        if (input.containsKey(InputFlag.MARS_UNIVERSITY_DUMMY_INPUT.getId())) {
            return Optional.empty(); // stopped
        }
        int science = cardService.countCardTags(card, Set.of(Tag.SCIENCE), input);
        if (science == 0) {
            return Optional.empty();
        }
        List<Integer> discarded = input.getOrDefault(InputFlag.MARS_UNIVERSITY_CARD.getId(), List.of());
        if (discarded.contains(InputFlag.SKIP_ACTION.getId())) {
            return Optional.empty();
        }
        if (discarded.size() >= science) {
            return Optional.empty();
        }
        Set<Integer> already = new HashSet<>(discarded);
        List<BuildInputChoice> choices = new ArrayList<>();
        choices.add(new BuildInputChoice(ID_MU_STOP, KIND_MU_STOP, InputFlag.MARS_UNIVERSITY_CARD.getId()));
        for (Integer handId : player.getHand().getCards()) {
            if (handId == card.getId() || already.contains(handId)) {
                continue;
            }
            choices.add(new BuildInputChoice(handId, KIND_TARGET, InputFlag.MARS_UNIVERSITY_CARD.getId()));
        }
        return Optional.of(new Slot(choices));
    }

    private boolean triggers(Player player, Card card, CardAction action) {
        if (card.getCardMetadata() != null && card.getCardMetadata().getCardAction() == action) {
            return true;
        }
        return player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .anyMatch(c -> c.getCardMetadata() != null && c.getCardMetadata().getCardAction() == action);
    }

    private List<Card> resourceCards(Player player, Card building, Set<CardCollectableResource> types,
                                     boolean includeBuilding) {
        List<Card> cards = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (Integer id : player.getPlayed().getCards()) {
            Card played = cardService.getCard(id);
            if (types.contains(played.getCollectableResource()) && seen.add(id)) {
                cards.add(played);
            }
        }
        if (includeBuilding && types.contains(building.getCollectableResource()) && seen.add(building.getId())) {
            cards.add(building);
        }
        return cards;
    }

    private static boolean needsTag(CardAction action) {
        return action == CardAction.CHOOSE_TAG || action == CardAction.TOPOGRAPHIC_MAPPING;
    }

    private static boolean needsSinglePhase(CardAction action) {
        if (action == null) {
            return false;
        }
        return action == CardAction.CRYOGENIC_SHIPMENT
                || action == CardAction.TOPOGRAPHIC_MAPPING
                || action == CardAction.UPDATE_PHASE_1_CARD
                || action == CardAction.UPDATE_PHASE_2_CARD
                || action == CardAction.COMMUNICATIONS_STREAMLINING
                || action == CardAction.UPDATE_PHASE_4_CARD
                || AiConstants.CARD_ACTIONS_WITH_PHASE_UPGRADE_EFFECT.contains(action);
    }

    private static boolean needsTwoPhases(CardAction action) {
        return action == CardAction.UPDATE_PHASE_CARD_TWICE;
    }

    private static boolean isCeosFavorite(Card card) {
        return card.getCardMetadata() != null
                && !card.getCardMetadata().getResourcesOnBuild().isEmpty()
                && card.getCardMetadata().getResourcesOnBuild().getFirst().getType() == CardCollectableResource.ANY;
    }

    private boolean needsCargoChoice(Player player, Card card) {
        if (card.getCardMetadata() == null || card.getCardMetadata().getBonuses() == null) {
            return false;
        }
        boolean raisesInfra = card.getCardMetadata().getBonuses().stream()
                .anyMatch(bonus -> bonus.getType() == GainType.INFRASTRUCTURE);
        if (!raisesInfra) {
            return false;
        }
        return player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .anyMatch(c -> c.getCardMetadata() != null
                        && c.getCardMetadata().getCardAction() == CardAction.CARGO_SHIPS);
    }

    public record Slot(List<BuildInputChoice> choices) {
    }
}
