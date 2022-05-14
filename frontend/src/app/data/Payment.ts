import {PaymentType} from './PaymentType';

export class Payment {
  constructor(value: number, type: PaymentType) {
    this.value = value;
    this.type = type;
  }

  value: number;
  type: PaymentType;
}
