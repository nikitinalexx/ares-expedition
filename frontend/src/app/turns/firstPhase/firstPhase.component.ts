import {Component, EventEmitter, Input, Output} from '@angular/core';
import {Game} from '../../data/Game';
import {GameRepository} from '../../model/gameRepository.model';
import {TurnType} from '../../data/TurnType';
import {NgForm} from '@angular/forms';
import {Card} from '../../data/Card';
import {CardColor} from '../../data/CardColor';
import {DiscountComponent} from "../../discount/discount.component";
import {BuildProjectRequest} from "../../data/BuildProjectRequest";
import {Payment} from "../../data/Payment";
import {PaymentType} from "../../data/PaymentType";

@Component({
  selector: 'app-first-phase',
  templateUrl: './firstPhase.component.html',
  styleUrls: ['./firstPhase.component.css']
})
export class FirstPhaseComponent {
  public errorMessage: string;
  isSubmitted = false;
  cardsToCell: number[];
  selectedProject: Card;

  @Input()
  game: Game;
  @Input()
  nextTurns: TurnType[];
  @Output() outputToParent = new EventEmitter<any>();

  constructor(private gameRepository: GameRepository, private discountService: DiscountComponent) {

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
    if (this.selectedProject && this.selectedProject.id === card.id) {
      this.selectedProject = null;
    } else {
      this.selectedProject = card;
    }
  }

  selectedProjectToBuildClass(card: Card): string {
    if (this.selectedProject && this.selectedProject.id === card.id) {
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

  getDiscountedMcPriceOfSelectedProject(): number {
    if (this.selectedProject) {
      return this.selectedProject.price - this.discountService.getDiscount(this.selectedProject, this.game);
    } else {
      return 0;
    }
  }


  onValueClick() {
    this.cardsToCell = [];
    this.selectedProject = null;
  }

  submitForm(form: NgForm) {
    this.errorMessage = null;
    this.isSubmitted = true;
    if (!form.valid) {
      console.log('form invalid');
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
      } else if (form.value.turn === 'project' && form.value.mcPrice !== null) {
        const request = new BuildProjectRequest(
          this.game.player.playerUuid,
          this.selectedProject.id,
          [new Payment(form.value.mcPrice, PaymentType.MEGACREDITS)],
          null
        );
        console.log(request);

        this.gameRepository.buildGreenProject(request).subscribe(data => {
          this.sendToParent(data);
          this.selectedProject = null;
        }, error => {
          this.errorMessage = error;
        });
      }
    }
  }

}
