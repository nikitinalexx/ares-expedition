import {Injectable} from '@angular/core';
import {RestDataSource} from './rest.datasource';
import {Observable} from 'rxjs';
import {Lobby} from '../data/Lobby';
import {NewGame} from "../data/NewGame";
import {PlayerReference} from "../data/PlayerReference";

@Injectable()
export class LobbyRepository {

  constructor(private dataSource: RestDataSource) {
  }

  getLobby(playerUuid: string): Observable<Lobby> {
    return this.dataSource.getLobby(playerUuid);
  }

  createLobbyGame(host: string, mulligan: boolean): Observable<any> {
    return this.dataSource.createLobbyGame(host, mulligan);
  }

  joinLobbyGame(player: string, gameId: number): Observable<any> {
    return this.dataSource.joinLobbyGame(player, gameId);
  }

  leaveLobbyGame(player: string, gameId: number): Observable<any> {
    return this.dataSource.leaveLobbyGame(player, gameId);
  }

  getLobbyGamePlayerUuid(player: string, gameId: number): Observable<PlayerReference> {
    return this.dataSource.getLobbyGamePlayerUuid(player, gameId);
  }

  confirmGameStart(player: string, gameId: number): Observable<any> {
    return this.dataSource.confirmGameStart(player, gameId);
  }

}
