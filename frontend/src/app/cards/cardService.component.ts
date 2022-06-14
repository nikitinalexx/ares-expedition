import {Component} from '@angular/core';
import {CardRepository} from '../model/cardRepository.model';
import {Card} from '../data/Card';

@Component({
  selector: 'app-project-cards',
  templateUrl: './cardService.component.html'
})
export class CardServiceComponent {

  constructor(private model: CardRepository) {
  }

  getProjectCards(): Card[] {
    return this.model.getProjectCards();
  }

  improvedCorporationsClicked($event: any) {
    this.model.improvedCorporationsFlagChanged($event.target.checked);
  }

}
