import {Injectable} from '@angular/core';
import {RestDataSource} from './rest.datasource';
import {Game} from '../data/Game';
import {Observable} from 'rxjs';
import {TurnType} from '../data/TurnType';
import {ActionDto} from '../data/ActionDto';
import {BuildProjectRequest} from "../data/BuildProjectRequest";
import {BlueActionRequest} from "../data/BlueActionRequest";
import {StandardProjectType} from "../data/StandardProjectType";

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

  confirmGameEnd(playerUuid: string): Observable<any> {
    return this.dataSource.confirmGameEnd(playerUuid);
  }

  pickCard(playerUuid: string): Observable<any> {
    return this.dataSource.pickCard(playerUuid);
  }

  sellCards(playerUuid: string, cards: number[]): Observable<any> {
    return this.dataSource.sellCards(playerUuid, cards);
  }

  discardCards(playerUuid: string, cards: number[]): Observable<any> {
    return this.dataSource.discardCards(playerUuid, cards);
  }

  collectIncome(playerUuid: string): Observable<any> {
    return this.dataSource.collectIncome(playerUuid);
  }

  draftCards(playerUuid: string): Observable<any> {
    return this.dataSource.draftCards(playerUuid);
  }

  plantForest(playerUuid: string): Observable<any> {
    return this.dataSource.plantForest(playerUuid);
  }

  increaseTemperature(playerUuid: string): Observable<any> {
    return this.dataSource.increaseTemperature(playerUuid);
  }

  standardProject(playerUuid: string, type: StandardProjectType): Observable<any> {
    return this.dataSource.standardProject(playerUuid, type);
  }

  exchangeHeat(playerUuid: string, value: number): Observable<any> {
    return this.dataSource.exchangeHeat(playerUuid, value);
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

  blueAction(requestBody: BlueActionRequest): Observable<any> {
    return this.dataSource.blueAction(requestBody);
  }

}
