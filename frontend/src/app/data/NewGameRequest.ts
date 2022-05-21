export class NewGameRequest {
  constructor(playerNames: string[]) {
    this.playerNames = playerNames;
  }

  playerNames: string[];
}
