export class BlueActionRequest {
  constructor(playerUuid: string, cardId: number, inputParams?: number[]) {
    this.playerUuid = playerUuid;
    this.cardId = cardId;
    this.inputParams = inputParams;
  }

  playerUuid: string;
  cardId: number;
  inputParams: number[];

}
