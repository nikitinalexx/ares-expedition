import {Component} from '@angular/core';
import {CardRepository} from "../../model/cardRepository.model";
import {Card} from "../../data/Card";

@Component({
  selector: 'app-project-cards',
  templateUrl: './experimentalCardService.component.html'
})
export class ExperimentalCardServiceComponent {

  constructor(private model: CardRepository) {
  }

  getExperimentalProjectCards(): Card[] {
    return this.model.getExperimentalProjects();
  }

}
