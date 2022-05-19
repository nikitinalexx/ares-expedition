import {Ocean} from './Ocean';

export class GameShort {
  constructor(temperature: number,
              oxygen: number,
              oceans: Ocean[],
              phaseTemperature?: number,
              phaseOxygen?: number,
              phaseOceans?: number) {
  }

  temperature: number;
  oxygen: number;
  oceans: Ocean[];
  phaseTemperature?: number;
  phaseOxygen?: number;
  phaseOceans?: number;
}
