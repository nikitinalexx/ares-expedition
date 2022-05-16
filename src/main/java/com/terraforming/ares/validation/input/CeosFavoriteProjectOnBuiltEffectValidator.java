package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.red.CeosFavoriteProject;
import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.ProjectCard;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@Component
@RequiredArgsConstructor
public class CeosFavoriteProjectOnBuiltEffectValidator implements OnBuiltEffectValidator<CeosFavoriteProject> {
    private static final String ERROR_MESSAGE_INPUT_MISSING = "CeosFavoriteProject: you are required to select a card to put resources on";
    private final CardService cardService;

    @Override
    public Class<CeosFavoriteProject> getType() {
        return CeosFavoriteProject.class;
    }

    @Override
    public String validate(ProjectCard builtCard, Player player, Map<Integer, List<Integer>> input) {
        if (!input.containsKey(InputFlag.CEOS_FAVORITE_PUT_RESOURCES.getId())) {
            return ERROR_MESSAGE_INPUT_MISSING;
        }

        List<Integer> cardInput = input.get(InputFlag.CEOS_FAVORITE_PUT_RESOURCES.getId());
        if (CollectionUtils.isEmpty(cardInput)) {
            return ERROR_MESSAGE_INPUT_MISSING;
        }

        int cardId = cardInput.get(0);

        if (!player.getPlayed().containsCard(cardId)) {
            return "You can't put resources on a card you haven't built";
        }

        ProjectCard targetCard = cardService.getProjectCard(cardId);
        if (targetCard.getCollectableResource() == CardCollectableResource.NONE || targetCard.getCollectableResource() == CardCollectableResource.FOREST) {
            return "Selected card doesn't collect any resources";
        }

        return null;
    }
}
