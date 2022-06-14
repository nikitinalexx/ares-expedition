import {Component, Inject, InjectionToken, OnInit} from '@angular/core';
import {FormBuilder, FormGroup} from '@angular/forms';
import {LobbyRepository} from '../model/lobbyRepository.model';
import {Lobby} from '../data/Lobby';
import {interval, Subscription} from 'rxjs';
import {LobbyGame} from '../data/LobbyGame';


export const BASE_URL = new InjectionToken('rest_url');

@Component({
  selector: 'app-lobby',
  templateUrl: './lobby.component.html',
  styleUrls: ['./lobby.component.css']
})
export class LobbyComponent implements OnInit {
  public errorMessage: string;
  private lobbySubscription: Subscription;
  private audio: HTMLAudioElement;
  nickname: string;
  gameUuid: string;
  lobby: Lobby;

  nicknameForm: FormGroup;
  createLobbyGameForm: FormGroup;

  constructor(private model: LobbyRepository,
              private formBuilder: FormBuilder,
              @Inject(BASE_URL) private url: string) {
    this.audio = new Audio('../../assets/sound/notification.mp3');
  }

  ngOnInit(): void {
    this.nicknameForm = this.formBuilder.group({
      nickname: ''
    });
    this.createLobbyGameForm = this.formBuilder.group({
      mulligan: false
    });
  }

  updateLobby() {
    this.model.getLobby(this.nickname).subscribe(response => {
      const previousLength = (this.lobby?.players ? this.lobby.players.length : 0);
      const currentLength = (response?.players ? response.players.length : 0);
      if (currentLength > previousLength) {
        this.audio.play();
      }
      this.lobby = response;
      this.errorMessage = null;

      this.loadGameLinkIfAvailable();
    }, error => {
      this.lobbySubscription.unsubscribe();
      this.nickname = null;
      this.lobby = null;
    });
  }

  loadGameLinkIfAvailable() {
    if (this.lobby?.playerGame && !this.gameUuid) {
      let allReady = true;
      for (const [p, ready] of Object.entries(this.lobby.playerGame.playerToStartConfirm)) {
        allReady = allReady && ready;
      }
      if (allReady) {
        this.model.getLobbyGamePlayerUuid(this.nickname, this.lobby.playerGame.id)
          .subscribe(r => this.gameUuid = r.uuid);
      }
    }
  }

  displayJoinButton(game: LobbyGame): boolean {
    return !this.lobby.playerGame;
  }

  displayLeaveButton(game: LobbyGame): boolean {
    return this.lobby.playerGame && this.lobby.playerGame.id === game.id;
  }

  getLength(obj: any): number {
    return Object.keys(obj).length;
  }

  submitLobbyForm(formGroup: FormGroup) {
    if (formGroup.valid) {
      this.model.getLobby(formGroup.value.nickname)
        .subscribe(response => {
          if (response) {
            this.errorMessage = null;
            this.nickname = formGroup.value.nickname;
            this.lobby = response;
            this.loadGameLinkIfAvailable();
            this.lobbySubscription = interval(5000).pipe().subscribe(val => this.updateLobby());
          } else {
            this.errorMessage = 'Couldn\'t join the lobby';
          }
        }, error => {
          this.errorMessage = error;
        });
    } else {
      this.errorMessage = 'Form Data Invalid';
    }
  }

  submitNewLobbyGameForm(formGroup: FormGroup) {
    if (formGroup.valid) {
      this.model.createLobbyGame(this.nickname, formGroup.value.mulligan)
        .subscribe(response => {
          this.updateLobby();
        }, error => {
          this.errorMessage = error;
        });
    } else {
      this.errorMessage = 'Form Data Invalid';
    }
  }


  joinGame(game: LobbyGame) {
    this.model.joinLobbyGame(this.nickname, game.id)
      .subscribe(
        response => this.updateLobby(),
        error => this.errorMessage = error
      );
  }

  leaveGame(game: LobbyGame) {
    this.model.leaveLobbyGame(this.nickname, game.id)
      .subscribe(
        response => {
          this.updateLobby();
          this.gameUuid = null;
        },
        error => this.errorMessage = error
      );
  }

  confirmGameStart(game: LobbyGame) {
    this.model.confirmGameStart(this.nickname, game.id)
      .subscribe(
        response => this.updateLobby(),
        error => this.errorMessage = error
      );
  }

  playerLinkAvailable(): boolean {
    return this.gameUuid !== undefined && this.gameUuid !== null;
  }

  getPlayerLink(): string {
    return this.url + '/game/' + this.gameUuid;
  }

}
