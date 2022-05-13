import {Component} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {GameRepository} from '../model/gameRepository.model';
import {Game} from '../data/Game';
import {Card} from '../data/Card';
import {TurnType} from '../data/TurnType';
import {Subscription, timer} from 'rxjs';


@Component({
  selector: 'app-game',
  templateUrl: './game.component.html'
})
export class GameComponent {
  public errorMessage: string;
  private playerUuid: string;
  private nextAction: string;
  public game: Game;
  public nextTurns: TurnType[];
  private subscription: Subscription;

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

  pickCorporationTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.PICK_CORPORATION])?.length > 0;
  }

  pickPhaseTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.PICK_PHASE])?.length > 0;
  }

  firstPhaseTurn(): boolean {
    return this.game && this.game.phase === 1;
  }

  GetOutputVal(newX) {
    this.identifyNextAction();
  }

  identifyNextAction() {
    this.model.nextAction(this.playerUuid).subscribe(data => {
      this.nextAction = data.action;
      if (this.nextAction === 'TURN') {
        this.errorMessage = null;
        this.model.nextTurns(this.playerUuid).subscribe(turns => {
          this.nextTurns = turns;
          this.model.getGame(this.playerUuid).subscribe(game => {
            this.game = game;
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
