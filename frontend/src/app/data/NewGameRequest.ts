import {Expansion} from './Expansion';

export class NewGameRequest {
  constructor(playerNames: string[],
              computers: boolean[],
              extraPoints: number[],
              mulligan: boolean,
              dummyHand: boolean,
              beginner: boolean,
              expansions: Expansion[]) {
    this.playerNames = playerNames;
    this.computers = computers;
    this.extraPoints = extraPoints;
    this.mulligan = mulligan;
    this.dummyHand = dummyHand;
    this.beginner = beginner;
    this.expansions = expansions;
  }

  playerNames: string[];
  computers: boolean[];
  extraPoints: number[];
  mulligan: boolean;
  dummyHand: boolean;
  beginner: boolean;
  expansions: Expansion[];
}
