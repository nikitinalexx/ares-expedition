import {Injectable} from '@angular/core';
import {RestDataSource} from './rest.datasource';
import {Card} from '../data/Card';
import {NewGame} from '../data/NewGame';
import {NewGameRequest} from '../data/NewGameRequest';
import {Observable} from 'rxjs';

@Injectable()
export class NewGameRepository {
  private projects: Card[] = new Array<Card>();

  constructor(private dataSource: RestDataSource) {
    this.dataSource.getData().subscribe(data => this.projects = data);
  }

  getProjectCards(): Card[] {
    return this.projects;
  }

  createNewGame(request: NewGameRequest): Observable<NewGame> {
    return this.dataSource.createGame(request);
  }

}
