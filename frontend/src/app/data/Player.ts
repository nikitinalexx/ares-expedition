import {Card} from './Card';
import {BasePlayer} from './BasePlayer';

export class Player extends BasePlayer {
  constructor(name: string,
              playerUuid: string,
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
              assortedEnterprisesDiscount: boolean,
              selfReplicatingDiscount: boolean,
              mayNiDiscount: boolean,
              activatedBlueCards: number[],
              corporationId?: number,
              phase?: number) {
    super(playerUuid, name, winPoints, mc, hand, played, heat, plants, mcIncome, heatIncome, plantsIncome,
      steelIncome, titaniumIncome, cardIncome, terraformingRating, forests, cardResources, phase);
  }

  corporations: Card[];
  corporationId: number;
  previousPhase: number;
  nextTurn: any;
  activatedBlueCards: number[];
  activatedBlueActionTwice: boolean;
  builtSpecialDesignLastTurn: boolean;
  builtWorkCrewsLastTurn: boolean;
  canBuildAnotherGreenWith9Discount: boolean;
  assortedEnterprisesDiscount: boolean;
  selfReplicatingDiscount: boolean;
  mayNiDiscount: boolean;

}
