package com.terraforming.ares.services;

import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.blueAction.AutoPickCardsAction;
import com.terraforming.ares.mars.MarsGame;
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
    private final ShuffleService shuffleService;
    private final Map<Integer, Card> projects;
    private final Map<Integer, Card> corporations;

    public CardService(CardFactory cardFactory, ShuffleService shuffleService) {
        this.shuffleService = shuffleService;
        projects = cardFactory.createProjects();
        corporations = cardFactory.createCorporations();
    }

    public Deck createProjectsDeck(List<Expansion> expansions) {
        if (expansions.contains(Expansion.BASE)) {
            return createAndShuffleDeck(projects.keySet());
        }

        return Deck.builder().build();
    }

    public Deck createCorporationsDeck(List<Expansion> expansions) {
        if (expansions.contains(Expansion.BASE)) {
            return createAndShuffleDeck(corporations.keySet());
        }

        return Deck.builder().build();
    }

    private Deck createAndShuffleDeck(Set<Integer> cards) {
        LinkedList<Integer> cardsList = new LinkedList<>(cards);

        shuffleService.shuffle(cardsList);

        return Deck.builder().cards(cardsList).build();
    }

    public Card getCard(int id) {
        Card projectCard = projects.get(id);
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

    public int countPlayedTags(Player player, Set<Tag> tags) {
        if (player == null || player.getPlayed() == null || CollectionUtils.isEmpty(player.getPlayed().getCards())) {
            return 0;
        }

        return (int) player.getPlayed()
                .getCards()
                .stream()
                .map(this::getCard)
                .flatMap(card -> card.getTags().stream())
                .filter(tags::contains)
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

    public List<Integer> dealCards(MarsGame game, int count) {
        int size = game.getProjectsDeck().size();
        if (size < count) {
            Deck newProjectsDeck = createProjectsDeck(List.of(Expansion.BASE));
            game.mergeDeck(newProjectsDeck);
        }
        return game.dealCards(count);
    }

}
