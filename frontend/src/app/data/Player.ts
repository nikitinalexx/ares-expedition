import {Card} from './Card';

export class Player {
  constructor(playerUuid: string,
              corporations: Card[],
              hand: Card[],
              played: Card[],
              previousPhase: number,
              mc: number,
              heat: number,
              plants: number,
              mcIncome: number,
              heatIncome: number,
              plantsIncome: number,
              steelIncome: number,
              nextTurn: any,
              titaniumIncome: number,
              cardResources: Map<number, number>,
              corporationId?: number,
              phase?: number) {

  }

  playerUuid: string;
  corporations: Card[];
  hand: Card[];
  played: Card[];
  corporationId: number;
  phase: number;
  previousPhase: number;
  mc: number;
  heat: number;
  plants: number;
  mcIncome: number;
  heatIncome: number;
  plantsIncome: number;
  cardIncome: number;
  steelIncome: number;
  titaniumIncome: number;
  nextTurn: any;
  cardResources: Map<number, number>;

}
