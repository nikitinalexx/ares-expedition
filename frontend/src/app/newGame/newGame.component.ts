import {Component, Inject, InjectionToken} from '@angular/core';
import {NewGameRepository} from '../model/newGameRepository.model';
import {NgForm} from '@angular/forms';


export const BASE_URL = new InjectionToken('rest_url');

@Component({
  selector: 'app-new-game',
  templateUrl: './newGame.component.html'
})
export class NewGameComponent {
  public playerCount: number;
  public errorMessage: string;
  public players: string[];

  constructor(private model: NewGameRepository, @Inject(BASE_URL) private url: string) {
  }

  createNewGame(form: NgForm) {
    if (form.valid) {
      this.model.createNewGame(this.playerCount)
        .subscribe(response => {
          if (response) {
            this.errorMessage = null;
            this.players = response.players;
          } else {
            this.errorMessage = 'New game creation failed';
          }
        }, error => {
          this.errorMessage = error;
        });
    } else {
      this.errorMessage = 'Form Data Invalid';
    }
  }

  getPlayerLink(player: string): string {
    return this.url + '/game/' + player;
  }

  playersPresent(): boolean {
    return this.players && this.players.length !== 0;
  }

}
