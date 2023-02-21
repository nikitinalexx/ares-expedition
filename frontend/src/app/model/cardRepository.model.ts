import {Injectable} from '@angular/core';
import {RestDataSource} from './rest.datasource';
import {Card} from '../data/Card';
import {Expansion} from '../data/Expansion';

@Injectable()
export class CardRepository {
  private projects: Card[] = new Array<Card>();
  private improvedCorporations = false;
  private discoveryExpansion = true;

  constructor(private dataSource: RestDataSource) {
    this.updateProjects();
  }

  improvedCorporationsFlagChanged(value: boolean) {
    this.improvedCorporations = value;
    this.updateProjects();
  }

  discoveryExpansionFlagChanged(value: boolean) {
    this.discoveryExpansion = value;
    this.updateProjects();
  }

  updateProjects() {
    const expansions = [Expansion.BASE];
    if (this.improvedCorporations) {
      expansions.push(Expansion.BUFFED_CORPORATION);
    }
    if (this.discoveryExpansion) {
      expansions.push(Expansion.DISCOVERY);
    }
    this.dataSource.getCards(expansions).subscribe(data => this.projects = data);
  }

  getProjectCards(): Card[] {
    return this.projects;
  }

}
