import {Player} from './Player';
import {Ocean} from './Ocean';
import {BasePlayer} from './BasePlayer';
import {ParameterColor} from './ParameterColor';
import {Milestone} from './Milestone';
import {Award} from './Award';
import {CrysisDto} from './CrysisDto';

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
              stateReason: string,
              aiComputer: boolean,
              winProbability: number,
              infrastructure?: number,
              phaseInfrastructure?: number,
              phaseTemperature?: number,
              phaseOxygen?: number,
              phaseOceans?: number,
              otherPlayers?: BasePlayer[],
              crysisDto?: CrysisDto) {
  }

  turns: number;
  player: Player;
  phase: number;
  awards: Award[];
  milestones: Milestone[];
  otherPlayers: BasePlayer[];
  temperature: number;
  oxygen: number;
  infrastructure?: number;
  phaseInfrastructure?: number;
  oceans: Ocean[];
  phaseTemperatureColor: ParameterColor;
  phaseOxygenColor: ParameterColor;
  dummyHandMode: boolean;
  usedDummyHand: number[];
  stateReason: string;
  phaseTemperature?: number;
  phaseOxygen?: number;
  phaseOceans?: number;
  crysisDto?: CrysisDto;
  aiComputer: boolean;
  winProbability: number;
}
