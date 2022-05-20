import {Card} from "./Card";

export class BasePlayer {
  constructor(winPoints: number,
              mc: number,
              hand: Card[],
              played: Card[],
              heat: number,
              plants: number,
              mcIncome: number,
              heatIncome: number,
              plantsIncome: number,
              steelIncome: number,
              titaniumIncome: number,
              cardIncome: number,
              terraformingRating: number,
              forests: number,
              phase?: number) {

  }

  winPoints: number;
  mc: number;
  heat: number;
  plants: number;
  mcIncome: number;
  heatIncome: number;
  plantsIncome: number;
  steelIncome: number;
  titaniumIncome: number;
  cardIncome: number;
  terraformingRating: number;
  forests: number;
  hand: Card[];
  played: Card[];
  phase?: number;

}
