import {BuildType} from './BuildType';

export class Build {
  constructor(type: BuildType,
              extraDiscount: number,
              priceLimit: number) {
  }

  type: BuildType;
  extraDiscount: number;
  priceLimit: number;
}
