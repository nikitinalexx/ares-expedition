import {Card} from './Card';

export class Player {
  constructor(corporations: Card[],
              hand: Card[],
              corporationId?: number,
              phase?: number) {

  }

  corporations: Card[];
  hand: Card[];
  corporationId: number;
  phase: number;

}
