export class SoloRecordEntity {
  constructor(uuid: string,
              playerName: string,
              victoryPoints: number,
              turnsLeft: number) {
  }

  uuid: string;
  playerName: string;
  victoryPoints: number;
  turnsLeft: number;
}
