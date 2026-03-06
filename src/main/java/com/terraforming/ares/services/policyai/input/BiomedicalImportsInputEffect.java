package com.terraforming.ares.services.policyai.input;

import com.terraforming.ares.cards.blue.Herbivores;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.policyai.action.ActionInputService;
import com.terraforming.ares.services.policyai.action.HeadAction;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import com.terraforming.ares.services.policyai.rollout.EffectStateReader;
import com.terraforming.ares.services.policyai.service.AbstractPolicyProcessor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class BiomedicalImportsInputEffect extends AbstractPolicyProcessor implements InputGeneratorEffect {

    @Override
    public boolean isPresent(PolicyRecord record, TableContext tableContext, Card card, Map<Integer, List<Integer>> input) {
        return card.getCardMetadata().getCardAction() == CardAction.BIOMEDICAL_IMPORTS;
    }

    @Override
    public void generateAndApply(PolicyRecord record, TableContext tableContext, Map<Integer, List<Integer>> input, Card card) {
        boolean canRaiseOxygen = !tableContext.getGame().getPlanetAtTheStartOfThePhase().isOxygenMax();

        if (!canRaiseOxygen) {
            record.universalPhaseUpgradeCount = 1;
            generatePhaseUpgrade(record, input);
            record.universalPhaseUpgradeCount = 0;
            return;
        }

        List<HeadAction> validActions = List.of(
                ActionInputService.biomedicalImportsUpgradeOxygen(),
                ActionInputService.biomedicalImportsPhaseUpgrade()
        );

        record.biomedicalImportsEffect = true;
        HeadAction best = predict(record, validActions);
        record.biomedicalImportsEffect = false;

        if (best == ActionInputService.biomedicalImportsUpgradeOxygen()) {
            input.put(InputFlag.BIOMEDICAL_IMPORTS_RAISE_OXYGEN.getId(), List.of());
            // rollout кислорода
            record.winPointsNoForests[0]++;
            record.mcIncomeTotal[0]++;
            if (tableContext.getPlayer().isUnmiCorporation() && !tableContext.getPlayer().isDidUnmiAction()) {
                record.unmiTurnAvailable = true;
            }
            if (record.oxygenLeft > 0) {
                record.oxygenLeft--;
            }
            if (tableContext.getPlayer().getCardResourcesCount().containsKey(Herbivores.class)) {
                rolloutAnimal(Herbivores.class, record, 1);
            }
        } else {
            record.universalPhaseUpgradeCount = 1;
            generatePhaseUpgrade(record, input);
            record.universalPhaseUpgradeCount = 0;
        }
    }




}
