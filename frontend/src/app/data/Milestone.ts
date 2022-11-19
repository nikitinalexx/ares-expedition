import {MilestoneType} from './MilestoneType';

export class Milestone {
  constructor(type: MilestoneType,
              players: Set<string>,
              playerToValue: Map<string, number>) {
  }

  type: MilestoneType;
  players: Set<string>;
  playerToValue: Map<string, number>;
}
