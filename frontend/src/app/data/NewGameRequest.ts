import {Expansion} from './Expansion';

export class NewGameRequest {
  constructor(playerNames: string[], computers: boolean[], mulligan: boolean, expansions: Expansion[]) {
    this.playerNames = playerNames;
    this.computers = computers;
    this.mulligan = mulligan;
    this.expansions = expansions;
  }

  playerNames: string[];
  computers: boolean[];
  mulligan: boolean;
  expansions: Expansion[];
}
