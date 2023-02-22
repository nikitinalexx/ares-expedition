import {Card} from './Card';
import {BasePlayer} from './BasePlayer';
import {Tag} from './Tag';

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
              blueActionExtraActivationsLeft: number,
              cardResources: Map<number, number>,
              cardToTag: Map<number, Tag[]>,
              terraformingRating: number,
              winPoints: number,
              forests: number,
              builtSpecialDesignLastTurn: boolean,
              builtWorkCrewsLastTurn: boolean,
              canBuildAnotherGreenWith9Discount: boolean,
              canBuildAnotherGreenWithPrice12: boolean,
              assortedEnterprisesDiscount: boolean,
              selfReplicatingDiscount: boolean,
              mayNiDiscount: boolean,
              canBuildInFirstPhase: number,
              activatedBlueCards: number[],
              phaseCards: number[],
              austellarMilestone: number,
              corporationId?: number,
              phase?: number) {
    super(playerUuid, name, winPoints, mc, hand, played, heat, plants, mcIncome, heatIncome, plantsIncome,
      steelIncome, titaniumIncome, cardIncome, terraformingRating, forests, cardResources, cardToTag,
      phaseCards, austellarMilestone, phase);
  }

  corporations: Card[];
  corporationId: number;
  previousPhase: number;
  nextTurn: any;
  activatedBlueCards: number[];
  blueActionExtraActivationsLeft: number;
  builtSpecialDesignLastTurn: boolean;
  builtWorkCrewsLastTurn: boolean;
  canBuildAnotherGreenWith9Discount: boolean;
  canBuildAnotherGreenWithPrice12: boolean;
  assortedEnterprisesDiscount: boolean;
  selfReplicatingDiscount: boolean;
  mayNiDiscount: boolean;
  canBuildInFirstPhase: number;

}
