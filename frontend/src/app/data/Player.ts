import {Card} from './Card';

export class Player {
  constructor(playerUuid: string,
              corporations: Card[],
              hand: Card[],
              previousPhase: number,
              corporationId?: number,
              phase?: number) {

  }

  playerUuid: string;
  corporations: Card[];
  hand: Card[];
  corporationId: number;
  phase: number;
  previousPhase: number;

}
