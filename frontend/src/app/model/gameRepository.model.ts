import {Injectable} from '@angular/core';
import {RestDataSource} from './rest.datasource';
import {Game} from '../data/Game';
import {Observable} from 'rxjs';
import {TurnType} from '../data/TurnType';
import {ActionsDto} from '../data/ActionsDto';
import {BuildProjectRequest} from "../data/BuildProjectRequest";
import {BlueActionRequest} from "../data/BlueActionRequest";
import {StandardProjectType} from "../data/StandardProjectType";
import {GameShort} from "../data/GameShort";
import {CrysisChoiceRequest} from "../data/CrysisChoiceRequest";
import {DiscardCardsRequest} from "../data/DiscardCardsRequest";
import {CrisisRecordEntity} from "../data/CrisisRecordEntity";

@Injectable()
export class GameRepository {

  constructor(private dataSource: RestDataSource) {
  }

  getGame(playerUuid: string): Observable<Game> {
    return this.dataSource.getGame(playerUuid);
  }

  getShortGame(playerUuid: string): Observable<GameShort> {
    return this.dataSource.getShortGame(playerUuid);
  }

  pickCorporation(playerUuid: string, corporationId: number, inputParams: Map<number, number[]>): Observable<any> {
    return this.dataSource.pickCorporation(playerUuid, corporationId, inputParams);
  }

  resolveOceanDetrimentTurn(playerUuid: string, inputParams: Map<number, number[]>): Observable<any> {
    return this.dataSource.resolveOceanDetrimentTurn(playerUuid, inputParams);
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

  pickExtraBonus(playerUuid: string): Observable<any> {
    return this.dataSource.pickExtraBonus(playerUuid);
  }

  raiseUnmiRt(playerUuid: string): Observable<any> {
    return this.dataSource.raiseUnmiRt(playerUuid);
  }

  sellVp(playerUuid: string): Observable<any> {
    return this.dataSource.sellVp(playerUuid);
  }

  crisisDummyChoiceTurn(playerUuid: string, choiceOptions: string[]): Observable<any> {
    return this.dataSource.crisisDummyChoiceTurn(playerUuid, choiceOptions);
  }

  sellCards = (playerUuid: string, cards: number[]) => {
    return this.dataSource.sellCards(playerUuid, cards);
  }

  sellCardsFinalTurn = (playerUuid: string, cards: number[]) => {
    return this.dataSource.sellCardsFinalTurn(playerUuid, cards);
  }

  mulliganCards = (playerUuid: string, cards: number[]) => {
    return this.dataSource.mulliganCards(playerUuid, cards);
  }

  discardCards(playerUuid: string, cards: number[]): Observable<any> {
    return this.dataSource.discardCards(playerUuid, cards);
  }

  crisisVpToTokenTurn(playerUuid: string, cards: number[]): Observable<any> {
    return this.dataSource.crisisVpToTokenTurn(playerUuid, cards);
  }

  collectIncome(playerUuid: string, doubleCollectProject: number): Observable<any> {
    return this.dataSource.collectIncome(playerUuid, doubleCollectProject);
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

  plantsToCrisisToken(playerUuid: string): Observable<any> {
    return this.dataSource.plantsToCrisisToken(playerUuid);
  }

  heatToCrisisToken(playerUuid: string): Observable<any> {
    return this.dataSource.heatToCrisisToken(playerUuid);
  }

  cardsToCrisisToken(request: DiscardCardsRequest): Observable<any> {
    return this.dataSource.cardsToCrisisToken(request);
  }

  standardProject(playerUuid: string, type: StandardProjectType): Observable<any> {
    return this.dataSource.standardProject(playerUuid, type);
  }

  exchangeHeat(playerUuid: string, value: number): Observable<any> {
    return this.dataSource.exchangeHeat(playerUuid, value);
  }

  getCrisisRecords(playerCount: number): Observable<CrisisRecordEntity[]> {
    return this.dataSource.getCrisisRecords(playerCount);
  }

  nextActions(playerUuid: string): Observable<ActionsDto> {
    return this.dataSource.nextActions(playerUuid);
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

  crysisImmediateChoice(requestBody: CrysisChoiceRequest): Observable<any> {
    return this.dataSource.crysisImmediateChoice(requestBody);
  }

  crysisPersistentChoice(requestBody: CrysisChoiceRequest): Observable<any> {
    return this.dataSource.crysisPersistentChoice(requestBody);
  }

  crysisPersistentAll(playerUuid: string): Observable<any> {
    return this.dataSource.crysisPersistentAll(playerUuid);
  }

  crysisImmediateAll(playerUuid: string): Observable<any> {
    return this.dataSource.crysisImmediateAll(playerUuid);
  }

  blueAction(requestBody: BlueActionRequest): Observable<any> {
    return this.dataSource.blueAction(requestBody);
  }

}
