import {Injectable} from '@angular/core';
import {RestDataSource} from './rest.datasource';
import {Game} from '../data/Game';
import {Observable} from 'rxjs';
import {TurnType} from '../data/TurnType';
import {ActionDto} from '../data/ActionDto';

@Injectable()
export class GameRepository {

  constructor(private dataSource: RestDataSource) {
  }

  getGame(playerUuid: string): Observable<Game> {
    return this.dataSource.getGame(playerUuid);
  }

  pickCorporation(playerUuid: string, corporationId: number): Observable<any> {
    return this.dataSource.pickCorporation(playerUuid, corporationId);
  }

  nextAction(playerUuid: string): Observable<ActionDto> {
    return this.dataSource.nextAction(playerUuid);
  }

  nextTurns(playerUuid: string): Observable<TurnType[]> {
    return this.dataSource.nextTurns(playerUuid);
  }

}
