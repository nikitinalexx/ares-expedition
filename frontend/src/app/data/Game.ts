import {Player} from './Player';
import {Ocean} from './Ocean';
import {BasePlayer} from './BasePlayer';
import {ParameterColor} from "./ParameterColor";

export class Game {
  constructor(turns: number,
              player: Player,
              phase: number,
              temperature: number,
              oxygen: number,
              oceans: Ocean[],
              phaseTemperatureColor: ParameterColor,
              phaseOxygenColor: ParameterColor,
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
  phaseTemperatureColor: ParameterColor;
  phaseOxygenColor: ParameterColor;
  phaseTemperature?: number;
  phaseOxygen?: number;
  phaseOceans?: number;
}
