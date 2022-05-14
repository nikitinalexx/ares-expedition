import {Inject, Injectable, InjectionToken} from '@angular/core';
import {HttpClient, HttpErrorResponse, HttpHeaders} from '@angular/common/http';
import {Observable, throwError} from 'rxjs';
import {catchError} from 'rxjs/operators';
import {Card} from '../data/Card';
import {NewGame} from '../data/NewGame';
import {NewGameRequest} from '../data/NewGameRequest';
import {Game} from '../data/Game';
import {TurnType} from "../data/TurnType";
import {ActionDto} from "../data/ActionDto";
import {BuildProjectRequest} from "../data/BuildProjectRequest";

export const REST_URL = new InjectionToken('rest_url');

@Injectable()
export class RestDataSource {
  constructor(private http: HttpClient,
              @Inject(REST_URL) private url: string) {
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

  pickCorporation(playerUuid: string, corporationId: number): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/game/player/corporation',
      {'playerUuid': playerUuid, 'corporationId': corporationId}
    );
  }

  pickPhase(playerUuid: string, phaseId: number): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/game/player/phase',
      {'playerUuid': playerUuid, 'phaseId': phaseId}
    );
  }

  skipTurn(playerUuid: string): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/game/player/skip',
      {'playerUuid': playerUuid}
    );
  }

  sellCards(playerUuid: string, cards: number[]): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/game/player/sell',
      {'playerUuid': playerUuid, 'cards': cards}
    );
  }

  nextAction(playerUuid: string): Observable<ActionDto> {
    return this.sendRequest<ActionDto>('GET', this.url + '/action/next/' + playerUuid);
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

  discardCards(playerUuid: string, cards: number[]): Observable<any> {
    return this.sendRequest<any>('POST', this.url + '/turn/cards/discard',
      {'playerUuid': playerUuid, 'cards': cards}
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
