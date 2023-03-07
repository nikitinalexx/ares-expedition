import {DetrimentToken} from './DetrimentToken';
import {CrysisCard} from './CrysisCard';
import {CrisisDummyCard} from './CrisisDummyCard';

export class CrysisDto {
  constructor(detrimentTokens: DetrimentToken[],
              openedCards: CrysisCard[],
              cardToTokensCount: Map<number, number>,
              forbiddenPhases: Map<string, number>,
              currentDummyCards: CrisisDummyCard[],
              chosenDummyPhases: number[],
              wonGame: boolean) {
  }

  detrimentTokens: DetrimentToken[];
  openedCards: CrysisCard[];
  cardToTokensCount: Map<number, number>;
  forbiddenPhases: Map<string, number>;
  currentDummyCards: CrisisDummyCard[];
  chosenDummyPhases: number[];
  wonGame: boolean;
}
