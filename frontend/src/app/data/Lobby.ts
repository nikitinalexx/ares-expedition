import {LobbyGame} from './LobbyGame';

export class Lobby {
  constructor(players: string[],
              games: LobbyGame[],
              playerGame?: LobbyGame) {
  }

  players: string[];
  games: LobbyGame[];
  playerGame: LobbyGame;
}

