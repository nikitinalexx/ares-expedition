import {MilestoneType} from './MilestoneType';

export class Milestone {
  constructor(type: MilestoneType,
              players: [],
              playerToValue: Map<string, number>) {
  }

  type: MilestoneType;
  players: [];
  playerToValue: Map<string, number>;
}
