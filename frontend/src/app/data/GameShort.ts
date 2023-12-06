import {Ocean} from './Ocean';
import {BasePlayer} from './BasePlayer';

export class GameShort {
  constructor(temperature: number,
              oxygen: number,
              infrastructure: number,
              oceans: Ocean[],
              phaseTemperature?: number,
              phaseInfrastructure?: number,
              phaseOxygen?: number,
              phaseOceans?: number,
              otherPlayers?: BasePlayer[]) {
  }

  temperature: number;
  oxygen: number;
  infrastructure: number;
  oceans: Ocean[];
  phaseTemperature?: number;
  phaseInfrastructure?: number;
  phaseOxygen?: number;
  phaseOceans?: number;
  otherPlayers: BasePlayer[];
}
