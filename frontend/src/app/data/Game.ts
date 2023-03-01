import {Player} from './Player';
import {Ocean} from './Ocean';
import {BasePlayer} from './BasePlayer';
import {ParameterColor} from './ParameterColor';
import {Milestone} from './Milestone';
import {Award} from './Award';

export class Game {
  constructor(turns: number,
              player: Player,
              phase: number,
              temperature: number,
              oxygen: number,
              oceans: Ocean[],
              phaseTemperatureColor: ParameterColor,
              phaseOxygenColor: ParameterColor,
              awards: Award[],
              milestones: Milestone[],
              dummyHandMode: boolean,
              usedDummyHand: number[],
              phaseTemperature?: number,
              phaseOxygen?: number,
              phaseOceans?: number,
              otherPlayers?: BasePlayer[]) {
  }

  turns: number;
  player: Player;
  phase: number;
  awards: Award[];
  milestones: Milestone[];
  otherPlayers: BasePlayer[];
  temperature: number;
  oxygen: number;
  oceans: Ocean[];
  phaseTemperatureColor: ParameterColor;
  phaseOxygenColor: ParameterColor;
  dummyHandMode: boolean;
  usedDummyHand: number[];
  phaseTemperature?: number;
  phaseOxygen?: number;
  phaseOceans?: number;
}
