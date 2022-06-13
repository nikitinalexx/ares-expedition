import {MilestoneType} from './MilestoneType';

export class Milestone {
  constructor(type: MilestoneType,
              players: Set<string>) {
  }

  type: MilestoneType;
  players: Set<string>;
}
