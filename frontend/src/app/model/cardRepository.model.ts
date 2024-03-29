import {Injectable} from '@angular/core';
import {RestDataSource} from './rest.datasource';
import {Card} from '../data/Card';
import {Expansion} from '../data/Expansion';

@Injectable()
export class CardRepository {
  private projects: Card[] = new Array<Card>();
  private experimentalProjects: Card[] = new Array<Card>();
  private improvedCorporations = false;
  private discoveryExpansion = true;
  private infrastructureExpansion = true;

  constructor(private dataSource: RestDataSource) {
    this.updateProjects();
    this.updateExperimentalProjects();
  }

  improvedCorporationsFlagChanged(value: boolean) {
    this.improvedCorporations = value;
    this.updateProjects();
  }

  discoveryExpansionFlagChanged(value: boolean) {
    this.discoveryExpansion = value;
    this.updateProjects();
  }

  infrastructureExpansionFlagChanged(value: boolean) {
    this.infrastructureExpansion = value;
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
    if (this.infrastructureExpansion) {
      expansions.push(Expansion.INFRASTRUCTURE);
    }
    this.dataSource.getCards(expansions).subscribe(data => this.projects = data);
  }

  updateExperimentalProjects() {
    this.dataSource.getExperimentalCards().subscribe(data => this.experimentalProjects = data);
  }

  getProjectCards(): Card[] {
    return this.projects;
  }

  getExperimentalProjects(): Card[] {
    return this.experimentalProjects;
  }

}
