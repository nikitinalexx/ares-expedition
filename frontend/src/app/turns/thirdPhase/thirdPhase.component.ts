import {Component, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {Game} from '../../data/Game';
import {GameRepository} from '../../model/gameRepository.model';
import {TurnType} from '../../data/TurnType';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {Card} from '../../data/Card';
import {CardColor} from '../../data/CardColor';
import {DiscountComponent} from '../../discount/discount.component';
import {SellCardsComponent} from '../sellCards/sellCards.component';
import {BlueActionRequest} from '../../data/BlueActionRequest';
import {ActionInputDataType} from '../../data/ActionInputDataType';

@Component({
  selector: 'app-third-phase',
  templateUrl: './thirdPhase.component.html',
  styleUrls: ['../turns.component.css']
})
export class ThirdPhaseComponent implements OnInit {
  public errorMessage: string;
  isSubmitted = false;
  selectedProject: Card;
  cardsToDiscard = [];
  @ViewChild(SellCardsComponent) sellCardsService;

  parentForm: FormGroup;

  @Input()
  game: Game;
  @Input()
  nextTurns: TurnType[];
  @Output() outputToParent = new EventEmitter<any>();

  constructor(private gameRepository: GameRepository,
              private discountService: DiscountComponent,
              private formBuilder: FormBuilder) {

  }

  ngOnInit() {
    this.parentForm = this.formBuilder.group({
      turn: ['', Validators.required]
    });
  }

  sendToParent(data: any) {
    this.outputToParent.emit(data);
  }

  sellCardsTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.SELL_CARDS])?.length > 0;
  }

  skipTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.SKIP_TURN])?.length > 0;
  }

  blueActionTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.PERFORM_BLUE_ACTION])?.length > 0;
  }

  getPlayerHand(): Card[] {
    return this.game?.player.hand;
  }

  getBlueUnplayedCards(): Card[] {
    return this.game?.player.played.filter(
      card => card.cardColor === CardColor[CardColor.BLUE]
        && card.active
        && !this.game.player.activatedBlueCards.find(abc => abc === card.id)
    );
  }

  expectsCardActionInput(): boolean {
    return this.selectedProject && this.selectedProject?.actionInputData.some(data =>
      data.type === ActionInputDataType[ActionInputDataType.DISCARD_CARD]
    );
  }

  getDiscardCards(): Card[] {
    return this.game.player.nextTurn.cards;
  }

  selectProject(card: Card) {
    if (this.selectedProject && this.selectedProject.id === card.id) {
      this.selectedProject = null;
    } else {
      this.selectedProject = card;
    }
  }

  selectedProjectClass(card: Card): string {
    if (this.selectedProject && this.selectedProject.id === card.id) {
      return 'clicked-card';
    }
    return '';
  }

  clearInput() {
    this.selectedProject = null;
    this.cardsToDiscard = [];
    this.errorMessage = null;
  }

  addCardToDiscard(card: Card) {
    if (!this.cardsToDiscard) {
      this.cardsToDiscard = [];
    }
    if (this.cardsToDiscard.find(element => element === card.id)) {
      const index = this.cardsToDiscard.indexOf(card.id, 0);
      if (index > -1) {
        this.cardsToDiscard.splice(index, 1);
      }
    } else {
      this.cardsToDiscard.push(card.id);
    }
  }

  getCardToDiscardClass(card: Card): string {
    if (this.cardsToDiscard && this.cardsToDiscard.find(c => c === card.id)) {
      return 'clicked-card';
    } else {
      return '';
    }
  }


  submitForm(formGroup: FormGroup) {
    this.errorMessage = null;
    this.isSubmitted = true;
    if (!formGroup.valid) {
      console.log('form invalid');
      return false;
    } else {
      if (formGroup.value.turn === 'skipTurn') {
        this.gameRepository.skipTurn(this.game.player.playerUuid).subscribe(
          data => this.sendToParent(data)
        );
      } else if (formGroup.value.turn === 'sellCards') {
        this.sellCardsService.sellCards(this.game);
        this.sendToParent(null);
      } else if (formGroup.value.turn === 'blueAction') {
        if (!this.selectedProject) {
          this.errorMessage = 'Select a blue card with action';
          return;
        }
        let inputParams = [];
        if (this.expectsCardActionInput()) {
          const expectedCount = this.selectedProject.actionInputData.find(data =>
            data.type === ActionInputDataType[ActionInputDataType.DISCARD_CARD]
          );
          const min = expectedCount.min;
          const max = expectedCount.max;
          if (!this.cardsToDiscard || this.cardsToDiscard.length < min || this.cardsToDiscard.length > max) {
            if (min === max) {
              this.errorMessage = 'Exactly ' + expectedCount + ' cards to discard are expected';
            } else {
              this.errorMessage = min + ' to ' + max + ' cards to discard are expected';
            }
            return;
          }
          inputParams = inputParams.concat(this.cardsToDiscard);
        }

        const request = new BlueActionRequest(
          this.game.player.playerUuid,
          this.selectedProject.id,
          inputParams
        );

        this.gameRepository.blueAction(request).subscribe(data => {
          this.sendToParent(data);
          this.clearInput();
        }, error => {
          this.errorMessage = error;
        });
      }
    }
  }

}
