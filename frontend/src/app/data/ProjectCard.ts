import {Tag} from './tag';
import {CardColor} from './CardColor';
import {SpecialEffect} from './SpecialEffect';
import {CardAction} from './CardAction';
import {ParameterColor} from './ParameterColor';
import {OceanRequirement} from './OceanRequirement';
import {Gain} from './Gain';

export class ProjectCard {
  constructor(id: number,
              name: string,
              price: number,
              winPoints: number,
              description: string,
              cardColor: CardColor,
              cardAction: CardAction,
              bonuses: Gain[],
              incomes: Gain[],
              tempReq: ParameterColor[],
              oxygenReq: ParameterColor[],
              oceanRequirement?: OceanRequirement,
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
  bonuses: Gain[];
  incomes: Gain[];
  tags: Tag[];
  specialEffects: SpecialEffect[];
  tagReq: Tag[];
  tempReq: ParameterColor[];
  oxygenReq: ParameterColor[];
  oceanRequirement: OceanRequirement;
}
