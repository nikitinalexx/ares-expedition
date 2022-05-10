import {Income} from './income';
import {Tag} from './tag';

export class ProjectCard {
  constructor(name: string, price: number, description: string, incomes: Income[] = [], tags: Tag[] = []) {
    this.name = name;
    this.price = price;
    this.tags = tags;
  }

  name: string;
  price: number;
  tags: Tag[];
  description: string;
}
