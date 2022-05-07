package com.terraforming.ares.cards.blue;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

import static com.terraforming.ares.model.InputFlag.VIRAL_ENHANCERS_TAKE_PLANT;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class ViralEnhancers implements BlueCard {
    private final int id;

    @Override
    public void onOtherProjectBuiltEffect(CardService cardService, MarsGame game, Player player, ProjectCard project, Map<Integer, List<Integer>> inputParams) {
        long tagsCount = project.getTags()
                .stream()
                .filter(tag -> tag == Tag.ANIMAL || tag == Tag.MICROBE || tag == Tag.PLANT)
                .count();

        if (tagsCount == 0) {
            return;
        }

        List<Integer> takePlantsInput = inputParams.getOrDefault(VIRAL_ENHANCERS_TAKE_PLANT.getId(), List.of());
        List<Integer> microbeAnimalsInput = inputParams.getOrDefault(InputFlag.VIRAL_ENHANCERS_PUT_RESOURCE.getId(), List.of());

        if (!CollectionUtils.isEmpty(takePlantsInput)) {
            player.setPlants(player.getPlants() + takePlantsInput.get(0));
        }

        if (!CollectionUtils.isEmpty(microbeAnimalsInput)) {
            for (Integer cardId : microbeAnimalsInput) {
                ProjectCard projectCard = cardService.getProjectCard(cardId);
                player.getCardResourcesCount().put(
                        projectCard.getClass(),
                        player.getCardResourcesCount().get(projectCard.getClass()) + 1
                );
            }
        }
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        //TODO test
        return true;
    }

    @Override
    public String description() {
        return "When you play an Animal, Microbe, or Plant, including these, gain 1 plant or add 1 animal or microbe to ANOTHER* card.";
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.MICROBE, Tag.PLANT);
    }

    @Override
    public int getPrice() {
        return 8;
    }
}
