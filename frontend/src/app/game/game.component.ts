import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {GameRepository} from '../model/gameRepository.model';
import {Game} from '../data/Game';
import {Card} from '../data/Card';
import {TurnType} from '../data/TurnType';
import {Subscription, timer} from 'rxjs';
import {FormBuilder, FormGroup} from "@angular/forms";
import {BasePlayer} from "../data/BasePlayer";
import {CardColor} from "../data/CardColor";


@Component({
  selector: 'app-game',
  templateUrl: './game.component.html',
  styleUrls: ['./game.component.css']
})
export class GameComponent implements OnInit {
  public errorMessage: string;
  private playerUuid: string;
  private nextAction: string;
  public game: Game;
  public nextTurns: TurnType[];
  private subscription: Subscription;
  private thirdPhaseSubscription: Subscription;
  parentForm: FormGroup;

  constructor(private route: ActivatedRoute,
              private model: GameRepository,
              private formBuilder: FormBuilder) {
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
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.PICK_CORPORATION])?.length > 0;
  }

  pickPhaseTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.PICK_PHASE])?.length > 0;
  }

  firstPhaseTurn(): boolean {
    return this.game && this.game.phase === 1 && this.nextAction === 'TURN';
  }

  secondPhaseTurn(): boolean {
    return this.game && this.game.phase === 2 && this.nextAction === 'TURN';
  }

  thirdPhaseTurn(): boolean {
    return this.game && this.game.phase === 3 && this.nextAction === 'TURN';
  }

  fourthPhaseTurn(): boolean {
    return this.game && this.game.phase === 4 && this.nextAction === 'TURN';
  }

  fifthPhaseTurn(): boolean {
    return this.game && this.game.phase === 5 && this.nextAction === 'TURN';
  }

  sixthPhaseTurn(): boolean {
    return this.game && this.game.phase === 6 && this.nextAction === 'TURN';
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

  phaseDisplayStyles(phase: number): string {
    let result = '';
    if (!this.currentPhase(phase)) {
      result += 'phase-transparent';
    }

    if (this.game?.player?.phase === phase) {
      result += ' selectedPhase';
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
    });
  }

  getPlayerToShow(): BasePlayer {
    if (this.parentForm.value?.player === 'You') {
      return this.game?.player;
    } else {
      const anotherPlayerId = this.parentForm.value.player as number;
      const result = this.game?.otherPlayers[anotherPlayerId];
      console.log(result);
      return result;
    }
  }

  identifyNextAction() {
    this.model.nextAction(this.playerUuid).subscribe(data => {
      const previousAction = this.nextAction;
      this.nextAction = data.action;
      if (this.nextAction === 'TURN') {
        this.errorMessage = null;
        this.model.nextTurns(this.playerUuid).subscribe(turns => {
          if (!this.nextTurns
            && previousAction === 'WAIT'
            && this.game.phase === 3
            && turns.find(t => t === TurnType.PERFORM_BLUE_ACTION)) {
            this.errorMessage = 'One of the global parameters reached maximum';
          }
          this.model.getGame(this.playerUuid).subscribe(game => {
            this.nextTurns = turns;
            this.game = game;
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
      } else if (this.nextAction === 'WAIT') {
        this.errorMessage = 'Waiting for the other player';
        this.nextTurns = null;
        if (!this.subscription || this.subscription.closed) {
          this.subscription = timer(1000, 2000).subscribe(val => this.identifyNextAction());
        }
      }
    });
  }

}
