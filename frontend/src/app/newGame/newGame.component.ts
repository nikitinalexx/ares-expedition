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
  loading: boolean;

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
      computer1: false,
      computer2: false,
      computer3: false,
      computer4: false,
      mulligan: false,
      improveWeakCorp: false,
      dummyHand: false,
      difficulty1: '',
      difficulty2: '',
      difficulty3: '',
      difficulty4: ''
    });
  }

  submitForm(formGroup: FormGroup) {
    if (formGroup.valid) {
      const names = [];
      const computers = [];
      for (let i = 1; i <= this.parentForm.value.playerCount; i++) {
        const name = this.parentForm.get('playerName' + i)?.value;
        if (!name) {
          this.errorMessage = 'Invalid names';
          return;
        }
        names.push(name);

        if (this.parentForm.get('computer' + i)?.value && !this.parentForm.get('difficulty' + i)?.value) {
          this.errorMessage = 'Choose computer difficulty or disable computer option';
          return;
        }

        if (this.parentForm.get('computer' + i)?.value) {
          computers.push(this.parentForm.get('difficulty' + i)?.value === 'random' ? 'RANDOM' : 'SMART');
        } else {
          computers.push('NONE');
        }
      }
      const expansions = [Expansion.BASE, Expansion.DISCOVERY];
      if (this.parentForm.value.improveWeakCorp) {
        expansions.push(Expansion.BUFFED_CORPORATION);
      }
      this.loading = true;
      this.players = null;
      this.model.createNewGame(
        new NewGameRequest(
          names,
          computers,
          this.parentForm.value.mulligan,
          this.parentForm.value.dummyHand,
          false,
          expansions)
      )
        .subscribe(response => {
          if (response) {
            this.errorMessage = null;
            this.players = response.players;
          } else {
            this.errorMessage = 'New game creation failed';
          }
          this.loading = false;
        }, error => {
          this.errorMessage = error;
          this.loading = false;
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
