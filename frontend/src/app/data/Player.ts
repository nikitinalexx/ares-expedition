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
              activatedBlueActionTwice: boolean,
              cardResources: Map<number, number>,
              terraformingRating: number,
              winPoints: number,
              forests: number,
              builtSpecialDesignLastTurn: boolean,
              builtWorkCrewsLastTurn: boolean,
              canBuildAnotherGreenWith9Discount: boolean,
              activatedBlueCards: number[],
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
  activatedBlueCards: number[];
  activatedBlueActionTwice: boolean;
  terraformingRating: number;
  winPoints: number;
  forests: number;
  builtSpecialDesignLastTurn: boolean;
  builtWorkCrewsLastTurn: boolean;
  canBuildAnotherGreenWith9Discount: boolean;

}
