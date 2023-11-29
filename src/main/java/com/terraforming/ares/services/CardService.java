package com.terraforming.ares.services;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
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
    private final Map<Integer, Card> discoveryCorporations;
    private final Map<Integer, Card> buffedCorporations;
    private final Map<Integer, Card> buffedCorporationsMapping;
    private final Set<Integer> crysisExcludedCards;
    private final Map<Integer, CrysisCard> crysisCards;
    private final Random random = new Random();

    private final Set<Integer> crysisCardsTier1;
    private final Set<Integer> crysisCardsTier2;
    private final Set<Integer> crysisCardsTier3;
    private final Set<Integer> crysisCardsTier4;
    private final Set<Integer> crysisCardsTier5;


    public CardService(CardFactory cardFactory, ShuffleService shuffleService) {
        this.shuffleService = shuffleService;
        projects = cardFactory.createAllProjects();
        baseCorporations = cardFactory.createBaseCorporations();
        discoveryCorporations = cardFactory.createDiscoveryCorporations();
        buffedCorporations = cardFactory.createBuffedCorporations();
        buffedCorporationsMapping = cardFactory.getBuffedCorporationsMapping();
        crysisExcludedCards = cardFactory.getCrysisExcludedCards();
        this.crysisCards = cardFactory.getCrysisCards().stream().collect(
                Collectors.toMap(CrysisCard::getId, Function.identity())
        );

        this.crysisCardsTier1 = getCrysisCardsByTier(cardFactory, CardTier.T1);
        this.crysisCardsTier2 = getCrysisCardsByTier(cardFactory, CardTier.T2);
        this.crysisCardsTier3 = getCrysisCardsByTier(cardFactory, CardTier.T3);
        this.crysisCardsTier4 = getCrysisCardsByTier(cardFactory, CardTier.T4);
        this.crysisCardsTier5 = getCrysisCardsByTier(cardFactory, CardTier.T5);
    }

    private Set<Integer> getCrysisCardsByTier(CardFactory cardFactory, CardTier tier) {
        return cardFactory.getCrysisCards()
                .stream().filter(card -> card.tier() == tier)
                .map(CrysisCard::getId).collect(Collectors.toSet());
    }

    public Deck createProjectsDeck(List<Expansion> expansions) {
        Stream<Integer> cardIdsStream = expansions.stream()
                .filter(projects::containsKey)
                .map(projects::get)
                .flatMap(cards -> cards.keySet().stream());

        if (expansions.contains(Expansion.CRYSIS)) {
            cardIdsStream = cardIdsStream
                    .filter(cardId -> !crysisExcludedCards.contains(cardId));
        }
        return createAndShuffleDeck(cardIdsStream);
    }

    public Deck createCorporationsDeck(Set<Expansion> expansions) {
        Map<Integer, Card> corporations = new HashMap<>();
        if (expansions.contains(Expansion.BASE)) {
            corporations.putAll(this.baseCorporations);
        }

        if (expansions.contains(Expansion.BUFFED_CORPORATION)) {
            corporations.putAll(this.buffedCorporationsMapping);
        }

        if (expansions.contains(Expansion.DISCOVERY)) {
            corporations.putAll(this.discoveryCorporations);
        }

        corporations = corporations.values()
                .stream()
                .filter(card -> card.isSupportedByExpansionSet(expansions))
                .collect(Collectors.toMap(Card::getId, Function.identity()));

        return createAndShuffleDeck(
                corporations.values().stream().map(Card::getId)
        );
    }

    public Deck createCrysisDeck(List<Expansion> expansions, int playerCount) {
        if (!expansions.contains(Expansion.CRYSIS)) {
            return Deck.builder().build();
        }

        LinkedList<Integer> crysisDeck = new LinkedList<>();

        crysisDeck.addAll(prepareCrisisTier1Cards(playerCount));
        crysisDeck.addAll(prepareCrisisTier2Cards(playerCount));
        crysisDeck.addAll(prepareCrisisTier3Cards(playerCount));
        crysisDeck.addAll(prepareCrisisTier4Cards(playerCount));
        crysisDeck.addAll(prepareCrisisTier5Cards(playerCount));

        return Deck.builder()
                .cards(crysisDeck)
                .build();
    }

    private List<Integer> prepareCrisisTier1Cards(int playerCount) {
        final List<Integer> tier1CardsByPlayer = filterCrysisCardsByPlayerCount(crysisCardsTier1, playerCount);
        removeRandomCards(tier1CardsByPlayer, 1);
        shuffleService.shuffle(tier1CardsByPlayer);
        return tier1CardsByPlayer;
    }

    private List<Integer> prepareCrisisTier2Cards(int playerCount) {
        final List<Integer> tier2CardsByPlayer = filterCrysisCardsByPlayerCount(crysisCardsTier2, playerCount);
        removeRandomCards(tier2CardsByPlayer, 2);
        shuffleService.shuffle(tier2CardsByPlayer);
        return tier2CardsByPlayer;
    }

    private List<Integer> prepareCrisisTier3Cards(int playerCount) {
        final List<Integer> tier3CardsByPlayer = filterCrysisCardsByPlayerCount(crysisCardsTier3, playerCount);
        removeRandomCards(tier3CardsByPlayer, 2);
        shuffleService.shuffle(tier3CardsByPlayer);
        return tier3CardsByPlayer;
    }

    private List<Integer> prepareCrisisTier4Cards(int playerCount) {
        final List<Integer> tier4CardsByPlayer = filterCrysisCardsByPlayerCount(crysisCardsTier4, playerCount);
        shuffleService.shuffle(tier4CardsByPlayer);
        return tier4CardsByPlayer;
    }

    private List<Integer> prepareCrisisTier5Cards(int playerCount) {
        final List<Integer> tier5CardsByPlayer = filterCrysisCardsByPlayerCount(crysisCardsTier5, playerCount);
        shuffleService.shuffle(tier5CardsByPlayer);
        return tier5CardsByPlayer;
    }

    private void removeRandomCards(List<Integer> cards, int count) {
        if (cards.size() < count) {
            throw new IllegalStateException("Unable to create a Crisis deck");
        }
        for (int i = 0; i < count; i++) {
            cards.remove(random.nextInt(cards.size()));
        }
    }

    private List<Integer> filterCrysisCardsByPlayerCount(Set<Integer> cards, int playerCount) {
        return cards.stream().map(this::getCrysisCard)
                .filter(card -> card.playerCount() == playerCount)
                .map(CrysisCard::getId)
                .collect(Collectors.toList());
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

        card = discoveryCorporations.get(id);
        if (card != null) {
            return card;
        }

        card = baseCorporations.get(id);
        if (card != null) {
            return card;
        }
        return buffedCorporations.get(id);
    }

    public CrysisCard getCrysisCard(int id) {
        return crysisCards.get(id);
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
        List<Tag> result = new ArrayList<>(card.getTags());

        if (input != null && input.containsKey(InputFlag.TAG_INPUT.getId())) {
            List<Integer> tagInput = input.get(InputFlag.TAG_INPUT.getId());
            result.remove(Tag.DYNAMIC);
            result.add(Tag.byIndex(tagInput.get(0)));
        }

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

    public Map<Tag, Long> countPlayedTagsAsMap(Player player) {
        if (player == null || player.getPlayed() == null || CollectionUtils.isEmpty(player.getPlayed().getCards())) {
            return Map.of();
        }

        return Stream.concat(player.getCardToTag().values().stream().flatMap(
                        List::stream
                ),
                player.getPlayed()
                        .getCards()
                        .stream()
                        .map(this::getCard)
                        .flatMap(card -> card.getTags().stream())
        ).collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
    }

    public Map<Tag, Long> countTagsOnCards(List<Integer> cards) {
        return cards.stream().map(this::getCard).flatMap(card -> card.getTags().stream()).collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
    }

    public int countUniquePlayedTags(Player player, Set<Tag> tagsToCheck) {
        if (player == null || player.getPlayed() == null || CollectionUtils.isEmpty(player.getPlayed().getCards())) {
            return 0;
        }

        return (int)
                Stream.concat(
                        player.getPlayed().getCards().stream().map(this::getCard)
                                .filter(card -> player.getCardToTag().containsKey(card.getClass()))
                                .flatMap(card -> player.getCardToTag().get(card.getClass()).stream())
                                .filter(tagsToCheck::contains),
                        player.getPlayed()
                                .getCards()
                                .stream()
                                .map(this::getCard)
                                .flatMap(card -> card.getTags().stream())
                                .filter(tagsToCheck::contains)
                ).distinct().count();

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
