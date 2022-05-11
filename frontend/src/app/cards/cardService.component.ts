import {Component} from '@angular/core';
import {CardRepository} from '../model/cardRepository.model';
import {ProjectCard} from '../data/ProjectCard';
import {CardColor} from "../data/CardColor";
import {SpecialEffect} from "../data/SpecialEffect";

@Component({
  selector: 'app-project-cards',
  templateUrl: './cardService.component.html'
})
export class CardServiceComponent {

  constructor(private model: CardRepository) {
  }

  getProjectCards(): ProjectCard[] {
    return this.model.getProjectCards();
  }

  getTagClasses(card: ProjectCard, tagNumber: number): string {
    if (card.tags[tagNumber]) {
      return 'tag-' + card.tags[tagNumber].toString().toLowerCase();
    }
  }

  getBackgroundColorClass(card: ProjectCard): string {
    switch (card.cardColor) {
      case CardColor.BLUE:
        return 'background-color-active';
      case CardColor.GREEN:
        return 'background-color-automated';
      case CardColor.RED:
        return 'background-color-events';
    }
  }

  getFormattedId(card: ProjectCard): string {
    return ('000' + card.id).substr(-3);
  }

  hasAmplifyGlobalRequirements(card: ProjectCard): boolean {
    return this.hasSpecialEffect(card, 0);
  }

  hasSpecialEffect(card: ProjectCard, effectIndex: number): boolean {
    return card.specialEffects.indexOf(SpecialEffect[effectIndex]) > -1;
  }
}
