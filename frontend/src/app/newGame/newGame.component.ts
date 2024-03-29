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
      advantageFlag1: false,
      advantageFlag2: false,
      advantageFlag3: false,
      advantageFlag4: false,
      extraPoints1: 0,
      extraPoints2: 0,
      extraPoints3: 0,
      extraPoints4: 0,
      mulligan: false,
      improveWeakCorp: false,
      dummyHand: false,
      infrastructure: false,
      experimental: false,
      difficulty1: '',
      difficulty2: '',
      difficulty3: '',
      difficulty4: ''
    });
  }

  anyDifficultyIsAi(): boolean {
    for (let i = 1; i <= this.parentForm.value.playerCount; i++) {
      if (this.parentForm.get('difficulty' + i)?.value == 'ai') {
        console.log(true);
        return true;
      }
    }
    return false;
  }

  submitForm(formGroup: FormGroup) {
    if (formGroup.valid) {
      const names = [];
      const computers = [];
      const extraPoints = [];
      for (let i = 1; i <= this.parentForm.value.playerCount; i++) {
        const name = this.parentForm.get('playerName' + i)?.value;
        if (!name) {
          this.errorMessage = 'Invalid names';
          return;
        }
        names.push(name);

        if (this.parentForm.value.experimental) {
          this.parentForm?.patchValue({[`difficulty${i}`]: null}, {onlySelf: true, emitEvent: false});
          this.parentForm?.patchValue({[`computer${i}`]: null}, {onlySelf: true, emitEvent: false});
        }

        if (this.parentForm.value.infrastructure && this.parentForm.get('computer' + i)?.value && this.parentForm.get('difficulty' + i)?.value == 'ai') {
          this.parentForm?.patchValue({[`difficulty${i}`]: null}, {onlySelf: true, emitEvent: false});
        }

        if (this.parentForm.get('computer' + i)?.value && !this.parentForm.get('difficulty' + i)?.value) {
          this.errorMessage = 'Choose computer difficulty or disable computer option';
          return;
        }

        if (this.parentForm.value.playerCount !== '2' && this.parentForm.get('computer' + i)?.value && this.parentForm.get('difficulty' + i)?.value == 'ai') {
          this.parentForm?.patchValue({[`difficulty${i}`]: null}, {onlySelf: true, emitEvent: false});
        }

        if (this.parentForm.value.playerCount === '2' && this.parentForm.get('computer' + i)?.value && this.parentForm.get('difficulty' + i)?.value == 'ai') {
          this.parentForm?.patchValue({'dummyHand': true}, {onlySelf: true, emitEvent: false});
        }

        if (this.parentForm.get('computer' + i)?.value) {
          switch (this.parentForm.get('difficulty' + i)?.value) {
            case 'random':
              computers.push('RANDOM');
              break;
            case 'smart':
              computers.push('SMART');
              break;
            case 'ai':
              computers.push('NETWORK');
              break;
          }
        } else {
          computers.push('NONE');
        }

        if (this.parentForm.value.playerCount > 1) {
          extraPoints.push(this.parentForm.get('advantageFlag' + i)?.value && this.parentForm.get('extraPoints' + i)?.value ? this.parentForm.get('extraPoints' + i)?.value : 0);
        }
      }
      const expansions = [Expansion.BASE, Expansion.DISCOVERY];
      if (this.parentForm.value.improveWeakCorp) {
        expansions.push(Expansion.BUFFED_CORPORATION);
      }
      if (this.parentForm.value.infrastructure) {
        expansions.push(Expansion.INFRASTRUCTURE);
      }
      if (this.parentForm.value.experimental) {
        expansions.push(Expansion.EXPERIMENTAL);
      }
      this.loading = true;
      this.players = null;
      this.model.createNewGame(
        new NewGameRequest(
          names,
          computers,
          extraPoints,
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

  clickAiComputer() {
    this.parentForm?.patchValue({'dummyHand': true}, {onlySelf: true, emitEvent: false});
  }
}
