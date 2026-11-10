package com.terraforming.ares.services.agent;

import com.terraforming.ares.cards.blue.Decomposers;
import com.terraforming.ares.cards.blue.ResearchGrant;
import com.terraforming.ares.cards.blue.Sawmill;
import com.terraforming.ares.cards.blue.InterplanetarySuperhighway;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.action.ActionInputData;
import com.terraforming.ares.model.action.ActionInputDataType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.TerraformingService;
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
 * Inputs for a blue action that was already chosen ({@link DecisionType#PERFORM_BLUE_ACTION}). Filled as a sequence of
 * {@link DecisionType#BLUE_INPUT} decisions — heat spends, hand discards and resource targets do not fit a joint menu.
 */
@Service
@RequiredArgsConstructor
public class BlueInputDecisions {
    public static final int KIND_PHASE = 0;
    public static final int KIND_TAG = 1;
    public static final int KIND_TARGET = 2;
    public static final int KIND_PLANT = 3;
    public static final int KIND_HEAT = 4;
    public static final int KIND_MICROBE_ADD = 5;
    public static final int KIND_MICROBE_SPEND = 6;
    public static final int KIND_CARGO_HEAT = 7;
    public static final int KIND_CARGO_PLANT = 8;
    public static final int KIND_DISCARD_STOP = 9;
    public static final int KIND_DECOMP_MICROBE = 10;
    public static final int KIND_DECOMP_CARD = 11;
    public static final int KINDS = 12;

    public static final int ID_PLANT = 0xFFFD;
    public static final int ID_MICROBE_ADD = 0xFFFC;
    public static final int ID_MICROBE_SPEND = 0xFFFB;
    public static final int ID_CARGO_HEAT = 0xFFFA;
    public static final int ID_CARGO_PLANT = 0xFFF9;
    public static final int ID_DISCARD_STOP = 0xFFF8;
    public static final int ID_DECOMP_MICROBE = 0xFFF7;
    public static final int ID_DECOMP_CARD = 0xFFF6;

    public static final int PHASE_UPGRADES = 10;
    public static final int MAX_CANDIDATES = 200;
    public static final int CANDIDATE_FEATURES = KINDS + CardFeatures.LENGTH + PHASE_UPGRADES + CardFeatures.TAGS + 1;
    public static final String OBSERVATION_LAYOUT = ObservationBuilder.LAYOUT
            + "; candidate=[kind(" + KINDS + "), cardFeatures(" + CardFeatures.LENGTH + "), phase(" + PHASE_UPGRADES
            + "), tag(" + CardFeatures.TAGS + "), amount/20]; sequential slots for blue-action InputFlags";

    private static final int DONE_MARKER = InputFlag.MARS_UNIVERSITY_DUMMY_INPUT.getId();

    private final ObservationBuilder observationBuilder;
    private final CardService cardService;
    private final CardValidationService cardValidationService;
    private final TerraformingService terraformingService;

    public static DecisionFile.Schema schema() {
        return new DecisionFile.Schema(DecisionType.BLUE_INPUT, ObservationBuilder.size(),
                DecisionType.BLUE_INPUT.maxCandidates(), CANDIDATE_FEATURES, OBSERVATION_LAYOUT);
    }

    public Map<Integer, List<Integer>> fill(MarsGame game, Player player, Card card,
                                            ToIntFunction<Decision> choose) {
        Map<Integer, List<Integer>> input = new HashMap<>();
        while (true) {
            Optional<Slot> slot = nextSlot(game, player, card, input);
            if (slot.isEmpty()) {
                input.remove(DONE_MARKER);
                return input;
            }
            List<BlueInputChoice> choices = slot.get().choices();
            Decision decision = decision(game, player, choices);
            int index = choose.applyAsInt(decision);
            apply(input, choices.get(index), card, player);
        }
    }

    public Decision decision(MarsGame game, Player player, List<BlueInputChoice> choices) {
        List<Candidate> candidates = new ArrayList<>(choices.size());
        for (BlueInputChoice choice : choices) {
            candidates.add(new Candidate(choice.id(), features(choice)));
        }
        if (candidates.size() > MAX_CANDIDATES) {
            throw new IllegalStateException("Blue input menu has " + candidates.size() + " options");
        }
        return new Decision(DecisionType.BLUE_INPUT, player.getUuid(),
                observationBuilder.build(game, player), candidates);
    }

    /** True when the card can be activated with some legal input this helper can build. */
    public boolean canActivate(MarsGame game, Player player, Card card) {
        try {
            Map<Integer, List<Integer>> probe = fill(game, player, card, d -> 0);
            return cardValidationService.validateBlueAction(player, game, card.getId(), probe) == null;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public Optional<Slot> nextSlot(MarsGame game, Player player, Card card, Map<Integer, List<Integer>> input) {
        if (input.containsKey(DONE_MARKER)) {
            return Optional.empty();
        }
        CardAction action = card.getCardMetadata() == null ? null : card.getCardMetadata().getCardAction();
        List<ActionInputData> data = card.getCardMetadata() == null ? List.of()
                : card.getCardMetadata().getActionsInputData();
        ActionInputData first = data.isEmpty() ? null : data.getFirst();

        if (needsCargo(card) && !input.containsKey(InputFlag.CARGO_SHIPS.getId())) {
            return Optional.of(new Slot(List.of(
                    new BlueInputChoice(ID_CARGO_PLANT, KIND_CARGO_PLANT, InputFlag.CARGO_SHIPS.getId()),
                    new BlueInputChoice(ID_CARGO_HEAT, KIND_CARGO_HEAT, InputFlag.CARGO_SHIPS.getId())
            )));
        }

        if (action == CardAction.EXPERIMENTAL_TECHNOLOGY || action == CardAction.VIRTUAL_EMPLOYEE_DEVELOPMENT) {
            if (!input.containsKey(InputFlag.PHASE_UPGRADE_CARD.getId())) {
                return Optional.of(new Slot(phaseChoices()));
            }
            return Optional.empty();
        }

        if (action == CardAction.RESEARCH_GRANT) {
            if (!input.containsKey(InputFlag.TAG_INPUT.getId())) {
                return Optional.of(new Slot(tagChoices(player)));
            }
            Optional<Slot> triggered = triggeredFromTag(player, card, input);
            if (triggered.isPresent()) {
                return triggered;
            }
            return Optional.empty();
        }

        if (action == CardAction.EXTREME_COLD_FUNGUS
                && !input.containsKey(InputFlag.EXTEME_COLD_FUNGUS_PICK_PLANT.getId())
                && !input.containsKey(InputFlag.EXTREME_COLD_FUNGUS_PUT_MICROBE.getId())) {
            return Optional.of(new Slot(extremeColdChoices(player)));
        }

        if (action == CardAction.FIBROUS_COMPOSITE_MATERIAL) {
            if (!input.containsKey(InputFlag.ADD_DISCARD_MICROBE.getId())) {
                return Optional.of(new Slot(fibrousMicrobeChoices(player, card, first)));
            }
            int amount = input.get(InputFlag.ADD_DISCARD_MICROBE.getId()).getFirst();
            if (amount == 3 && !input.containsKey(InputFlag.PHASE_UPGRADE_CARD.getId())) {
                return Optional.of(new Slot(phaseChoices()));
            }
            return Optional.empty();
        }

        if (action == CardAction.POWER_INFRASTRUCTURE
                || action == CardAction.GREEN_HOUSES
                || action == CardAction.HITECH_LAB
                || (first != null && first.getType() == ActionInputDataType.DISCARD_HEAT)) {
            if (!input.containsKey(InputFlag.DISCARD_HEAT.getId())) {
                return Optional.of(new Slot(heatChoices(player, action, first)));
            }
            return Optional.empty();
        }

        if (action == CardAction.DECOMPOSING_FUNGUS || action == CardAction.CONSERVED_BIOME
                || action == CardAction.SYMBIOTIC_FUNGUD
                || (first != null && first.getType() == ActionInputDataType.MICROBE_ANIMAL_CARD)
                || (first != null && first.getType() == ActionInputDataType.MICROBE_CARD)) {
            if (!input.containsKey(InputFlag.CARD_CHOICE.getId())) {
                Set<CardCollectableResource> types = action == CardAction.SYMBIOTIC_FUNGUD
                        || (first != null && first.getType() == ActionInputDataType.MICROBE_CARD)
                        ? Set.of(CardCollectableResource.MICROBE)
                        : Set.of(CardCollectableResource.MICROBE, CardCollectableResource.ANIMAL);
                boolean requireResource = action == CardAction.DECOMPOSING_FUNGUS;
                List<BlueInputChoice> targets = resourceTargets(player, types, InputFlag.CARD_CHOICE.getId(),
                        requireResource);
                if (targets.isEmpty()) {
                    return Optional.empty();
                }
                return Optional.of(new Slot(targets));
            }
            return Optional.empty();
        }

        if (first != null && first.getType() == ActionInputDataType.DISCARD_CARD) {
            List<Integer> discarded = input.getOrDefault(InputFlag.CARD_CHOICE.getId(), List.of());
            if (discarded.size() >= first.getMax() || discarded.contains(InputFlag.SKIP_ACTION.getId())) {
                return Optional.empty();
            }
            List<BlueInputChoice> choices = new ArrayList<>();
            if (!discarded.isEmpty() || first.getMin() <= 0) {
                choices.add(new BlueInputChoice(ID_DISCARD_STOP, KIND_DISCARD_STOP, InputFlag.CARD_CHOICE.getId()));
            }
            Set<Integer> already = new HashSet<>(discarded);
            for (Integer handId : player.getHand().getCards()) {
                if (!already.contains(handId)) {
                    choices.add(new BlueInputChoice(handId, KIND_TARGET, InputFlag.CARD_CHOICE.getId()));
                }
            }
            if (choices.isEmpty()) {
                return Optional.empty();
            }
            // Must pick at least min cards before stop is allowed
            if (discarded.size() < first.getMin()) {
                choices.removeIf(c -> c.kind() == KIND_DISCARD_STOP);
            }
            if (choices.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new Slot(choices));
        }

        if (first != null && first.getType() == ActionInputDataType.ADD_DISCARD_MICROBE) {
            if (!input.containsKey(InputFlag.ADD_DISCARD_MICROBE.getId())) {
                return Optional.of(new Slot(addDiscardMicrobeChoices(game, player, card, action, first)));
            }
            return Optional.empty();
        }

        if (AiConstants.ACTIONS_WITHOUT_INPUT_PARAMS.contains(card.getClass())
                || cardValidationService.validateBlueAction(player, game, card.getId(), input) == null) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    void apply(Map<Integer, List<Integer>> input, BlueInputChoice choice, Card card, Player player) {
        switch (choice.kind()) {
            case KIND_PHASE -> input.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(choice.id()));
            case KIND_TAG -> input.put(InputFlag.TAG_INPUT.getId(), List.of(choice.id()));
            case KIND_TARGET -> {
                if (choice.flag() == InputFlag.CARD_CHOICE.getId()
                        || choice.flag() == InputFlag.VIRAL_ENHANCERS_PUT_RESOURCE.getId()
                        || choice.flag() == InputFlag.MARS_UNIVERSITY_CARD.getId()) {
                    List<Integer> list = new ArrayList<>(input.getOrDefault(choice.flag(), List.of()));
                    list.add(choice.id());
                    input.put(choice.flag(), list);
                } else if (choice.flag() == InputFlag.EXTREME_COLD_FUNGUS_PUT_MICROBE.getId()) {
                    input.put(choice.flag(), List.of(choice.id()));
                } else {
                    input.put(choice.flag(), List.of(choice.id()));
                }
            }
            case KIND_PLANT -> {
                if (choice.flag() == InputFlag.EXTEME_COLD_FUNGUS_PICK_PLANT.getId()) {
                    input.put(InputFlag.EXTEME_COLD_FUNGUS_PICK_PLANT.getId(), List.of(0));
                } else {
                    int plants = input.getOrDefault(InputFlag.VIRAL_ENHANCERS_TAKE_PLANT.getId(), List.of(0)).getFirst();
                    input.put(InputFlag.VIRAL_ENHANCERS_TAKE_PLANT.getId(), List.of(plants + 1));
                }
            }
            case KIND_HEAT -> input.put(InputFlag.DISCARD_HEAT.getId(), List.of(choice.id()));
            case KIND_MICROBE_ADD -> input.put(InputFlag.ADD_DISCARD_MICROBE.getId(), List.of(1));
            case KIND_MICROBE_SPEND -> {
                int max = choice.id() > 0 ? choice.id() : 3;
                input.put(InputFlag.ADD_DISCARD_MICROBE.getId(), List.of(max));
            }
            case KIND_CARGO_HEAT -> input.put(InputFlag.CARGO_SHIPS.getId(),
                    List.of(InputFlag.CARGO_SHIPS_HEAT.getId()));
            case KIND_CARGO_PLANT -> input.put(InputFlag.CARGO_SHIPS.getId(), List.of());
            case KIND_DISCARD_STOP -> {
                if (choice.flag() == InputFlag.MARS_UNIVERSITY_CARD.getId()
                        && !input.containsKey(InputFlag.MARS_UNIVERSITY_CARD.getId())) {
                    input.put(InputFlag.MARS_UNIVERSITY_CARD.getId(), List.of(InputFlag.SKIP_ACTION.getId()));
                }
                input.put(DONE_MARKER, List.of(1));
            }
            case KIND_DECOMP_MICROBE -> {
                int microbes = input.getOrDefault(InputFlag.DECOMPOSERS_TAKE_MICROBE.getId(), List.of(0)).getFirst();
                input.put(InputFlag.DECOMPOSERS_TAKE_MICROBE.getId(), List.of(microbes + 1));
            }
            case KIND_DECOMP_CARD -> {
                int cards = input.getOrDefault(InputFlag.DECOMPOSERS_TAKE_CARD.getId(), List.of(0)).getFirst();
                input.put(InputFlag.DECOMPOSERS_TAKE_CARD.getId(), List.of(cards + 1));
            }
            default -> throw new IllegalStateException("Unknown blue-input kind " + choice.kind());
        }
    }

    float[] features(BlueInputChoice choice) {
        float[] features = new float[CANDIDATE_FEATURES];
        features[choice.kind()] = 1f;
        int pos = KINDS;
        if (choice.kind() == KIND_TARGET && choice.id() < 20000) {
            Card card = cardService.getCard(choice.id());
            if (card != null) {
                System.arraycopy(CardFeatures.of(card, false), 0, features, pos, CardFeatures.LENGTH);
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
        pos += CardFeatures.TAGS;
        if (choice.kind() == KIND_HEAT || choice.kind() == KIND_MICROBE_SPEND) {
            features[pos] = choice.id() / 20f;
        }
        return features;
    }

    private List<BlueInputChoice> phaseChoices() {
        List<BlueInputChoice> choices = new ArrayList<>(PHASE_UPGRADES);
        for (int i = 0; i < PHASE_UPGRADES; i++) {
            choices.add(new BlueInputChoice(i, KIND_PHASE, InputFlag.PHASE_UPGRADE_CARD.getId()));
        }
        return choices;
    }

    private List<BlueInputChoice> tagChoices(Player player) {
        List<Tag> taken = player.getCardToTag().getOrDefault(ResearchGrant.class, List.of());
        List<BlueInputChoice> choices = new ArrayList<>();
        for (Tag tag : Tag.values()) {
            if (tag == Tag.DYNAMIC || taken.contains(tag)) {
                continue;
            }
            choices.add(new BlueInputChoice(tag.ordinal(), KIND_TAG, InputFlag.TAG_INPUT.getId()));
        }
        return choices;
    }

    private List<BlueInputChoice> heatChoices(Player player, CardAction action, ActionInputData data) {
        int max = player.getHeat();
        if (action == CardAction.GREEN_HOUSES) {
            max = Math.min(4, max);
        } else if (data != null && data.getMax() < Integer.MAX_VALUE && data.getMax() > 0) {
            max = Math.min(data.getMax(), max);
        }
        // Cap candidates: heat can be huge; 40 keeps the menu under MAX_CANDIDATES.
        max = Math.min(max, 40);
        int min = 1;
        if (data != null && data.getMin() > 0) {
            min = data.getMin();
        }
        List<BlueInputChoice> choices = new ArrayList<>();
        for (int h = min; h <= max; h++) {
            choices.add(new BlueInputChoice(h, KIND_HEAT, InputFlag.DISCARD_HEAT.getId()));
        }
        return choices;
    }

    private List<BlueInputChoice> fibrousMicrobeChoices(Player player, Card card, ActionInputData data) {
        List<BlueInputChoice> choices = new ArrayList<>();
        choices.add(new BlueInputChoice(ID_MICROBE_ADD, KIND_MICROBE_ADD, InputFlag.ADD_DISCARD_MICROBE.getId()));
        int max = data == null ? 3 : data.getMax();
        int have = player.getCardResourcesCount().getOrDefault(card.getClass(), 0);
        if (have >= max) {
            choices.add(new BlueInputChoice(max, KIND_MICROBE_SPEND, InputFlag.ADD_DISCARD_MICROBE.getId()));
        }
        return choices;
    }

    private List<BlueInputChoice> addDiscardMicrobeChoices(MarsGame game, Player player, Card card,
                                                           CardAction action, ActionInputData data) {
        List<BlueInputChoice> choices = new ArrayList<>();
        choices.add(new BlueInputChoice(ID_MICROBE_ADD, KIND_MICROBE_ADD, InputFlag.ADD_DISCARD_MICROBE.getId()));
        boolean mustAddOnly = (action == CardAction.GHG_PRODUCTION && !terraformingService.canIncreaseTemperature(game))
                || (action == CardAction.NITRITE_REDUCTING && !terraformingService.canRevealOcean(game))
                || (action == CardAction.REGOLITH_EATERS && !terraformingService.canIncreaseOxygen(game));
        int max = data.getMax();
        int have = player.getCardResourcesCount().getOrDefault(card.getClass(), 0);
        if (!mustAddOnly && have >= max) {
            choices.add(new BlueInputChoice(max, KIND_MICROBE_SPEND, InputFlag.ADD_DISCARD_MICROBE.getId()));
        }
        return choices;
    }

    private List<BlueInputChoice> extremeColdChoices(Player player) {
        List<BlueInputChoice> choices = new ArrayList<>();
        choices.add(new BlueInputChoice(ID_PLANT, KIND_PLANT, InputFlag.EXTEME_COLD_FUNGUS_PICK_PLANT.getId()));
        choices.addAll(resourceTargets(player, Set.of(CardCollectableResource.MICROBE),
                InputFlag.EXTREME_COLD_FUNGUS_PUT_MICROBE.getId(), false));
        return choices;
    }

    private List<BlueInputChoice> resourceTargets(Player player, Set<CardCollectableResource> types, int flag,
                                                  boolean requirePositiveCount) {
        List<BlueInputChoice> choices = new ArrayList<>();
        for (Integer id : player.getPlayed().getCards()) {
            Card played = cardService.getCard(id);
            if (!types.contains(played.getCollectableResource())) {
                continue;
            }
            if (requirePositiveCount && player.getCardResourcesCount().getOrDefault(played.getClass(), 0) < 1) {
                continue;
            }
            choices.add(new BlueInputChoice(id, KIND_TARGET, flag));
        }
        return choices;
    }

    private Optional<Slot> triggeredFromTag(Player player, Card card, Map<Integer, List<Integer>> input) {
        Optional<Slot> decomposers = decomposersSlot(player, card, input);
        if (decomposers.isPresent()) {
            return decomposers;
        }
        Optional<Slot> viral = viralSlot(player, card, input);
        if (viral.isPresent()) {
            return viral;
        }
        return marsUniversitySlot(player, card, input);
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
        List<BlueInputChoice> choices = new ArrayList<>();
        choices.add(new BlueInputChoice(ID_DECOMP_MICROBE, KIND_DECOMP_MICROBE, InputFlag.DECOMPOSERS_TAKE_MICROBE.getId()));
        int onCard = player.getCardResourcesCount().getOrDefault(Decomposers.class, 0) + microbes;
        if (onCard > cardsTaken) {
            choices.add(new BlueInputChoice(ID_DECOMP_CARD, KIND_DECOMP_CARD, InputFlag.DECOMPOSERS_TAKE_CARD.getId()));
        }
        return Optional.of(new Slot(choices));
    }

    private Optional<Slot> viralSlot(Player player, Card card, Map<Integer, List<Integer>> input) {
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
        List<BlueInputChoice> choices = new ArrayList<>();
        choices.add(new BlueInputChoice(ID_PLANT, KIND_PLANT, InputFlag.VIRAL_ENHANCERS_TAKE_PLANT.getId()));
        choices.addAll(resourceTargets(player, Set.of(CardCollectableResource.ANIMAL, CardCollectableResource.MICROBE),
                InputFlag.VIRAL_ENHANCERS_PUT_RESOURCE.getId(), false));
        return Optional.of(new Slot(choices));
    }

    private Optional<Slot> marsUniversitySlot(Player player, Card card, Map<Integer, List<Integer>> input) {
        if (!triggers(player, card, CardAction.MARS_UNIVERSITY)) {
            return Optional.empty();
        }
        int science = cardService.countCardTags(card, Set.of(Tag.SCIENCE), input);
        if (science == 0) {
            return Optional.empty();
        }
        List<Integer> discarded = input.getOrDefault(InputFlag.MARS_UNIVERSITY_CARD.getId(), List.of());
        if (discarded.contains(InputFlag.SKIP_ACTION.getId()) || discarded.size() >= science) {
            return Optional.empty();
        }
        List<BlueInputChoice> choices = new ArrayList<>();
        choices.add(new BlueInputChoice(ID_DISCARD_STOP, KIND_DISCARD_STOP, InputFlag.MARS_UNIVERSITY_CARD.getId()));
        Set<Integer> already = new HashSet<>(discarded);
        for (Integer handId : player.getHand().getCards()) {
            if (!already.contains(handId)) {
                choices.add(new BlueInputChoice(handId, KIND_TARGET, InputFlag.MARS_UNIVERSITY_CARD.getId()));
            }
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

    private static boolean needsCargo(Card card) {
        return card.getClass() == Sawmill.class || card.getClass() == InterplanetarySuperhighway.class;
    }

    public record Slot(List<BlueInputChoice> choices) {
    }
}
