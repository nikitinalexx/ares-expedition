import {Player} from './Player';
import {Ocean} from './Ocean';
import {AnotherPlayer} from './AnotherPlayer';

export class Game {
  constructor(player: Player,
              phase: number,
              temperature: number,
              oxygen: number,
              oceans: Ocean[],
              phaseTemperature?: number,
              phaseOxygen?: number,
              phaseOceans?: number,
              otherPlayers?: AnotherPlayer[]) {
  }

  player: Player;
  phase: number;
  otherPlayers: AnotherPlayer[];
  temperature: number;
  oxygen: number;
  oceans: Ocean[];
  phaseTemperature?: number;
  phaseOxygen?: number;
  phaseOceans?: number;
}
