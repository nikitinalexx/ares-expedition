import {Card} from './Card';
import {BasePlayer} from './BasePlayer';

export class Player extends BasePlayer {
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
              cardIncome: number,
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
    super(winPoints, mc, hand, played, heat, plants, mcIncome, heatIncome, plantsIncome,
      steelIncome, titaniumIncome, cardIncome, terraformingRating, forests, phase);
  }

  playerUuid: string;
  corporations: Card[];
  corporationId: number;
  previousPhase: number;
  nextTurn: any;
  cardResources: Map<number, number>;
  activatedBlueCards: number[];
  activatedBlueActionTwice: boolean;
  builtSpecialDesignLastTurn: boolean;
  builtWorkCrewsLastTurn: boolean;
  canBuildAnotherGreenWith9Discount: boolean;

}
