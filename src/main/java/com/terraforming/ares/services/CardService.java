package com.terraforming.ares.services;

import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.blueAction.AutoPickCardsAction;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
public class CardService {
    private final ShuffleService shuffleService;
    private final Map<Expansion, Map<Integer, Card>> projects;
    private final Map<Integer, Card> baseCorporations;
    private final Map<Integer, Card> buffedCorporations;
    private final Map<Integer, Card> buffedCorporationsMapping;

    public CardService(CardFactory cardFactory, ShuffleService shuffleService) {
        this.shuffleService = shuffleService;
        projects = cardFactory.createAllProjects();
        baseCorporations = cardFactory.createCorporations();
        buffedCorporations = cardFactory.createBuffedCorporations();
        buffedCorporationsMapping = cardFactory.getBuffedCorporationsMapping();
    }

    public Deck createProjectsDeck(List<Expansion> expansions) {
        return createAndShuffleDeck(expansions.stream()
                .filter(projects::containsKey)
                .map(projects::get)
                .flatMap(cards -> cards.keySet().stream()));
    }

    public Deck createCorporationsDeck(List<Expansion> expansions) {
        Map<Integer, Card> corporations = new HashMap<>();
        if (expansions.contains(Expansion.BASE)) {
            corporations.putAll(this.baseCorporations);
        }

        if (expansions.contains(Expansion.BUFFED_CORPORATION)) {
            corporations.putAll(this.buffedCorporationsMapping);
        }

        return createAndShuffleDeck(
                corporations.values().stream().map(Card::getId)
        );
    }

    private Deck createAndShuffleDeck(Stream<Integer> cards) {
        LinkedList<Integer> cardsList = cards.collect(Collectors.toCollection(LinkedList::new));

        shuffleService.shuffle(cardsList);

        return Deck.builder().cards(cardsList).build();
    }

    public Card getCard(int id) {
        Card card;

        for (Map.Entry<Expansion, Map<Integer, Card>> expansionEntry : projects.entrySet()) {
            card = expansionEntry.getValue().get(id);
            if (card != null) {
                return card;
            }
        }

        card = baseCorporations.get(id);
        if (card != null) {
            return card;
        }
        return buffedCorporations.get(id);
    }

    public AutoPickCardsAction dealCards(Deck deck, Player player) {
        AutoPickCardsAction.AutoPickCardsActionBuilder resultBuilder = AutoPickCardsAction.builder();

        for (Integer card : deck.getCards()) {
            player.getHand().addCard(card);
            resultBuilder.takenCard(CardDto.from(getCard(card)));
        }

        return resultBuilder.build();
    }

    public int countCardTagsWithDynamic(Card card, Player player, Set<Tag> tagsToCount) {
        int tagCount = 0;

        if (card.getTags().contains(Tag.DYNAMIC) && player.getCardToTag().containsKey(card.getClass())
                && player.getCardToTag().get(card.getClass()).stream().anyMatch(tagsToCount::contains)) {
            tagCount++;
        }

        return tagCount + (int) card.getTags().stream().filter(tagsToCount::contains).count();
    }

    public int countCardTags(Card card, Set<Tag> tags, Map<Integer, List<Integer>> input) {
        int tagCount = 0;

        if (input != null && input.containsKey(InputFlag.TAG_INPUT.getId())) {
            List<Integer> tagInput = input.get(InputFlag.TAG_INPUT.getId());

            if (tags.contains(Tag.byIndex(tagInput.get(0)))) {
                tagCount++;
            }
        }

        return tagCount + (int) card.getTags().stream().filter(tags::contains).count();
    }

    public List<Tag> getCardTags(Card card, Map<Integer, List<Integer>> input) {
        List<Tag> result = new ArrayList<>();

        if (input != null && input.containsKey(InputFlag.TAG_INPUT.getId())) {
            List<Integer> tagInput = input.get(InputFlag.TAG_INPUT.getId());
            result.add(Tag.byIndex(tagInput.get(0)));
        }

        result.addAll(card.getTags());
        return result;
    }

    public int countPlayedTags(Player player, Set<Tag> tagsToCheck) {
        if (player == null || player.getPlayed() == null || CollectionUtils.isEmpty(player.getPlayed().getCards())) {
            return 0;
        }

        return (int) (
                player.getPlayed().getCards().stream().map(this::getCard)
                        .filter(card -> player.getCardToTag().containsKey(card.getClass()))
                        .flatMap(card -> player.getCardToTag().get(card.getClass()).stream())
                        .filter(tagsToCheck::contains)
                        .count()

                        + player.getPlayed()
                        .getCards()
                        .stream()
                        .map(this::getCard)
                        .flatMap(card -> card.getTags().stream())
                        .filter(tagsToCheck::contains)
                        .count()
        );
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
            Deck newProjectsDeck = createProjectsDeck(game.getExpansions());
            game.mergeDeck(newProjectsDeck);
        }
        return game.dealCards(count);
    }

    public Integer dealCardWithTag(Tag tag, MarsGame game) {
        while (true) {
            List<Integer> cards = dealCards(game, 1);
            if (CollectionUtils.isEmpty(cards)) {
                break;
            }
            Integer cardId = cards.get(0);

            final Card card = getCard(cardId);

            if (card.getTags().contains(tag) || card.getTags().contains(Tag.DYNAMIC)) {
                return cardId;
            }
        }
        throw new IllegalStateException("Unable to find a card with tag");
    }

}
