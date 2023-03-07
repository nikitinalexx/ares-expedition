export class CrysisChoiceRequest {
  constructor(playerUuid: string, cardId: number, inputParams?: Map<number, number[]>) {
    this.playerUuid = playerUuid;
    this.cardId = cardId;
    this.inputParams = inputParams;
  }

  playerUuid: string;
  cardId: number;
  inputParams: Map<number, number[]>;

}
