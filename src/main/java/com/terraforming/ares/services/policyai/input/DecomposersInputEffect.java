package com.terraforming.ares.services.policyai.input;

import com.terraforming.ares.cards.blue.Decomposers;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.policyai.action.ActionInputService;
import com.terraforming.ares.services.policyai.action.ActionRegistry;
import com.terraforming.ares.services.policyai.action.HeadAction;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import com.terraforming.ares.services.policyai.rollout.EffectStateReader;
import com.terraforming.ares.services.policyai.service.AbstractPolicyProcessor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DecomposersInputEffect extends AbstractPolicyProcessor implements InputGeneratorEffect {

    @Override
    public boolean isPresent(PolicyRecord record, TableContext tableContext, Card card, Map<Integer, List<Integer>> input) {
        return (record.hasPlayedBlueCard(Decomposers.class) || card.getClass() == Decomposers.class)
                && countDecomposerTags(card, input) > 0;
    }

    @Override
    public void generateAndApply(PolicyRecord record, TableContext tableContext, Map<Integer, List<Integer>> input, Card card) {
        int totalActivations = countDecomposerTags(card, input);
        int currentMicrobes;

        int takeMicrobes = 0;
        int takeCards = 0;

        record.decomposersActivationsLeft = (byte) totalActivations;

        for (int i = 0; i < totalActivations; i++) {
            currentMicrobes = record.microbeResources[0][ActionRegistry.MICROBE_TARGET_TO_OFFSET.get(Decomposers.class)];

            if (currentMicrobes == 0) {
                // нет выбора — только микроб
                record.microbeResources[0][ActionRegistry.MICROBE_TARGET_TO_OFFSET.get(Decomposers.class)]++;
                microbeAdded(record, 1);
                takeMicrobes++;
                record.decomposersActivationsLeft--;
                continue;
            }

            List<HeadAction> validActions = List.of(
                    ActionInputService.decomposersPickMicrobe(),
                    ActionInputService.decomposersPickCard()
            );

            HeadAction best = predict(record, validActions);
            record.decomposersActivationsLeft--;

            if (best == ActionInputService.decomposersPickMicrobe()) {
                record.microbeResources[0][ActionRegistry.MICROBE_TARGET_TO_OFFSET.get(Decomposers.class)]++;
                microbeAdded(record, 1);
                takeMicrobes++;
            } else {
                takeCards++;
                record.addGenericCardToHand();
                record.microbeResources[0][ActionRegistry.MICROBE_TARGET_TO_OFFSET.get(Decomposers.class)]--;
            }
        }

        record.decomposersActivationsLeft = 0;

        if (takeMicrobes > 0) {
            input.put(InputFlag.DECOMPOSERS_TAKE_MICROBE.getId(), List.of(takeMicrobes));
        }
        if (takeCards > 0) {
            input.put(InputFlag.DECOMPOSERS_TAKE_CARD.getId(), List.of(takeCards));
        }
    }

    private int countDecomposerTags(Card card, Map<Integer, List<Integer>> input) {
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












