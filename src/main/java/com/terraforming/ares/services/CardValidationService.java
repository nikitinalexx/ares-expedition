package com.terraforming.ares.services;

import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.ProjectCard;
import com.terraforming.ares.model.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@Service
@RequiredArgsConstructor
public class CardValidationService {
    private final DeckService deckService;

    public String validateCard(PlayerContext player, Planet planet, int cardId) {
        ProjectCard projectCard = deckService.getProjectCard(cardId);
        if (projectCard == null) {
            return "Card doesn't exist " + cardId;
        }

        if (!player.getHand().getCards().contains(cardId)) {
            return "Can't build a project that you don't have";
        }

        return validateOxygen(planet, projectCard)
                .or(() -> validateTemperature(planet, projectCard))
                .or(() -> validateOceans(planet, projectCard))
                .or(() -> validateTags(player, projectCard))
                .orElse(null);
        //validate payment
    }

    private Optional<String> validateOxygen(Planet planet, ProjectCard card) {
        if (planet.isValidOxygen(card.getOxygenRequirement())) {
            return Optional.empty();
        } else {
            return Optional.of("Oxygen requirement not met");
        }
    }

    private Optional<String> validateTemperature(Planet planet, ProjectCard card) {
        if (planet.isValidTemperatute(card.getTemperatureRequirement())) {
            return Optional.empty();
        } else {
            return Optional.of("Temperature requirement not met");
        }
    }

    private Optional<String> validateOceans(Planet planet, ProjectCard card) {
        if (planet.isValidNumberOfOceans(card.getOceanRequirement())) {
            return Optional.empty();
        } else {
            return Optional.of("Ocean requirement not met");
        }
    }

    private Optional<String> validateTags(PlayerContext playerContext, ProjectCard projectCard) {
        List<Integer> cards = playerContext.getPlayed().getCards();

        List<Tag> tagRequirements = new LinkedList<>(projectCard.getTagRequirements());

        if (tagRequirements.isEmpty()) {
            return Optional.empty();
        }

        for (Integer card : cards) {
            ProjectCard builtProject = deckService.getProjectCard(card);

            tagRequirements.removeAll(builtProject.getTags());

            if (tagRequirements.isEmpty()) {
                return Optional.empty();
            }
        }

        return Optional.of("Project tag requirements not met");
    }
}
