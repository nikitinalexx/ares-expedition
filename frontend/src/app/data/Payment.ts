import {PaymentType} from './PaymentType';

export class Payment {
  constructor(value: number, type: PaymentType) {
  }

  value: number;
  type: PaymentType;
}
