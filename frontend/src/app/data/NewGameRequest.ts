import {Expansion} from './Expansion';

export class NewGameRequest {
  constructor(playerNames: string[], mulligan: boolean, expansions: Expansion[]) {
    this.playerNames = playerNames;
    this.mulligan = mulligan;
    this.expansions = expansions;
  }

  playerNames: string[];
  mulligan: boolean;
  expansions: Expansion[];
}
