import {Ocean} from './Ocean';
import {BasePlayer} from './BasePlayer';

export class GameShort {
  constructor(temperature: number,
              oxygen: number,
              oceans: Ocean[],
              phaseTemperature?: number,
              phaseOxygen?: number,
              phaseOceans?: number,
              otherPlayers?: BasePlayer[]) {
  }

  temperature: number;
  oxygen: number;
  oceans: Ocean[];
  phaseTemperature?: number;
  phaseOxygen?: number;
  phaseOceans?: number;
  otherPlayers: BasePlayer[];
}
