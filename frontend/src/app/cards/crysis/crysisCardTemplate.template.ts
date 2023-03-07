import {Component, Input} from '@angular/core';
import {Game} from '../../data/Game';
import {CrysisCard} from '../../data/CrysisCard';
import {CrysisCardAction} from '../../data/CrysisCardAction';
import {CrysisActiveCardAction} from '../../data/CrysisActiveCardAction';
import {CardTier} from "../../data/CardTier";

@Component({
  selector: 'app-crysis-card-template',
  templateUrl: 'crysisCardTemplate.template.html',
  styleUrls: ['crysisCardTemplate.template.css']
})
export class CrysisCardTemplateComponent {
  @Input()
  card: CrysisCard;
  @Input()
  game: Game;

  reduceOxygenPersistentEffect(): boolean {
    return this.card.cardAction === CrysisCardAction.ATMOSPHERE_RUPTURE
      || this.card.cardAction === CrysisCardAction.DUST_CLOUDS
      || this.card.cardAction === CrysisCardAction.SEISMIC_AFTERSHOCKS
      || this.card.cardAction === CrysisCardAction.IMPACT_DESERT
      || this.card.cardAction === CrysisCardAction.COLLAPSING_CITIES;
  }

  oxygenNumberOfReductions(): string {
    if (this.card.cardAction === CrysisCardAction.IMPACT_DESERT) {
      return '2';
    } else {
      return '';
    }
  }

  temperatureNumberOfReductions(): string {
    if (this.card.cardAction === CrysisCardAction.CROP_FAILURES) {
      return '2';
    } else {
      return '';
    }
  }

  tier4Card(card: CrysisCard): boolean {
    return card.cardTier === CardTier.T4;
  }

  oceanNumberOfReductions(): string {
    if (this.card.cardAction === CrysisCardAction.REGLACIATION) {
      return '2';
    } else {
      return '';
    }
  }

  reduceTemperaturePersistentEffect(): boolean {
    return this.card.cardAction === CrysisCardAction.EMERGENCY_SHELTERS
      || this.card.cardAction === CrysisCardAction.GREENHOUSE_GAS_DEGRADATION
      || this.card.cardAction === CrysisCardAction.DISRUPTED_SUPPLY_LINES
      || this.card.cardAction === CrysisCardAction.CROP_FAILURES;
  }

  closeOceanPersistentEffect(): boolean {
    return this.card.cardAction === CrysisCardAction.CATASTROPHIC_EROSION
      || this.card.cardAction === CrysisCardAction.BIODIVERSITY_LOSS
      || this.card.cardAction === CrysisCardAction.REGLACIATION;
  }

  oceanOrTemperatureEffect(): boolean {
    return this.card.cardAction === CrysisCardAction.BARREN_CRATER;
  }

  oceanOrOxygenEffect(): boolean {
    return this.card.cardAction === CrysisCardAction.ATMOSPHERIC_ESCAPE
      || this.card.cardAction === CrysisCardAction.IONOSPHERIC_TEAR;
  }

  temperatureOrOxygenEffect(): boolean {
    return this.card.cardAction === CrysisCardAction.LOCALIZED_TSUNAMI
      || this.card.cardAction === CrysisCardAction.INFRASTRUCTURE_COLLAPSE;
  }

  playSpaceCardEffect(): boolean {
    return this.card.cardAction === CrysisCardAction.ATMOSPHERE_RUPTURE;
  }

  playPlantCardEffect(): boolean {
    return this.card.cardAction === CrysisCardAction.CROP_FAILURES;
  }

  playEnergyCardEffect(): boolean {
    return this.card.cardAction === CrysisCardAction.LOCALIZED_TSUNAMI;
  }

  playBuildingCardEffect(): boolean {
    return this.card.cardAction === CrysisCardAction.EMERGENCY_SHELTERS;
  }

  playScienceCardEffect(): boolean {
    return this.card.cardAction === CrysisCardAction.IMPACT_DESERT;
  }

  playEventCardEffect(): boolean {
    return this.card.cardAction === CrysisCardAction.ATMOSPHERIC_ESCAPE
      || this.card.cardAction === CrysisCardAction.REGLACIATION;
  }

  playEarthJupiterCardEffect(): boolean {
    return this.card.cardAction === CrysisCardAction.DISRUPTED_SUPPLY_LINES
      || this.card.cardAction === CrysisCardAction.IONOSPHERIC_TEAR;
  }

  playGreenCardEffect(): boolean {
    return this.card.cardAction === CrysisCardAction.CATASTROPHIC_EROSION
      || this.card.cardAction === CrysisCardAction.COLLAPSING_CITIES;
  }

  playBlueCardEffect(): boolean {
    return this.card.cardAction === CrysisCardAction.BARREN_CRATER
      || this.card.cardAction === CrysisCardAction.GREENHOUSE_GAS_DEGRADATION;
  }

  isCrysisActiveCard(): boolean {
    return this.card.crysisActiveCardAction === CrysisActiveCardAction.PLANTS_INTO_TOKENS
      || this.card.crysisActiveCardAction === CrysisActiveCardAction.HEAT_INTO_TOKENS
      || this.card.crysisActiveCardAction === CrysisActiveCardAction.CARDS_INTO_TOKENS
      || this.card.cardAction === CrysisCardAction.INFRASTRUCTURE_COLLAPSE;
  }

  isPlantsIntoTokensAction(): boolean {
    return this.card.crysisActiveCardAction === CrysisActiveCardAction.PLANTS_INTO_TOKENS;
  }

  isHeatIntoTokensAction(): boolean {
    return this.card.crysisActiveCardAction === CrysisActiveCardAction.HEAT_INTO_TOKENS;
  }

  isCardsIntoTokensAction(): boolean {
    return this.card.crysisActiveCardAction === CrysisActiveCardAction.CARDS_INTO_TOKENS;
  }

  isPlayBlueCardAction(): boolean {
    return this.card.cardAction === CrysisCardAction.INFRASTRUCTURE_COLLAPSE;
  }

  isPlayAndDiscardCrisis(): boolean {
    return this.card.cardAction === CrysisCardAction.PLAY_AND_DISCARD_CRYSIS;
  }

  tokenCounter(): number {
    return Object.entries(this.game.crysisDto.cardToTokensCount).find(entry => entry[0] === this.card.id.toString())[1];
  }

  immediateEffectTextStyle(): string {
    const textLength = this.card.immediateEffect.length;
    if (textLength < 30) {
      return 'margin-top: 20px';
    } else if (textLength < 60) {
      return 'margin-top: 10px';
    } else {
      return '';
    }
  }
}
