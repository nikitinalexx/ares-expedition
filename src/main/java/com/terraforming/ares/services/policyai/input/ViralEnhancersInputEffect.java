package com.terraforming.ares.services.policyai.input;

import com.terraforming.ares.cards.blue.ViralEnhancers;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.policyai.ResourceHelper;
import com.terraforming.ares.services.policyai.action.ActionInputService;
import com.terraforming.ares.services.policyai.action.ActionRegistry;
import com.terraforming.ares.services.policyai.action.HeadAction;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import com.terraforming.ares.services.policyai.rollout.EffectStateReader;
import com.terraforming.ares.services.policyai.service.AbstractPolicyProcessor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ViralEnhancersInputEffect extends AbstractPolicyProcessor implements InputGeneratorEffect {

    @Override
    public boolean isPresent(PolicyRecord record, TableContext tableContext, Card card, Map<Integer, List<Integer>> input) {
        return (record.hasPlayedBlueCard(ViralEnhancers.class) || card.getClass() == ViralEnhancers.class)
                && countViralEnhancersTags(card, input) > 0;
    }

    @Override
    public void generateAndApply(PolicyRecord record, TableContext tableContext, Map<Integer, List<Integer>> input, Card card) {
        int totalTags = countViralEnhancersTags(card, input);

        List<Card> animalCards = ResourceHelper.getAnyAvailableAnimals(tableContext);
        List<Card> microbeCards = ResourceHelper.getAnyAvailableMicrobes(tableContext);

        Map<HeadAction, Card> resourceActionToCard = new HashMap<>();
        animalCards.forEach(c -> resourceActionToCard.put(ActionInputService.getAnimalTargetAction(c), c));
        microbeCards.forEach(c -> resourceActionToCard.put(ActionInputService.getMicrobeTargetAction(c), c));

        int takenPlants = 0;
        List<Integer> chosenResourceCards = new ArrayList<>();

        record.viralEnhancersActivationsLeft = (byte) totalTags;

        for (int i = 0; i < totalTags; i++) {
            if (resourceActionToCard.isEmpty()) {
                takenPlants++;
                record.plants[0]++;
                record.viralEnhancersActivationsLeft--;
                continue;
            }

            List<HeadAction> validActions = new ArrayList<>(resourceActionToCard.keySet());
            validActions.add(ActionInputService.takePlantTargetAction());

            HeadAction best = predict(record, validActions);
            record.viralEnhancersActivationsLeft--;

            if (best == ActionInputService.takePlantTargetAction()) {
                takenPlants++;
                record.plants[0]++;
            } else {
                Card target = resourceActionToCard.get(best);
                chosenResourceCards.add(target.getId());
                if (target.getCollectableResource() == CardCollectableResource.ANIMAL) {
                    rolloutAnimal(target.getClass(), record, 1);
                } else {
                    record.microbeResources[0][ActionRegistry.MICROBE_TARGET_TO_OFFSET.get(target.getClass())]++;
                    microbeAdded(record, 1);
                }
            }
        }

        record.viralEnhancersActivationsLeft = 0;

        if (takenPlants > 0) {
            input.put(InputFlag.VIRAL_ENHANCERS_TAKE_PLANT.getId(), List.of(takenPlants));
        }
        if (!chosenResourceCards.isEmpty()) {
            input.put(InputFlag.VIRAL_ENHANCERS_PUT_RESOURCE.getId(), chosenResourceCards);
        }
    }

    private int countViralEnhancersTags(Card card, Map<Integer, List<Integer>> input) {
        long tagCount = card.getTags().stream()
                .filter(t -> t == Tag.MICROBE || t == Tag.PLANT || t == Tag.ANIMAL)
                .count();
        List<Integer> tagInput = input.getOrDefault(InputFlag.TAG_INPUT.getId(), List.of());
        if (!tagInput.isEmpty()) {
            Tag t = Tag.values()[tagInput.getFirst()];
            if (t == Tag.MICROBE || t == Tag.PLANT || t == Tag.ANIMAL) {
                tagCount++;
            }
        }
        return (int) tagCount;
    }
}
