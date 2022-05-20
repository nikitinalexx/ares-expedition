package com.terraforming.ares.services;

import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.blueAction.AutoPickCardsAction;
import com.terraforming.ares.model.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
public class CardService {
    private final Map<Integer, ProjectCard> projects;
    private final Map<Integer, CorporationCard> corporations;

    public CardService(CardFactory cardFactory) {
        projects = cardFactory.createProjects();
        corporations = cardFactory.createCorporations();
    }

    public Deck createProjectsDeck(List<Expansion> expansions) {
        if (expansions.stream().anyMatch(Expansion.BASE::equals)) {
            return Deck.builder()
                    .cards(new LinkedList<>(projects.keySet()))
                    .build();
        } else {
            return Deck.builder().build();
        }
    }

    public Deck createCorporationsDeck(List<Expansion> expansions) {
        if (expansions.stream().anyMatch(Expansion.BASE::equals)) {
            return Deck.builder()
                    .cards(new LinkedList<>(corporations.keySet()))
                    .build();
        } else {
            return Deck.builder().build();
        }
    }

    public Card getCard(int id) {
        ProjectCard projectCard = projects.get(id);
        if (projectCard != null) {
            return projectCard;
        }
        return corporations.get(id);
    }

    public AutoPickCardsAction dealCards(Deck deck, Player player) {
        AutoPickCardsAction.AutoPickCardsActionBuilder resultBuilder = AutoPickCardsAction.builder();

        for (Integer card : deck.getCards()) {
            player.getHand().addCard(card);
            resultBuilder.takenCard(CardDto.from(getCard(card)));
        }

        return resultBuilder.build();
    }

    public int countPlayedTags(Player player, Tag tag) {
        if (player == null || player.getPlayed() == null || CollectionUtils.isEmpty(player.getPlayed().getCards())) {
            return 0;
        }

        return (int) player.getPlayed()
                .getCards()
                .stream()
                .map(this::getCard)
                .flatMap(card -> card.getTags().stream())
                .filter(tag::equals)
                .count();
    }

    public int countPlayedCards(Player player, Set<CardColor> colors) {
        if (player == null || player.getPlayed() == null || CollectionUtils.isEmpty(player.getPlayed().getCards())) {
            return 0;
        }

        return (int) player.getPlayed()
                .getCards()
                .stream()
                .map(this::getCard)
                .map(Card::getColor)
                .filter(colors::contains)
                .count();
    }

}
