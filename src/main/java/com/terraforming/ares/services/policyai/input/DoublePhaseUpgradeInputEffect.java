package com.terraforming.ares.services.policyai.input;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import com.terraforming.ares.services.policyai.service.AbstractPolicyProcessor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class DoublePhaseUpgradeInputEffect extends AbstractPolicyProcessor implements InputGeneratorEffect {

    @Override
    public boolean isPresent(PolicyRecord record, TableContext tableContext, Card card, Map<Integer, List<Integer>> input) {
        return card.getCardMetadata().getCardAction() == CardAction.UPDATE_PHASE_CARD_TWICE;
    }

    @Override
    public void generateAndApply(PolicyRecord record,
                                 TableContext tableContext,
                                 Map<Integer, List<Integer>> input,
                                 Card card) {

        record.universalPhaseUpgradeCount = 2;

        generatePhaseUpgrade(record, input);
        int first = input.get(InputFlag.PHASE_UPGRADE_CARD.getId()).getFirst();

        input.remove(InputFlag.PHASE_UPGRADE_CARD.getId());
        record.universalPhaseUpgradeCount--;

        generatePhaseUpgrade(record, input);
        int second = input.get(InputFlag.PHASE_UPGRADE_CARD.getId()).getFirst();

        record.universalPhaseUpgradeCount--;

        int firstPhase = first / 2;
        int secondPhase = second / 2;

        if (firstPhase == secondPhase) {

            int[] phaseUpgrades = record.getPhaseUpgrades();
            boolean fixed = false;

            for (int i = 0; i < phaseUpgrades.length; i++) {

                if (i == firstPhase) continue;

                int state = phaseUpgrades[i];

                if (state == 0) {
                    second = i * 2;
                    fixed = true;
                    break;
                }

                if (state == 1) {
                    second = i * 2 + 1;
                    fixed = true;
                    break;
                }

                if (state == 2) {
                    second = i * 2;
                    fixed = true;
                    break;
                }
            }

            if (!fixed) {
                int phases = phaseUpgrades.length;
                int next = (firstPhase + 1) % phases;
                second = next * 2;
            }
        }

        if (first / 2 == second / 2) {
            throw new IllegalStateException("Two upgrades from same phase: " + first + " " + second);
        }

        input.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(first, second));
    }
}
