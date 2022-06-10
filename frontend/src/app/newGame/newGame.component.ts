import {Component, Inject, InjectionToken, OnInit} from '@angular/core';
import {NewGameRepository} from '../model/newGameRepository.model';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {NewGameRequest} from '../data/NewGameRequest';
import {PlayerReference} from '../data/PlayerReference';
import {Expansion} from '../data/Expansion';


export const BASE_URL = new InjectionToken('rest_url');

@Component({
  selector: 'app-new-game',
  templateUrl: './newGame.component.html',
  styleUrls: ['./newGame.component.css']
})
export class NewGameComponent implements OnInit {
  public playerCount: number;
  public errorMessage: string;
  public players: PlayerReference[];

  parentForm: FormGroup;

  constructor(private model: NewGameRepository,
              private formBuilder: FormBuilder,
              @Inject(BASE_URL) private url: string) {
  }

  ngOnInit(): void {
    this.parentForm = this.formBuilder.group({
      playerCount: ['2', Validators.required],
      playerName1: '',
      playerName2: '',
      playerName3: '',
      playerName4: '',
      mulligan: false,
      improveWeakCorp: false
    });
  }

  submitForm(formGroup: FormGroup) {
    if (formGroup.valid) {
      const names = [];
      for (let i = 1; i <= this.parentForm.value.playerCount; i++) {
        const name = this.parentForm.get('playerName' + i)?.value;
        if (!name) {
          this.errorMessage = 'Invalid names';
          return;
        }
        names.push(name);
      }
      const expansions = [Expansion.BASE];
      if (this.parentForm.value.improveWeakCorp) {
        expansions.push(Expansion.BUFFED_CORPORATION);
      }
      this.model.createNewGame(new NewGameRequest(names, this.parentForm.value.mulligan, expansions))
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

  getPlayerCountArray(): number[] {
    const arr = [];
    for (let i = 1; i <= this.parentForm.value.playerCount; i++) {
      arr.push(i);
    }
    return arr;
  }

}
