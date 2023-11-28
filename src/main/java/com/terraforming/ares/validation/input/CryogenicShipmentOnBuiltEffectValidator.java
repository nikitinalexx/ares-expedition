package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.blue.BacterialAggregates;
import com.terraforming.ares.cards.red.CryogenicShipment;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardResourceService;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class CryogenicShipmentOnBuiltEffectValidator implements OnBuiltEffectValidator<CryogenicShipment> {
    private final OnBuiltEffectValidationService onBuiltEffectValidationService;
    private final CardService cardService;
    private final CardResourceService cardResourceService;
    private static final String INCORRECT_INPUT_ERROR_MESSAGE =
            "Cryogenic Shipment: requires an input with choice to put resources on";

    @Override
    public Class<CryogenicShipment> getType() {
        return CryogenicShipment.class;
    }

    @Override
    public String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        if (!input.containsKey(InputFlag.CRYOGENIC_SHIPMENT_PUT_RESOURCE.getId())) {
            return INCORRECT_INPUT_ERROR_MESSAGE;
        }

        List<Integer> cardInput = input.get(InputFlag.CRYOGENIC_SHIPMENT_PUT_RESOURCE.getId());
        if (CollectionUtils.isEmpty(cardInput)) {
            return INCORRECT_INPUT_ERROR_MESSAGE;
        }

        Integer inputCardId = cardInput.get(0);

        if (inputCardId != InputFlag.SKIP_ACTION.getId()) {
            if (!player.getPlayed().containsCard(inputCardId)) {
                return "Selected card was not built by player";
            }

            Card inputCard = cardService.getCard(inputCardId);
            if (inputCard.getCollectableResource() != CardCollectableResource.MICROBE
                    && inputCard.getCollectableResource() != CardCollectableResource.ANIMAL) {
                return "Selected card does not collect Animals or Microbes";
            }

            if (inputCard.getClass().equals(BacterialAggregates.class) && player.getCardResourcesCount().get(BacterialAggregates.class) == 5) {
                return "The maximum number of microbes has already been reached on the selected card";
            }

            String resourceSubmissionMessage = cardResourceService.resourceSubmissionMessage(inputCard,
                    BacterialAggregates.class,
                    player.getCardResourcesCount().get(BacterialAggregates.class),
                    5);

            if (resourceSubmissionMessage != null) {
                return resourceSubmissionMessage;
            }

            return null;
        }

        return onBuiltEffectValidationService.validatePhaseUpgrade(input);
    }
}
