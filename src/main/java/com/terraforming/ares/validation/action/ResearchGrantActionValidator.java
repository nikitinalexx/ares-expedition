package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.ResearchGrant;
import com.terraforming.ares.cards.corporations.DummyCard;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.validation.input.OnBuiltEffectValidator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 21.02.2023
 */
@Component
public class ResearchGrantActionValidator implements ActionValidator<ResearchGrant> {
    private static final String ERROR_MESSAGE = "Choose the tag to put on card";

    private final CardService cardService;
    private final Map<Class<?>, OnBuiltEffectValidator<?>> onBuiltEffectValidators;

    public ResearchGrantActionValidator(CardService cardService,
                                        List<OnBuiltEffectValidator<?>> onBuiltEffectValidators) {
        this.cardService = cardService;

        this.onBuiltEffectValidators = onBuiltEffectValidators.stream().collect(
                Collectors.toMap(
                        OnBuiltEffectValidator::getType,
                        Function.identity()
                )
        );
    }

    @Override
    public Class<ResearchGrant> getType() {
        return ResearchGrant.class;
    }

    @Override
    public String validate(MarsGame game, Player player, Map<Integer, List<Integer>> inputParameters) {
        final List<Tag> cardTags = player.getCardToTag().get(ResearchGrant.class);
        if (cardTags.stream().noneMatch(tag -> tag == Tag.DYNAMIC)) {
            return "No more space for new tags";
        }
        if (inputParameters.isEmpty()) {
            return ERROR_MESSAGE;
        }

        final List<Integer> tagInputParams = inputParameters.get(InputFlag.TAG_INPUT.getId());
        if (tagInputParams.isEmpty()) {
            return ERROR_MESSAGE;
        }

        final Tag inputTag = Tag.byIndex(tagInputParams.get(0));

        if (inputTag == null || inputTag == Tag.DYNAMIC) {
            return "Choose the valid tag to put on card";
        }

        if (cardTags.stream().anyMatch(tag -> tag == inputTag)) {
            return "You can't put the same tag on this card";
        }


        return player.getPlayed()
                .getCards()
                .stream()
                .map(cardService::getCard)
                .filter(Card::onBuiltEffectApplicableToOther)
                .map(c -> onBuiltEffectValidators.get(c.getClass()))
                .filter(Objects::nonNull)
                .map(validator -> validator.validate(game, new DummyCard(), player, inputParameters))
                .filter(Objects::nonNull)
                .findAny()
                .orElse(null);
    }
}
