import {Payment} from './Payment';
import {BuildProjectParams} from './BuildProjectParams';

export class BuildProjectRequest {
  constructor(playerUuid: string, cardId: number, payments: Payment[], params?: BuildProjectParams) {
    this.playerUuid = playerUuid;
    this.cardId = cardId;
    this.payments = payments;
    this.params = params;
  }

  playerUuid: string;
  cardId: number;
  payments: Payment[];
  params: BuildProjectParams;

}
