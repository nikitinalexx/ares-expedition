import {Player} from './Player';
import {Ocean} from './Ocean';
import {BasePlayer} from './BasePlayer';

export class Game {
  constructor(turns: number,
              player: Player,
              phase: number,
              temperature: number,
              oxygen: number,
              oceans: Ocean[],
              phaseTemperature?: number,
              phaseOxygen?: number,
              phaseOceans?: number,
              otherPlayers?: BasePlayer[]) {
  }

  turns: number;
  player: Player;
  phase: number;
  otherPlayers: BasePlayer[];
  temperature: number;
  oxygen: number;
  oceans: Ocean[];
  phaseTemperature?: number;
  phaseOxygen?: number;
  phaseOceans?: number;
}
