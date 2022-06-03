export class NewGameRequest {
  constructor(playerNames: string[], mulligan: boolean) {
    this.playerNames = playerNames;
    this.mulligan = mulligan;
  }

  playerNames: string[];
  mulligan: boolean;
}
