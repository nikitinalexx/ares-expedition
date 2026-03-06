package com.terraforming.ares.services.policyai.rollout;

import com.terraforming.ares.cards.blue.Herbivores;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.policyai.action.ActionInputService;
import com.terraforming.ares.services.policyai.action.HeadAction;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import org.nd4j.common.io.CollectionUtils;

import java.util.List;
import java.util.Map;

public class BiomedicalImportsEffect extends AbstractTagEffect {

    private final List<Integer> phaseInput;
    private final List<Integer> raiseOxygenInput;
    private final Card playedCard;
    private final MarsGame marsGame;
    private final Player player;

    public BiomedicalImportsEffect(Card playedCard, Map<Integer, List<Integer>> params, MarsGame marsGame, Player player) {
        this.phaseInput = params.get(InputFlag.PHASE_UPGRADE_CARD.getId());
        this.raiseOxygenInput = params.get(InputFlag.BIOMEDICAL_IMPORTS_RAISE_OXYGEN.getId());
        this.playedCard = playedCard;
        this.marsGame = marsGame;
        this.player = player;
    }

    @Override
    public boolean isPresent() {
        return playedCard.getCardMetadata().getCardAction() == CardAction.BIOMEDICAL_IMPORTS;
    }

    @Override
    public void prepareContext(PolicyRecord record) {
        record.biomedicalImportsEffect = true;
    }

    @Override
    public void clearContext(PolicyRecord record) {
        record.biomedicalImportsEffect = false;
    }

    @Override
    public List<EffectDecision> resolveDecisions(EffectStateReader state) {
        boolean choseOxygen = !CollectionUtils.isEmpty(raiseOxygenInput);
        boolean couldChooseOxygen = !marsGame.getPlanetAtTheStartOfThePhase().isOxygenMax();

        if (!couldChooseOxygen) {
            return List.of(upgradeDecision());
        }

        if (!choseOxygen) {
            return List.of(chooseDecision(), upgradeDecision());
        }

        return List.of(chooseDecision());
    }

    private EffectDecision upgradeDecision() {
        int phaseUpgradeIndex = phaseInput.getFirst();

        return new EffectDecision() {
            @Override
            public HeadAction getChosenAction() {
                return ActionInputService.choosePhaseUpgradeAction(phaseUpgradeIndex);
            }

            @Override
            public void applyRollout(PolicyRecord record) {
                record.universalPhaseUpgradeCount = 0;
                record.setPhaseUpgrade(phaseUpgradeIndex);
            }
        };
    }

    private EffectDecision chooseDecision() {
        boolean choseOxygen = !CollectionUtils.isEmpty(raiseOxygenInput);

        return new EffectDecision() {
            @Override
            public HeadAction getChosenAction() {
                if (choseOxygen) {
                    return ActionInputService.biomedicalImportsUpgradeOxygen();
                } else {
                    return ActionInputService.biomedicalImportsPhaseUpgrade();
                }
            }

            @Override
            public void applyRollout(PolicyRecord record) {
                record.biomedicalImportsEffect = false;
                // если выбрал кислород — откатываем кислород в состоянии
                if (choseOxygen) {
                    if (!marsGame.getPlanetAtTheStartOfThePhase().isOxygenMax()) {
                        //simulate terraforming rating
                        record.winPointsNoForests[0]++;
                        record.mcIncomeTotal[0]++;
                        if (player.isUnmiCorporation() && !player.isDidUnmiAction()) {
                            record.unmiTurnAvailable = true;
                        }
                    }
                    if (record.oxygenLeft > 0) {
                        record.oxygenLeft--;
                    }
                    if (player.getCardResourcesCount().containsKey(Herbivores.class)) {
                        rolloutAnimal(Herbivores.class, record, 1);
                    }
                } else {
                    record.universalPhaseUpgradeCount = 1;
                }
            }
        };
    }
}
