import {Injectable} from '@angular/core';
import {RestDataSource} from './rest.datasource';
import {Game} from '../data/Game';
import {Observable} from 'rxjs';

@Injectable()
export class GameRepository {

  constructor(private dataSource: RestDataSource) {
  }

  getGame(playerUuid: string): Observable<Game> {
    return this.dataSource.getGame(playerUuid);
  }

}
