import {Payment} from './Payment';
import {BuildProjectParams} from './BuildProjectParams';

export class BuildProjectRequest {
  constructor(playerUuid: string, cardId: number, payments: Payment[], params?: BuildProjectParams) {
  }

  playerUuid: string;
  cardId: number;
  payments: Payment[];
  params: BuildProjectParams;

}
