export class LobbyGame {
  constructor(id: number,
              name: string,
              mulligan: boolean,
              playerToStartConfirm: Map<string, boolean>) {
  }

  id: number;
  name: string;
  mulligan: boolean;
  playerToStartConfirm: Map<string, boolean>;
}
