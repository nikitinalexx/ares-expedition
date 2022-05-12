export class NewGameRequest {
  constructor(playersCount: number) {
    this.playersCount = playersCount;
  }

  playersCount: number;
}
