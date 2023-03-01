import {Card} from './Card';
import {BasePlayer} from './BasePlayer';
import {Tag} from './Tag';
import {Build} from "./Build";

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
              activatedBlueCards: number[],
              phaseCards: number[],
              austellarMilestone: number,
              builds: Build[],
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
  builds: Build[];

}
