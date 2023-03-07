export class CrisisRecordEntity {
  constructor(uuid: string,
              playerName: string,
              victoryPoints: number,
              terraformingPoints: number,
              playerCount: number,
              turnsLeft: number) {
  }

  uuid: string;
  playerName: string;
  victoryPoints: number;
  terraformingPoints: number;
  playerCount: number;
  turnsLeft: number;
}
