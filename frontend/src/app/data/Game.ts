import {Player} from './Player';
import {Ocean} from './Ocean';

export class Game {
  constructor(player: Player,
              phase: number,
              temperature: number,
              oxygen: number,
              oceans: Ocean[],
              otherPlayers?: Player[]) {
  }

  player: Player;
  phase: number;
  otherPlayers: Player[];
  temperature: number;
  oxygen: number;
  oceans: Ocean[];
}
