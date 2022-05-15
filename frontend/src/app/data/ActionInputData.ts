import {ActionInputDataType} from './ActionInputDataType';

export class ActionInputData {
  constructor(type: ActionInputDataType, min: number, max: number) {
    this.type = type;
    this.min = min;
    this.max = max;
  }

  type: ActionInputDataType;
  min: number;
  max: number;
}
