import {Injectable} from '@angular/core';
import {RestDataSource} from './rest.datasource';
import {Game} from '../data/Game';
import {Observable} from 'rxjs';
import {TurnType} from '../data/TurnType';
import {ActionDto} from '../data/ActionDto';
import {BuildProjectRequest} from "../data/BuildProjectRequest";

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

  pickPhase(playerUuid: string, phaseId: number): Observable<any> {
    return this.dataSource.pickPhase(playerUuid, phaseId);
  }

  skipTurn(playerUuid: string): Observable<any> {
    return this.dataSource.skipTurn(playerUuid);
  }

  sellCards(playerUuid: string, cards: number[]): Observable<any> {
    return this.dataSource.sellCards(playerUuid, cards);
  }

  nextAction(playerUuid: string): Observable<ActionDto> {
    return this.dataSource.nextAction(playerUuid);
  }

  nextTurns(playerUuid: string): Observable<TurnType[]> {
    return this.dataSource.nextTurns(playerUuid);
  }

  buildGreenProject(requestBody: BuildProjectRequest): Observable<any> {
    return this.dataSource.buildGreenProject(requestBody);
  }

  buildBlueRedProject(requestBody: BuildProjectRequest): Observable<any> {
    return this.dataSource.buildBlueRedProject(requestBody);
  }

}
