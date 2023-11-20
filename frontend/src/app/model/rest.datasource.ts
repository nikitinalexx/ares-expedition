import {Injectable} from '@angular/core';
import {HttpClient, HttpErrorResponse, HttpHeaders} from '@angular/common/http';
import {Observable, throwError} from 'rxjs';
import {catchError, map} from 'rxjs/operators';
import {Card} from '../data/Card';
import {NewGame} from '../data/NewGame';
import {NewGameRequest} from '../data/NewGameRequest';
import {Game} from '../data/Game';
import {TurnType} from '../data/TurnType';
import {ActionsDto} from '../data/ActionsDto';
import {BuildProjectRequest} from '../data/BuildProjectRequest';
import {BlueActionRequest} from '../data/BlueActionRequest';
import {StandardProjectType} from '../data/StandardProjectType';
import {environment} from '../../environments/environment';
import {GameShort} from '../data/GameShort';
import {Lobby} from '../data/Lobby';
import {PlayerReference} from '../data/PlayerReference';
import {Expansion} from '../data/Expansion';
import {CrysisChoiceRequest} from '../data/CrysisChoiceRequest';
import {DiscardCardsRequest} from "../data/DiscardCardsRequest";
import {CrisisRecordEntity} from "../data/CrisisRecordEntity";
import {SoloRecordEntity} from "../data/SoloRecordEntity";
import {RecentGameDto} from "../data/RecentGameDto";
import {CrisisRecordsDto} from "../data/CrisisRecordsDto";

@Injectable()
export class RestDataSource {
  url = environment.baseUrl;

  constructor(private http: HttpClient) {
  }

  getCards(expansions: Expansion[]): Observable<Card[]> {
    return this.sendRequest<Card[]>('POST', this.url + '/projects',
      {expansions}
    );
  }

  getLobby(nickname: string): Observable<Lobby> {
    return this.sendRequest<Lobby>('GET', this.url + '/lobby/' + nickname);
  }

  createGame(requestBody: NewGameRequest): Observable<NewGame> {
    return this.sendRequest<NewGame>('POST', this.url + '/game/new', requestBody);
  }

  createLobbyGame(host: string, mulligan: boolean): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/lobby/game/new',
      {host, mulligan}
    );
  }

  joinLobbyGame(player: string, gameId: number): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/lobby/game/join',
      {player, gameId}
    );
  }

  leaveLobbyGame(player: string, gameId: number): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/lobby/game/leave',
      {player, gameId}
    );
  }

  getLobbyGamePlayerUuid(player: string, gameId: number): Observable<PlayerReference> {
    return this.sendRequest<PlayerReference>('POST', this.url + '/lobby/game/uuid',
      {player, gameId}
    );
  }

  confirmGameStart(player: string, gameId: number): Observable<any> {
    return this.sendRequest<NewGame>('POST', this.url + '/lobby/game/confirm',
      {player, gameId}
    );
  }

  getGame(playerUuid: string): Observable<Game> {
    return this.sendRequest<Game>('GET', this.url + '/game/player/' + playerUuid);
  }

  getShortGame(playerUuid: string): Observable<GameShort> {
    return this.sendRequest<GameShort>('GET', this.url + '/game/short/player/' + playerUuid);
  }

  pickCorporation(playerUuid: string, corporationId: number, inputParams: Map<number, number[]>): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/game/player/corporation',
      {playerUuid, corporationId, inputParams}
    );
  }

  resolveOceanDetrimentTurn(playerUuid: string, inputParams: Map<number, number[]>): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/resolve-ocean-detriment',
      {playerUuid: playerUuid, inputParams: inputParams}
    );
  }

  pickPhase(playerUuid: string, phaseId: number): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/game/player/phase',
      {playerUuid, phaseId}
    );
  }

  skipTurn(playerUuid: string): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/game/player/skip',
      {player: playerUuid}
    );
  }

  confirmGameEnd(playerUuid: string): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/game/player/game-end/confirm',
      {player: playerUuid}
    );
  }

  pickExtraBonus(playerUuid: string): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/pick-extra-bonus',
      {player: playerUuid}
    );
  }

  raiseUnmiRt(playerUuid: string): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/unmi/rt',
      {player: playerUuid}
    );
  }

  sellCards(playerUuid: string, cards: number[]): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/game/player/sell',
      {playerUuid, cards}
    );
  }

  sellVp(playerUuid: string): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/game/player/sell-vp',
      {player: playerUuid}
    );
  }

  crisisDummyChoiceTurn(playerUuid: string, choiceOptions: string[]): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/crisis-dummy-choice',
      {playerUuid: playerUuid, choiceOptions: choiceOptions}
    );
  }

  mulliganCards(playerUuid: string, cards: number[]): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/game/player/mulligan',
      {playerUuid, cards}
    );
  }

  sellCardsFinalTurn(playerUuid: string, cards: number[]): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/game/player/sell/last',
      {playerUuid, cards}
    );
  }

  crisisVpToTokenTurn(playerUuid: string, cards: number[]): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/crisis-vp-to-token',
      {playerUuid, cards}
    );
  }

  nextActions(playerUuid: string): Observable<ActionsDto> {
    return this.sendRequest<ActionsDto>('GET', this.url + '/action/next/' + playerUuid);
  }

  nextTurns(playerUuid: string): Observable<TurnType[]> {
    return this.sendRequest<TurnType[]>('GET', this.url + '/turns/next/' + playerUuid);
  }

  buildGreenProject(requestBody: BuildProjectRequest): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/build/green', requestBody);
  }

  buildBlueRedProject(requestBody: BuildProjectRequest): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/build/blue-red', requestBody);
  }

  crysisImmediateChoice(requestBody: CrysisChoiceRequest): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/crysis-immediate-choice', requestBody);
  }

  crysisPersistentChoice(requestBody: CrysisChoiceRequest): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/crysis-persistent-choice', requestBody);
  }

  crysisPersistentAll(playerUuid: string): Observable<ActionsDto> {
    return this.sendRequest<ActionsDto>(
      'POST',
      this.url + '/turn/crysis-persistent-all',
      {player: playerUuid}
    );
  }

  crysisImmediateAll(playerUuid: string): Observable<ActionsDto> {
    return this.sendRequest<ActionsDto>(
      'POST',
      this.url + '/turn/crysis-immediate-all',
      {player: playerUuid}
    );
  }

  blueAction(requestBody: BlueActionRequest): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/blue/action', requestBody);
  }

  discardCards(playerUuid: string, cards: number[]): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/cards/discard',
      {playerUuid, cards}
    );
  }

  collectIncome(playerUuid: string, doubleCollectProject: number): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/collect-income',
      {playerUuid, cardId: doubleCollectProject}
    );
  }

  draftCards(playerUuid: string): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/draft-cards',
      {player: playerUuid}
    );
  }

  plantForest(playerUuid: string): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/forest',
      {player: playerUuid}
    );
  }

  increaseTemperature(playerUuid: string): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/temperature',
      {player: playerUuid}
    );
  }

  plantsToCrisisToken(playerUuid: string): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/plants-crisis-token',
      {player: playerUuid}
    );
  }

  heatToCrisisToken(playerUuid: string): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/heat-crisis-token',
      {player: playerUuid}
    );
  }

  cardsToCrisisToken(request: DiscardCardsRequest): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/cards-crisis-token', request);
  }

  standardProject(playerUuid: string, type: StandardProjectType): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/standard',
      {playerUuid, type: type.valueOf()}
    );
  }

  exchangeHeat(playerUuid: string, value: number): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/heat-exchange',
      {playerUuid, value}
    );
  }

  getCrisisRecordsByPoints(playerCount: number): Observable<CrisisRecordEntity[]> {
    return this.sendRequest<any>('GET', this.url + '/crisis/records/points?playerCount=' + playerCount);
  }

  getCrisisRecordsByTurns(playerCount: number): Observable<CrisisRecordEntity[]> {
    return this.sendRequest<any>('GET', this.url + '/crisis/records/turns?playerCount=' + playerCount);
  }

  getCrisisRecords(playerCount: number, difficultyLevel:number): Observable<CrisisRecordsDto> {
    return this.sendRequest<any>('GET', this.url + '/crisis/records?playerCount=' + playerCount + '&difficultyLevel=' + difficultyLevel);
  }

  getSoloRecords(): Observable<SoloRecordEntity[]> {
    return this.sendRequest<any>('GET', this.url + '/solo/records');
  }

  getRecentGames(): Observable<RecentGameDto[]> {
    return this.sendRequest<any>('GET', this.url + '/recent').pipe(
      map(responseData => {
        return responseData.map(item => new RecentGameDto(
          item.uuid,
          item.playerName,
          item.playerCount,
          item.victoryPoints,
          item.turns,
          item.date,
          item.isCrisis
        ));
      })
    );
  }


  private sendRequest<T>(verb: string, url: string, body?: any)
    : Observable<T> {

    let myHeaders = new HttpHeaders();
    myHeaders = myHeaders.set('Access-Key', '<secret>');
    myHeaders = myHeaders.set('Application-Names', ['exampleApp', 'proAngular']);

    return this.http.request<T>(verb, url, {
      body,
      headers: myHeaders
    }).pipe(catchError((error: HttpErrorResponse) => {
      console.log(error);
      return throwError(`Error: ${error.error?.message} (${error.status})`);
    }));
  }

}
