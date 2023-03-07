import {CardTier} from './CardTier';
import {CrysisCardAction} from './CrysisCardAction';
import {CrysisActiveCardAction} from "./CrysisActiveCardAction";

export class CrysisCard {
  constructor(id: number,
              cardTier: CardTier,
              playerCount: number,
              name: string,
              cardAction: CrysisCardAction,
              immediateEffect: string,
              immediateOptions: string[],
              persistentOptions: string[],
              initialTokens: number,
              persistentEffectRequiresChoice: boolean,
              crysisActiveCardAction: CrysisActiveCardAction) {
  }

  id: number;
  cardTier: CardTier;
  playerCount: number;
  name: string;
  cardAction: CrysisCardAction;
  immediateEffect: string;
  immediateOptions: string[];
  persistentOptions: string[];
  initialTokens: number;
  persistentEffectRequiresChoice: boolean;
  crysisActiveCardAction: CrysisActiveCardAction;
}
