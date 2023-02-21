import {Card} from './Card';
import {Tag} from "./Tag";

export class BasePlayer {
  constructor(playerUuid: string,
              name: string,
              winPoints: number,
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
              cardResources: Map<number, number>,
              cardToTag: Map<number, Tag[]>,
              phaseCards: number[],
              phase?: number) {
  }

  playerUuid: string;
  name: string;
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
  phaseCards: number[];
  phase?: number;
  cardResources: Map<number, number>;
  cardToTag: Map<number, Tag[]>;

}
