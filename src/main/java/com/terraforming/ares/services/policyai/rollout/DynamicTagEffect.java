package com.terraforming.ares.services.policyai.rollout;

import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.policyai.action.ActionInputService;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Map;

public class DynamicTagEffect extends AbstractTagEffect {

    private final List<Integer> tagInput;

    public DynamicTagEffect(Map<Integer, List<Integer>> params) {
        this.tagInput = params.get(InputFlag.TAG_INPUT.getId());
    }

    @Override
    public boolean isPresent() {
        return !CollectionUtils.isEmpty(tagInput);
    }

    @Override
    public void prepareContext(PolicyRecord ctx) {
        ctx.choosingTag = true;
    }

    @Override
    public void clearContext(PolicyRecord ctx) {
        ctx.choosingTag = false;
    }

    @Override
    public List<EffectDecision> resolveDecisions(EffectStateReader state) {
        int tagIndex = tagInput.getFirst();

        return List.of(new EffectDecision() {
            @Override
            public int getChosenAction() {
                return ActionInputService.chooseTag(tagIndex);
            }

            @Override
            public void applyRollout(PolicyRecord record) {
                //TODO technically we could rollout more.
                switch (Tag.values()[tagIndex]) {
                    case SPACE -> record.spaceTags[0]++;
                    case EARTH -> record.earthTags[0]++;
                    case EVENT -> record.eventTags[0]++;
                    case SCIENCE -> record.scienceTags[0]++;
                    case PLANT -> record.plantTags[0]++;
                    case ENERGY -> record.energyTags[0]++;
                    case BUILDING -> record.buildingTags[0]++;
                    case ANIMAL -> record.animalTags[0]++;
                    case JUPITER -> record.jupiterTags[0]++;
                    case MICROBE -> record.microbeTags[0]++;
                }
            }
        });
    }

}
