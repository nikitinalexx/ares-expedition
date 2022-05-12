import {Inject, Injectable, InjectionToken} from '@angular/core';
import {HttpClient, HttpErrorResponse, HttpHeaders} from '@angular/common/http';
import {Observable, throwError} from 'rxjs';
import {catchError} from 'rxjs/operators';
import {Card} from '../data/Card';
import {NewGame} from '../data/NewGame';
import {NewGameRequest} from '../data/NewGameRequest';
import {Game} from "../data/Game";

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

  private sendRequest<T>(verb: string, url: string, body?: any)
    : Observable<T> {

    let myHeaders = new HttpHeaders();
    myHeaders = myHeaders.set('Access-Key', '<secret>');
    myHeaders = myHeaders.set('Application-Names', ['exampleApp', 'proAngular']);

    return this.http.request<T>(verb, url, {
      body,
      headers: myHeaders
    }).pipe(catchError((error: HttpErrorResponse) => {
      return throwError(`Error: ${error.error?.message} (${error.status})`);
    }));
  }

}
