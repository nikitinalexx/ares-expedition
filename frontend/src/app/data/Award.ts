import {AwardType} from './AwardType';

export class Award {
  constructor(type: AwardType,
              winPoints: Map<string, number>) {
  }

  type: AwardType;
  winPoints: Map<string, number>;
}
