import {Component, Input} from '@angular/core';
import {BasePlayer} from '../data/BasePlayer';
import {Game} from '../data/Game';
import {Tag} from '../data/Tag';
import {CardAction} from "../data/CardAction";

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./../game/game.component.css']
})
export class NavbarComponent {

  @Input()
  game: Game;
  @Input()
  player: BasePlayer;

  constructor() {

  }

  hasHeatEarthIncome(): boolean {
    return this.hasCardAction(CardAction.HEAT_EARTH_INCOME);
  }

  hasHeatEnergyIncome(): boolean {
    return this.hasCardAction(CardAction.HEAT_ENERGY_INCOME);
  }

  hasHeatSpaceIncome(): boolean {
    return this.hasCardAction(CardAction.HEAT_SPACE_INCOME);
  }

  hasMcSpaceIncome(): boolean {
    return this.hasCardAction(CardAction.MC_SPACE_INCOME);
  }

  hasCardScienceIncome(): boolean {
    return this.hasCardAction(CardAction.CARD_SCIENCE_INCOME);
  }

  hasMcEarthIncome(): boolean {
    return this.hasCardAction(CardAction.MC_EARTH_INCOME);
  }

  hasMcEventIncome(): boolean {
    return this.hasCardAction(CardAction.MC_EVENT_INCOME);
  }

  hasMcPlantIncome(): boolean {
    return this.hasCardAction(CardAction.MC_ANIMAL_PLANT_INCOME);
  }

  hasMcForestIncome(): boolean {
    return this.hasCardAction(CardAction.MC_FOREST_INCOME);
  }

  hasAnimalPlantIncome(): boolean {
    return this.hasCardAction(CardAction.MC_ANIMAL_PLANT_INCOME);
  }

  hasPlantPlantIncome(): boolean {
    return this.hasCardAction(CardAction.PLANT_PLANT_INCOME);
  }

  hasPlantMicrobeIncome(): boolean {
    return this.hasCardAction(CardAction.PLANT_MICROBE_INCOME);
  }

  hasMcBuildingIncome(): boolean {
    return this.hasCardAction(CardAction.MC_2_BUILDING_INCOME);
  }

  hasMcEnergyIncome(): boolean {
    return this.hasCardAction(CardAction.MC_ENERGY_INCOME);
  }

  hasMcScienceIncome(): boolean {
    return this.hasCardAction(CardAction.MC_SCIENCE_INCOME);
  }

  hasEnergyDiscount(): boolean {
    return this.hasCardAction(CardAction.THORGATE_CORPORATION) || this.hasCardAction(CardAction.ENERGY_SUBSIDIES);
  }

  hasEarthDiscount(): boolean {
    return this.hasCardAction(CardAction.TERACTOR_CORPORATION) || this.hasCardAction(CardAction.INTERPLANETARY_CONFERENCE);
  }

  hasJupiterDiscount(): boolean {
    return this.hasCardAction(CardAction.INTERPLANETARY_CONFERENCE);
  }

  hasEventDiscount(): boolean {
    return this.hasCardAction(CardAction.MEDIA_GROUP);
  }

  getEnergyDiscount(): number {
    let discount = 0;
    if (this.hasCardAction(CardAction.THORGATE_CORPORATION)) {
      discount -= 3;
    }
    if (this.hasCardAction(CardAction.ENERGY_SUBSIDIES)) {
      discount -= 4;
    }
    return discount;
  }

  getEarthDiscount(): number {
    let discount = 0;
    if (this.hasCardAction(CardAction.TERACTOR_CORPORATION)) {
      discount -= 3;
    }
    if (this.hasCardAction(CardAction.INTERPLANETARY_CONFERENCE)) {
      discount -= 3;
    }
    return discount;
  }

  getJupiterDiscount(): number {
    if (this.hasCardAction(CardAction.INTERPLANETARY_CONFERENCE)) {
      return -3;
    }
    return 0;
  }

  getEventDiscount(): number {
    if (this.hasCardAction(CardAction.MEDIA_GROUP)) {
      return -5;
    }
    return 0;
  }

  countMcEarthIncome(): number {
    return this.player.played.filter(
      card => card.cardAction && card.cardAction === CardAction.MC_EARTH_INCOME
    ).length;
  }

  hasCardAction(type: CardAction): boolean {
    return this.player?.played.some(
      card => card.cardAction && card.cardAction === type
    );
  }

  chosenPhases(): number[] {
    const phases = [];
    for (let i = 1; i <= 5; i++) {
      if (this.phaseChosenByAnyone(i)) {
        phases.push(i);
      }
    }
    return phases;
  }

  phaseChosenByAnyone(phase: number): boolean {
    return this.game?.player.phase === phase ||
      this.game?.otherPlayers?.some(p => p.phase === phase);
  }

  phaseDisplayStyles(phase: number): string {
    let result = '';
    if (!this.currentPhase(phase)) {
      result += 'phase-transparent';
    }

    if (this.player?.phase === phase) {
      result += ' selectedPhase';
    }

    return result;
  }

  getUniqueTags() {
    const tags = this.player?.played
      .map(card => {
        const tagsWithDynamic = [];
        for (let i = 0; i < card.tags.length; i++) {
          if (card.tags[i] !== Tag.DYNAMIC) {
            tagsWithDynamic.push(card.tags[i]);
          } else if (this.player.cardToTag[card.id][i] !== Tag.DYNAMIC) {
            tagsWithDynamic.push(this.player.cardToTag[card.id][i]);
          }
        }
        return tagsWithDynamic;
      })
      .reduce((acc, val) => acc.concat(val), []);

    if (!tags) {
      return [];
    }

    const result = tags?.reduce((a, c) => (a.set(c, (a.get(c) || 0) + 1), a), new Map<Tag, number>());
    return Array.from(result?.entries()).sort((a, b) => b[1] - a[1]);
  }

  currentPhase(phase: number): boolean {
    return this.game?.phase === phase;
  }

}
