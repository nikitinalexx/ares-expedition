import {Component, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {Game} from '../../data/Game';
import {GameRepository} from '../../model/gameRepository.model';
import {TurnType} from '../../data/TurnType';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {Card} from '../../data/Card';
import {DiscountComponent} from '../../discount/discount.component';
import {SellCardsComponent} from '../sellCards/sellCards.component';
import {BlueActionRequest} from '../../data/BlueActionRequest';
import {ActionInputDataType} from '../../data/ActionInputDataType';
import {CardResource} from '../../data/CardResource';
import {CardAction} from '../../data/CardAction';
import {InputFlag} from '../../data/InputFlag';
import {StandardProjectType} from '../../data/StandardProjectType';

@Component({
  selector: 'app-third-phase',
  templateUrl: './thirdPhase.component.html',
  styleUrls: ['../turns.component.css']
})
export class ThirdPhaseComponent implements OnInit {
  public errorMessage: string;
  isSubmitted = false;
  selectedProject: Card;
  actionTargetCards = [];
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
      turn: ['', Validators.required],
      heatInput: [''],
      addOrUseMicrobe: ['addMicrobe'],
      gainPlantOrMicrobe: ['gainPlant'],
      standardProject: ['ocean'],
      heatExchangeInput: 0
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

  confirmGameEndTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.GAME_END_CONFIRM])?.length > 0;
  }

  blueActionTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.PERFORM_BLUE_ACTION])?.length > 0;
  }

  plantForestTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.PLANT_FOREST])?.length > 0;
  }

  increaseTemperatureTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.INCREASE_TEMPERATURE])?.length > 0;
  }

  standardProjectTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.STANDARD_PROJECT])?.length > 0;
  }

  canPlayExtraBlueAction(): boolean {
    return this.game.player.phase === 3 && !this.game.player.activatedBlueActionTwice && this.game.player.activatedBlueCards?.length > 0;
  }

  getPlayerHandForAction(): Card[] {
    if (this.selectedProject && this.selectedProject?.actionInputData.some(data =>
      data.type === ActionInputDataType[ActionInputDataType.DISCARD_CARD]
    )) {
      return this.game.player.hand;
    }
    if (this.selectedProject && this.selectedProject?.actionInputData.some(data =>
      data.type === ActionInputDataType[ActionInputDataType.MICROBE_ANIMAL_CARD]
    )) {
      return this.game.player.played.filter(card =>
        card.cardResource === CardResource[CardResource.ANIMAL] || card.cardResource === CardResource[CardResource.MICROBE]
      );
    }

    if (this.selectedProject && (
      this.selectedProject.cardAction === CardAction[CardAction.EXTREME_COLD_FUNGUS]
      || this.selectedProject?.actionInputData.some(data =>
      data.type === ActionInputDataType[ActionInputDataType.MICROBE_CARD]
      ))) {
      return this.game.player.played.filter(card =>
        card.cardResource === CardResource[CardResource.MICROBE]
      );
    }
  }

  getActiveCards(): Card[] {
    return this.game?.player.played.filter(
      card => card.active
        && !this.game.player.activatedBlueCards.find(abc => abc === card.id)
    );
  }

  getPlayedActiveCards(): Card[] {
    return this.game?.player.played.filter(card =>
      card.active && this.game.player.activatedBlueCards?.find(playedCardId => playedCardId === card.id)
    );
  }

  expectsCardActionInput(): boolean {
    return this.selectedProject && this.selectedProject?.actionInputData.some(data =>
      data.type === ActionInputDataType[ActionInputDataType.DISCARD_CARD]
      || data.type === ActionInputDataType[ActionInputDataType.MICROBE_CARD]
      || data.type === ActionInputDataType[ActionInputDataType.MICROBE_ANIMAL_CARD]
    );
  }

  expectsHeatActionInput(): boolean {
    return this.selectedProject && this.selectedProject?.actionInputData.some(data =>
      data.type === ActionInputDataType[ActionInputDataType.DISCARD_HEAT]
    );
  }

  expectsAddDiscardMicrobeInput(): boolean {
    return this.selectedProject && this.selectedProject?.actionInputData.some(data =>
      data.type === ActionInputDataType[ActionInputDataType.ADD_DISCARD_MICROBE]
    );
  }

  expectsMicrobeInput(): boolean {
    return this.selectedProject && this.selectedProject?.actionInputData.some(data =>
      data.type === ActionInputDataType[ActionInputDataType.MICROBE_CARD]
    );
  }

  expectsExtremeColdFungusInput(): boolean {
    return this.selectedProject && this.selectedProject.cardAction === CardAction.EXTREME_COLD_FUNGUS;
  }

  getDiscardCards(): Card[] {
    return this.game.player.nextTurn.cards;
  }

  selectProject(card: Card) {
    if (this.selectedProject && this.selectedProject.id === card.id) {
      this.clearInput();
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
    this.actionTargetCards = [];
    this.errorMessage = null;
  }

  addCardToActionTargetCards(card: Card) {
    if (!this.actionTargetCards) {
      this.actionTargetCards = [];
    }
    if (this.actionTargetCards.find(element => element === card.id)) {
      const index = this.actionTargetCards.indexOf(card.id, 0);
      if (index > -1) {
        this.actionTargetCards.splice(index, 1);
      }
    } else {
      this.actionTargetCards.push(card.id);
    }
  }

  getActionTargetCardClass(card: Card): string {
    if (this.actionTargetCards && this.actionTargetCards.find(c => c === card.id)) {
      return 'clicked-card';
    } else {
      return '';
    }
  }

  canExchangeHeat(): boolean {
    return this.game.player.played.some(card => card.cardAction === CardAction.HELION_CORPORATION)
      && this.game.player.heat > 0
      && this.nextTurns
      && this.nextTurns.find(turn => turn === TurnType[TurnType.EXCHANGE_HEAT])?.length > 0;
  }


  submitForm(formGroup: FormGroup) {
    this.errorMessage = null;
    this.isSubmitted = true;
    if (!formGroup.valid) {
      console.log('form invalid');
      return false;
    } else {
      if (formGroup.value.turn === 'confirmGameEnd') {
        this.gameRepository.confirmGameEnd(this.game.player.playerUuid).subscribe(
          data => this.sendToParent(data)
        );
      } else if (formGroup.value.turn === 'exchangeHeat') {
        if (!this.parentForm.value.heatExchangeInput) {
          this.errorMessage = 'Input amount of heat to exchange';
          return;
        }
        if (this.parentForm.value.heatExchangeInput < 0 || this.parentForm.value.heatExchangeInput > this.game.player.heat) {
          this.errorMessage = 'Invalid amount of heat';
          return;
        }
        this.gameRepository.exchangeHeat(
          this.game.player.playerUuid, this.parentForm.value.heatExchangeInput
        ).subscribe(data => this.sendToParent(data), error => {
          this.errorMessage = error;
        });
      } else if (formGroup.value.turn === 'standardProject') {
        if (!this.parentForm.value.standardProject) {
          this.errorMessage = 'Select standard project';
          return;
        }
        this.gameRepository.standardProject(
          this.game.player.playerUuid,
          StandardProjectType[this.parentForm.value.standardProject.toUpperCase()]
        ).subscribe(data => this.sendToParent(data), error => {
          this.errorMessage = error;
        });
      } else if (formGroup.value.turn === 'plantForest') {
        this.gameRepository.plantForest(this.game.player.playerUuid).subscribe(
          data => this.sendToParent(data)
        );
      } else if (formGroup.value.turn === 'increaseTemperature') {
        this.gameRepository.increaseTemperature(this.game.player.playerUuid).subscribe(
          data => this.sendToParent(data)
        );
      } else if (formGroup.value.turn === 'skipTurn') {
        this.gameRepository.skipTurn(this.game.player.playerUuid).subscribe(
          data => this.sendToParent(data), error => {
            this.sendToParent(null);
          }
        );
      } else if (formGroup.value.turn === 'sellCards') {
        this.sellCardsService.sellCards(this.game, data => this.sendToParent(data));
      } else if (formGroup.value.turn === 'blueAction' || formGroup.value.turn === 'extraBlueAction') {
        if (!this.selectedProject) {
          this.errorMessage = 'Select a blue card with action';
          return;
        }
        let inputParams = [];
        if (this.expectsCardActionInput()) {
          const expectedCount = this.selectedProject.actionInputData.find(data =>
            data.type === ActionInputDataType[ActionInputDataType.DISCARD_CARD]
            || data.type === ActionInputDataType[ActionInputDataType.MICROBE_ANIMAL_CARD]
            || data.type === ActionInputDataType[ActionInputDataType.MICROBE_CARD]
          );
          const min = expectedCount.min;
          const max = expectedCount.max;
          if (!this.actionTargetCards || this.actionTargetCards.length < min || this.actionTargetCards.length > max) {
            if (min === max) {
              this.errorMessage = 'Exactly ' + min + ' cards are expected to be selected';
            } else {
              this.errorMessage = min + ' to ' + max + ' cards are expected to be selected';
            }
            return;
          }
          inputParams = inputParams.concat(this.actionTargetCards);
        }

        if (this.expectsHeatActionInput()) {
          const expectedCount = this.selectedProject.actionInputData.find(data =>
            data.type === ActionInputDataType[ActionInputDataType.DISCARD_HEAT]
          );
          const min = expectedCount.min;
          const max = expectedCount.max;
          const inputValue = this.parentForm.get('heatInput').value;
          if (!inputValue || inputValue < min || inputValue > max) {
            this.errorMessage = min + ' to ' + max + ' heat to discard is expected';
            return;
          }
          if (this.game.player.heat < inputValue) {
            this.errorMessage = 'Not enough heat';
            return;
          }
          inputParams.push(inputValue);
        }

        if (this.expectsAddDiscardMicrobeInput()) {
          const inputConfig = this.selectedProject.actionInputData.find(data =>
            data.type === ActionInputDataType[ActionInputDataType.ADD_DISCARD_MICROBE]
          );
          if (this.parentForm.value.addOrUseMicrobe === 'addMicrobe') {
            inputParams.push(inputConfig.min);
          } else {
            inputParams.push(inputConfig.max);
          }
        }

        if (this.expectsExtremeColdFungusInput()) {
          if (this.parentForm.value.gainPlantOrMicrobe === 'gainPlant') {
            inputParams.push(InputFlag.EXTEME_COLD_FUNGUS_PICK_PLANT.valueOf());
          } else {
            if (!this.actionTargetCards || this.actionTargetCards.length !== 1) {
              this.errorMessage = 'One card is expected to be selected';
              return;
            }
            inputParams.push(InputFlag.EXTREME_COLD_FUNGUS_PUT_MICROBE.valueOf());
            inputParams.push(this.actionTargetCards[0]);
          }
        }

        const request = new BlueActionRequest(
          this.game.player.playerUuid,
          this.selectedProject.id,
          inputParams
        );

        this.gameRepository.blueAction(request).subscribe(data => {
          this.sendToParent(data);
          this.clearInput();

          this.parentForm.get('turn').setValue('blueAction');
        }, error => {
          this.errorMessage = error;
        });
      }
    }
  }

}
