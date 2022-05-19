import {Component} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {GameRepository} from '../model/gameRepository.model';
import {Game} from '../data/Game';
import {Card} from '../data/Card';
import {TurnType} from '../data/TurnType';
import {Subscription, timer} from 'rxjs';


@Component({
  selector: 'app-game',
  templateUrl: './game.component.html',
  styleUrls: ['./game.component.css']
})
export class GameComponent {
  public errorMessage: string;
  private playerUuid: string;
  private nextAction: string;
  public game: Game;
  public nextTurns: TurnType[];
  private subscription: Subscription;
  private thirdPhaseSubscription: Subscription;

  constructor(private route: ActivatedRoute, private model: GameRepository) {
    this.route.params.subscribe(playerUuid => {
      this.playerUuid = playerUuid.playerUuid;
      model.getGame(this.playerUuid).subscribe(data => {
        this.game = data;
        this.identifyNextAction();
      });
    });
  }

  getCorporationCards(): Card[] {
    return this.game?.player.corporations;
  }

  getPlayerHand(): Card[] {
    return this.game?.player.hand;
  }

  getPlayerPlayedCards(): Card[] {
    return this.game?.player.played;
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
          this.nextTurns = turns;
          this.model.getGame(this.playerUuid).subscribe(game => {
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
