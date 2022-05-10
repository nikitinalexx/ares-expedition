import {Component} from '@angular/core';
import {CardRepository} from '../model/cardRepository.model';
import {ProjectCard} from '../data/ProjectCard';

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
}
