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

  discoveryExpansionClicked($event: any) {
    this.model.discoveryExpansionFlagChanged($event.target.checked);
  }

  infrastructureExpansionClicked($event: any) {
    this.model.infrastructureExpansionFlagChanged($event.target.checked);
  }

}
