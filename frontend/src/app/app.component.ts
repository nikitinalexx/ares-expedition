import { Component } from '@angular/core';
import {Model} from './model/repository.model';
import {ProjectCard} from './model/projectCard.model';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'frontend';

  constructor(private model: Model) {
  }

  getProjectCards(): ProjectCard[] {
    return this.model.getProjectCards();
  }
}
