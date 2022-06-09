import {Injectable} from '@angular/core';
import {RestDataSource} from './rest.datasource';
import {Card} from '../data/Card';

@Injectable()
export class CardRepository {
  private projects: Card[] = new Array<Card>();

  constructor(private dataSource: RestDataSource) {
    this.dataSource.getCards().subscribe(data => this.projects = data);
  }

  getProjectCards(): Card[] {
    return this.projects;
  }

}
