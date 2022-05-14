import {Component, EventEmitter, Input, Output} from '@angular/core';
import {Game} from '../../data/Game';
import {GameRepository} from '../../model/gameRepository.model';
import {TurnType} from '../../data/TurnType';
import {NgForm} from '@angular/forms';
import {Card} from '../../data/Card';
import {CardColor} from '../../data/CardColor';

@Component({
  selector: 'app-first-phase',
  templateUrl: './firstPhase.component.html',
  styleUrls: ['./firstPhase.component.css']
})
export class FirstPhaseComponent {
  public errorMessage: string;
  isSubmitted = false;
  cardsToCell: number[];
  selectedProject: number;

  @Input()
  game: Game;
  @Input()
  nextTurns: TurnType[];
  @Output() outputToParent = new EventEmitter<any>();

  constructor(private gameRepository: GameRepository) {

  }

  sendToParent(data: any) {
    this.outputToParent.emit(data);
  }

  buildGreenProjectTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.BUILD_GREEN_PROJECT])?.length > 0;
  }

  sellCardsTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.SELL_CARDS])?.length > 0;
  }

  skipTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.SKIP_TURN])?.length > 0;
  }

  getPlayerHand(): Card[] {
    return this.game?.player.hand;
  }

  getGreenPlayerHand(): Card[] {
    return this.game?.player.hand.filter(card => card.cardColor === CardColor[CardColor.GREEN]);
  }

  clickSellCard(card: Card) {
    if (!this.cardsToCell) {
      this.cardsToCell = [];
    }
    if (this.cardsToCell.find(element => element === card.id)) {
      const index = this.cardsToCell.indexOf(card.id, 0);
      if (index > -1) {
        this.cardsToCell.splice(index, 1);
      }
    } else {
      this.cardsToCell.push(card.id);
    }
  }

  clickProjectToBuild(card: Card) {
    if (this.selectedProject && this.selectedProject === card.id) {
      this.selectedProject = null;
    } else {
      this.selectedProject = card.id;
    }
  }

  selectedProjectToBuildClass(card: Card): string {
    if (this.selectedProject && this.selectedProject === card.id) {
      return 'clicked-card';
    }
    return '';
  }

  selectedSellCardClass(card: Card): string {
    if (this.cardsToCell && this.cardsToCell.find(element => element === card.id)) {
      return 'clicked-card';
    }
    return '';
  }


  onValueClick() {
    this.cardsToCell = [];
    this.selectedProject = null;
  }

  submitForm(form: NgForm) {
    this.isSubmitted = true;
    if (!form.valid) {
      return false;
    } else {
      if (form.value.turn === 'skipTurn') {
        this.gameRepository.skipTurn(this.game.player.playerUuid).subscribe(
          data => this.sendToParent(data)
        );
      } else if (form.value.turn === 'sellCards' && this.cardsToCell && this.cardsToCell.length > 0) {
        this.gameRepository.sellCards(this.game.player.playerUuid, this.cardsToCell).subscribe(
          data => {
            this.sendToParent(data);
            this.cardsToCell = [];
          }
        );
      }
    }
  }

}
