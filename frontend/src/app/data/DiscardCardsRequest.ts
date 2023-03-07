export class DiscardCardsRequest {
  constructor(playerUuid: string, cards: number[]) {
    this.playerUuid = playerUuid;
    this.cards = cards;
  }

  playerUuid: string;
  cards: number[];

}
