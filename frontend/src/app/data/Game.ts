import {Player} from './Player';

export class Game {
  constructor(player: Player,
              otherPlayers?: Player[]) {
  }

  player: Player;
  otherPlayers: Player[];
}
