import {Injectable} from '@angular/core';
import {HttpClient, HttpErrorResponse, HttpHeaders} from '@angular/common/http';
import {Observable, throwError} from 'rxjs';
import {catchError} from 'rxjs/operators';
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
import {GameShort} from "../data/GameShort";


@Injectable()
export class RestDataSource {
  url = environment.baseUrl;

  constructor(private http: HttpClient) {
  }

  getData(): Observable<Card[]> {
    return this.sendRequest<Card[]>('GET', this.url + '/projects');
  }

  createGame(requestBody: NewGameRequest): Observable<NewGame> {
    return this.sendRequest<NewGame>('POST', this.url + '/game/new', requestBody);
  }

  getGame(playerUuid: string): Observable<Game> {
    return this.sendRequest<Game>('GET', this.url + '/game/player/' + playerUuid);
  }

  getShortGame(playerUuid: string): Observable<GameShort> {
    return this.sendRequest<GameShort>('GET', this.url + '/game/short/player/' + playerUuid);
  }

  pickCorporation(playerUuid: string, corporationId: number): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/game/player/corporation',
      {playerUuid: playerUuid, corporationId: corporationId}
    );
  }

  pickPhase(playerUuid: string, phaseId: number): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/game/player/phase',
      {playerUuid: playerUuid, phaseId: phaseId}
    );
  }

  skipTurn(playerUuid: string): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/game/player/skip',
      {playerUuid: playerUuid}
    );
  }

  confirmGameEnd(playerUuid: string): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/game/player/game-end/confirm',
      {playerUuid: playerUuid}
    );
  }

  pickCard(playerUuid: string): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/pick-card',
      {playerUuid: playerUuid}
    );
  }

  sellCards(playerUuid: string, cards: number[]): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/game/player/sell',
      {playerUuid: playerUuid, cards: cards}
    );
  }

  sellCardsFinalTurn(playerUuid: string, cards: number[]): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/game/player/sell/last',
      {playerUuid: playerUuid, cards: cards}
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

  blueAction(requestBody: BlueActionRequest): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/blue/action', requestBody);
  }

  discardCards(playerUuid: string, cards: number[]): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/cards/discard',
      {playerUuid: playerUuid, cards: cards}
    );
  }

  discardDraftedCards(playerUuid: string, cards: number[]): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/cards/discard/drafted',
      {playerUuid: playerUuid, cards: cards}
    );
  }

  collectIncome(playerUuid: string): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/collect-income',
      {playerUuid: playerUuid}
    );
  }

  draftCards(playerUuid: string): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/draft-cards',
      {playerUuid: playerUuid}
    );
  }

  plantForest(playerUuid: string): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/forest',
      {playerUuid: playerUuid}
    );
  }

  increaseTemperature(playerUuid: string): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/temperature',
      {playerUuid: playerUuid}
    );
  }

  standardProject(playerUuid: string, type: StandardProjectType): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/standard',
      {playerUuid: playerUuid, type: type.valueOf()}
    );
  }

  exchangeHeat(playerUuid: string, value: number): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/heat-exchange',
      {playerUuid: playerUuid, value: value}
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
