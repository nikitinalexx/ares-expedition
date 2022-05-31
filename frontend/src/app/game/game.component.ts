import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {GameRepository} from '../model/gameRepository.model';
import {Game} from '../data/Game';
import {Card} from '../data/Card';
import {TurnType} from '../data/TurnType';
import {Subscription, timer} from 'rxjs';
import {FormBuilder, FormGroup} from '@angular/forms';
import {BasePlayer} from '../data/BasePlayer';
import {CardColor} from '../data/CardColor';
import {Tag} from '../data/Tag';


@Component({
  selector: 'app-game',
  templateUrl: './game.component.html',
  styleUrls: ['./game.component.css']
})
export class GameComponent implements OnInit {
  public errorMessage: string;
  private playerUuid: string;
  private playersToNextActions: Map<string, string>;
  public game: Game;
  public nextTurns: TurnType[];
  private subscription: Subscription;
  private thirdPhaseSubscription: Subscription;
  private audio: HTMLAudioElement;
  parentForm: FormGroup;

  constructor(private route: ActivatedRoute,
              private model: GameRepository,
              private formBuilder: FormBuilder) {
    this.audio = new Audio('../../assets/sound/notification.mp3');
    this.route.params.subscribe(playerUuid => {
      this.playerUuid = playerUuid.playerUuid;
      model.getGame(this.playerUuid).subscribe(data => {
        this.game = data;
        this.identifyNextAction();
      });
    });
  }

  ngOnInit(): void {
    this.parentForm = this.formBuilder.group({
      player: 'You'
    });
  }

  yourTurnInfo(): boolean {
    if (!this.playersToNextActions) {
      return true;
    }
    const playerUuid = this.game.player.playerUuid;
    if (this.playersToNextActions[playerUuid] === 'TURN') {
      return false;
    } else {
      return true;
    }
  }

  otherPlayerTurnInfo(anotherPlayer: BasePlayer): boolean {
    if (!this.playersToNextActions) {
      return true;
    }
    const currentPlayer = this.game.player.playerUuid;
    if (this.playersToNextActions[currentPlayer] === 'TURN') {
      return false;
    }
    if (this.playersToNextActions[anotherPlayer.playerUuid] === 'TURN') {
      return false;
    } else {
      return true;
    }
  }

  getCorporationCards(): Card[] {
    return this.game?.player.corporations;
  }

  getPlayerHand(player: BasePlayer): Card[] {
    return player?.hand;
  }

  getPlayerPlayedCards(player: BasePlayer): Card[] {
    return player?.played;
  }

  getPlayedBlueCardsWithCorporation(player: BasePlayer): Card[] {
    return player?.played.filter(card => card.cardColor === CardColor.BLUE || card.cardColor === CardColor.CORPORATION);
  }

  getPlayedRedCards(player: BasePlayer): Card[] {
    return player?.played.filter(card => card.cardColor === CardColor.RED);
  }

  getPlayedGreenCards(player: BasePlayer): Card[] {
    return player?.played.filter(card => card.cardColor === CardColor.GREEN);
  }

  pickCorporationTurn(): boolean {
    return this.game && this.game.phase === 7;
  }

  pickPhaseTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.PICK_PHASE])?.length > 0;
  }

  firstPhaseTurn(): boolean {
    return this.game && this.game.phase === 1 && this.playersToNextActions[this.game.player.playerUuid] === 'TURN';
  }

  secondPhaseTurn(): boolean {
    return this.game && this.game.phase === 2 && this.playersToNextActions[this.game.player.playerUuid] === 'TURN';
  }

  thirdPhaseTurn(): boolean {
    return this.game && this.game.phase === 3 && this.playersToNextActions[this.game.player.playerUuid] === 'TURN';
  }

  fourthPhaseTurn(): boolean {
    return this.game && this.game.phase === 4 && this.playersToNextActions[this.game.player.playerUuid] === 'TURN';
  }

  fifthPhaseTurn(): boolean {
    return this.game && this.game.phase === 5 && this.playersToNextActions[this.game.player.playerUuid] === 'TURN';
  }

  sixthPhaseTurn(): boolean {
    return this.game && this.game.phase === 6 && this.playersToNextActions[this.game.player.playerUuid] === 'TURN';
  }

  gameEnd(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.GAME_END])?.length > 0;
  }

  GetOutputVal(newX) {
    this.identifyNextAction();
  }

  phaseChosenByAnyone(phase: number): boolean {
    return this.game?.player.phase === phase ||
      this.game?.otherPlayers?.some(p => p.phase === phase);
  }

  chosenPhases(): number[] {
    const phases = [];
    for (let i = 1; i <= 5; i++) {
      if (this.phaseChosenByAnyone(i)) {
        phases.push(i);
      }
    }
    return phases;
  }

  currentPhase(phase: number): boolean {
    return this.game?.phase === phase;
  }

  phaseDisplayStyles(phase: number): string[] {
    const result = [];
    if (!this.currentPhase(phase)) {
      result.push('phase-transparent');
    }

    if (this.game?.player?.phase === phase) {
      result.push('selectedPhase');
    }

    return result;
  }

  updateGameShort() {
    this.model.getShortGame(this.playerUuid).subscribe(shortGame => {
      this.game.temperature = shortGame.temperature;
      this.game.phaseTemperature = shortGame.phaseTemperature;
      this.game.phaseOxygen = shortGame.phaseOxygen;
      this.game.oxygen = shortGame.oxygen;
      this.game.oceans = shortGame.oceans;
      this.game.phaseOceans = shortGame.phaseOceans;
      this.game.otherPlayers = shortGame.otherPlayers;
    });
  }

  getPlayerToShow(): BasePlayer {
    if (this.parentForm.value?.player === 'You') {
      return this.game?.player;
    } else {
      const anotherPlayerId = this.parentForm.value.player as number;
      return this.game?.otherPlayers[anotherPlayerId];
    }
  }

  getUniqueTags(player: BasePlayer) {
    const tags = player?.played.map(card => card.tags).reduce((acc, val) => acc.concat(val), []);
    if (!tags) {
      return [];
    }

    const result = tags?.reduce((a, c) => (a.set(c, (a.get(c) || 0) + 1), a), new Map<Tag, number>());
    return Array.from(result?.entries());
  }

  identifyNextAction() {
    this.model.nextActions(this.playerUuid).subscribe(data => {
      const previousAction = this.playersToNextActions
        ? this.playersToNextActions[this.game.player.playerUuid]
        : null;
      this.playersToNextActions = data.playersToNextActions;
      if (this.playersToNextActions[this.game.player.playerUuid] === 'TURN') {
        if (previousAction && previousAction === 'WAIT' && document.hidden) {
          this.audio.play();
        }
        this.model.nextTurns(this.playerUuid).subscribe(turns => {
          const globalParamReachedMax = !this.nextTurns
            && previousAction === 'WAIT'
            && this.game.phase === 3
            && turns.find(t => t === TurnType.PERFORM_BLUE_ACTION);
          if (globalParamReachedMax) {
            this.errorMessage = 'One of the global parameters reached maximum';
          }
          this.model.getGame(this.playerUuid).subscribe(game => {
            this.nextTurns = turns;
            this.game = game;
            if (!globalParamReachedMax) {
              this.errorMessage = null;
            }
            if (this.game.phase === 3 && (!this.thirdPhaseSubscription || this.thirdPhaseSubscription.closed)) {
              this.thirdPhaseSubscription = timer(2000, 2000).subscribe(
                val => this.updateGameShort()
              );
            } else if (this.game.phase !== 3 && this.thirdPhaseSubscription) {
              this.thirdPhaseSubscription.unsubscribe();
            }
          });
        });
        if (this.subscription) {
          this.subscription.unsubscribe();
        }
      } else if (this.playersToNextActions[this.game.player.playerUuid] === 'WAIT') {
        this.errorMessage = 'Waiting for the other player';
        this.nextTurns = null;
        if (!this.subscription || this.subscription.closed) {
          this.subscription = timer(1000, 2000).subscribe(val => this.identifyNextAction());
        }
      }
    });
  }

}
