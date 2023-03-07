import {Component, Inject, InjectionToken, OnInit} from '@angular/core';
import {NewGameRepository} from '../model/newGameRepository.model';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {NewGameRequest} from '../data/NewGameRequest';
import {PlayerReference} from '../data/PlayerReference';
import {Expansion} from '../data/Expansion';


export const BASE_URL = new InjectionToken('rest_url');

@Component({
  selector: 'app-new-crysis-game',
  templateUrl: './newGameCrysis.component.html',
  styleUrls: ['./newGameCrysis.component.css']
})
export class NewGameCrysisComponent implements OnInit {
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
      playerCount: ['1', Validators.required],
      playerName1: '',
      playerName2: '',
      playerName3: '',
      playerName4: '',
      beginner: false
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
        computers.push(false);
        names.push(name);
      }
      const expansions = [Expansion.BASE, Expansion.DISCOVERY, Expansion.CRYSIS];
      this.loading = true;
      this.players = null;
      this.model.createNewGame(
        new NewGameRequest(
          names,
          computers,
          true,
          false,
          this.parentForm.value.beginner,
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
