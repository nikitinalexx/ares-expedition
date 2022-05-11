import {Income} from './income';
import {Tag} from './tag';
import {CardColor} from './CardColor';
import {SpecialEffect} from './SpecialEffect';
import {CardAction} from './CardAction';
import {ParameterColor} from './ParameterColor';
import {OceanRequirement} from './OceanRequirement';

export class ProjectCard {
  constructor(id: number,
              name: string,
              price: number,
              winPoints: number,
              description: string,
              cardColor: CardColor,
              cardAction: CardAction,
              tempReq: ParameterColor[],
              oxygenReq: ParameterColor[],
              oceanRequirement?: OceanRequirement,
              incomes: Income[] = [],
              tags: Tag[] = [],
              specialEffects: SpecialEffect[] = [],
              tagReq: Tag[] = []) {
  }

  id: number;
  name: string;
  price: number;
  winPoints: number;
  description: string;
  cardColor: CardColor;
  cardAction: CardAction;
  incomes: Income[];
  tags: Tag[];
  specialEffects: SpecialEffect[];
  tagReq: Tag[];
  tempReq: ParameterColor[];
  oxygenReq: ParameterColor[];
  oceanRequirement: OceanRequirement;
}
