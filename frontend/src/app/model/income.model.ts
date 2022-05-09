import {IncomeType} from './incomeType.model';

export class Income {
  constructor(type: IncomeType, value: number) {
    this.type = type;
    this.value = value;
  }

  type: IncomeType;
  value: number;
}
