import {Injectable} from '@angular/core';
import {RestDataSource} from './rest.datasource';
import {NewGame} from '../data/NewGame';
import {NewGameRequest} from '../data/NewGameRequest';
import {Observable} from 'rxjs';

@Injectable()
export class NewGameRepository {

  constructor(private dataSource: RestDataSource) {
  }

  createNewGame(request: NewGameRequest): Observable<NewGame> {
    return this.dataSource.createGame(request);
  }

}
