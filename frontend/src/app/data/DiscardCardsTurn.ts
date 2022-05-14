import {Card} from './Card';

export class DiscardCardsTurn {
  constructor(cards: Card[], size: number, onlyFromSelectedCards: boolean) {
    this.cards = cards;
    this.size = size;
    this.onlyFromSelectedCards = onlyFromSelectedCards;
  }

  cards: Card[];
  size: number;
  onlyFromSelectedCards: boolean;
}
