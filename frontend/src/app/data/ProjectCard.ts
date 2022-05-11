import {Income} from './income';
import {Tag} from './tag';
import {CardColor} from './CardColor';
import {SpecialEffect} from './SpecialEffect';

export class ProjectCard {
  constructor(id: number,
              name: string,
              price: number,
              winPoints: number,
              description: string,
              cardColor: CardColor,
              incomes: Income[] = [],
              tags: Tag[] = [],
              specialEffects: SpecialEffect[] = []) {
  }

  id: number;
  name: string;
  price: number;
  winPoints: number;
  description: string;
  cardColor: CardColor;
  incomes: Income[];
  tags: Tag[];
  specialEffects: SpecialEffect[];
}
