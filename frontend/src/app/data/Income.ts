import {IncomeType} from './IncomeType';

export class Income {
  constructor(type: IncomeType, value: number) {
    this.type = type;
    this.value = value;
  }

  type: IncomeType;
  value: number;
}
