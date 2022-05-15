import {CardResource} from './CardResource';

export class WinPointsInfo {
  constructor(type: CardResource, resources: number, points: number) {
  }

  type: CardResource;
  resources: number;
  points: number;
}
