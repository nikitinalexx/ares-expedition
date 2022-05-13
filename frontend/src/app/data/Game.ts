import {Player} from './Player';

export class Game {
  constructor(player: Player,
              phase: number,
              otherPlayers?: Player[]) {
  }

  player: Player;
  phase: number;
  otherPlayers: Player[];
}
