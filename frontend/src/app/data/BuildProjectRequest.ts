import {Payment} from './Payment';

export class BuildProjectRequest {
  constructor(playerUuid: string, cardId: number, payments: Payment[], inputParams?: Map<number, number[]>) {
    this.playerUuid = playerUuid;
    this.cardId = cardId;
    this.payments = payments;
    this.inputParams = inputParams;
  }

  playerUuid: string;
  cardId: number;
  payments: Payment[];
  inputParams: Map<number, number[]>;

}
