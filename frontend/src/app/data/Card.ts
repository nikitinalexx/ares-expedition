import {Tag} from './Tag';
import {CardColor} from './CardColor';
import {SpecialEffect} from './SpecialEffect';
import {CardAction} from './CardAction';
import {ParameterColor} from './ParameterColor';
import {OceanRequirement} from './OceanRequirement';
import {Gain} from './Gain';
import {WinPointsInfo} from './WinPointsInfo';
import {CardResource} from './CardResource';
import {PutResourceOnBuild} from './PutResourceOnBuild';
import {ActionInputData} from './ActionInputData';
import {Expansion} from './Expansion';

export class Card {
  constructor(id: number,
              name: string,
              price: number,
              winPoints: number,
              description: string,
              actionDescription: string,
              cardColor: CardColor,
              cardAction: CardAction,
              bonuses: Gain[],
              incomes: Gain[],
              tempReq: ParameterColor[],
              oxygenReq: ParameterColor[],
              infrastructureReq: ParameterColor[],
              cardResource: CardResource,
              resourcesOnBuild: PutResourceOnBuild[],
              active: boolean,
              corporation: boolean,
              expansion: Expansion,
              actionInputData?: ActionInputData[],
              winPointsInfo?: WinPointsInfo,
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
  actionDescription: string;
  cardColor: CardColor;
  cardAction: CardAction;
  bonuses: Gain[];
  incomes: Gain[];
  tags: Tag[];
  specialEffects: SpecialEffect[];
  tagReq: Tag[];
  tempReq: ParameterColor[];
  oxygenReq: ParameterColor[];
  infrastructureReq: ParameterColor[];
  oceanRequirement: OceanRequirement;
  winPointsInfo: WinPointsInfo;
  cardResource: CardResource;
  resourcesOnBuild: PutResourceOnBuild[];
  active: boolean;
  actionInputData: ActionInputData[];
  corporation: boolean;
  expansion: Expansion;
}
