export class RecentGameDto {
  uuid: string;
  playerName: string;
  playerCount: number;
  victoryPoints: number;
  turns: number;
  date: string;
  isCrisis: boolean;

  constructor(uuid: string,
              playerName: string,
              playerCount: number,
              victoryPoints: number,
              turns: number,
              date: Date,
              isCrisis: boolean) {
    this.uuid = uuid;
    this.playerName = playerName;
    this.playerCount = playerCount;
    this.victoryPoints = victoryPoints;
    this.turns = turns;
    this.date = date ? RecentGameDto.formatDate(date) : '';
    this.isCrisis = isCrisis;
  }

  private static formatDate(date: Date): string {
    const dateObject = new Date(date);

    return dateObject.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit'
    });
  }
}
