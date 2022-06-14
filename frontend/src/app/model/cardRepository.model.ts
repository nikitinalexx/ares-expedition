import {Injectable} from '@angular/core';
import {RestDataSource} from './rest.datasource';
import {Card} from '../data/Card';
import {Expansion} from '../data/Expansion';

@Injectable()
export class CardRepository {
  private projects: Card[] = new Array<Card>();
  private improvedCorporations = false;

  constructor(private dataSource: RestDataSource) {
    this.updateProjects();
  }

  improvedCorporationsFlagChanged(value: boolean) {
    this.improvedCorporations = value;
    this.updateProjects();
  }

  updateProjects() {
    const expansions = [Expansion.BASE];
    if (this.improvedCorporations) {
      expansions.push(Expansion.BUFFED_CORPORATION);
    }
    this.dataSource.getCards(expansions).subscribe(data => this.projects = data);
  }

  getProjectCards(): Card[] {
    return this.projects;
  }

}
