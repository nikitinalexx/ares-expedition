import {Component, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {Game} from '../../data/Game';
import {GameRepository} from '../../model/gameRepository.model';
import {FormBuilder, FormGroup} from '@angular/forms';
import {TurnType} from '../../data/TurnType';
import {InputFlag} from '../../data/InputFlag';
import {CrysisCard} from '../../data/CrysisCard';
import {SellCardsComponent} from '../sellCards/sellCards.component';
import {CrysisChoiceRequest} from '../../data/CrysisChoiceRequest';
import {Card} from '../../data/Card';
import {CrysisCardAction} from '../../data/CrysisCardAction';
import {ScrollComponent} from '../../scroll/scroll.component';
import {CardColor} from '../../data/CardColor';
import {DiscardCardsTurn} from '../../data/DiscardCardsTurn';
import {CrisisDummyCard} from '../../data/CrisisDummyCard';
import {DetrimentToken} from '../../data/DetrimentToken';

@Component({
  selector: 'app-crysis-turn',
  templateUrl: './crysisTurn.component.html'
})
export class CrysisTurnComponent implements OnInit {
  @Input()
  game: Game;
  turns: TurnType[];
  @Output() outputToParent = new EventEmitter<any>();
  @ViewChild(SellCardsComponent) sellCardsService;

  @Input() set nextTurns(value: TurnType[]) {
    this.turns = value;
    if (this.resolveImmediateWithChoiceAction()) {
      this.parentForm?.patchValue({turn: 'immediate-choice'}, {onlySelf: true, emitEvent: false});
    }
    if (this.resolvePersistentWithChoiceAction()) {
      this.parentForm?.patchValue({turn: 'persistent-choice'}, {onlySelf: true, emitEvent: false});
    }
  }

  selectedProject: number;
  projectsToDiscard: number[];
  public errorMessage: string;
  parentForm: FormGroup;
  crisisDummySoloChoice: string[];

  constructor(private gameRepository: GameRepository,
              private formBuilder: FormBuilder,
              private scrollService: ScrollComponent) {

  }

  ngOnInit() {
    this.parentForm = this.formBuilder.group({
      choice: '',
      turn: this.resolvePersistentWithChoiceAction() ? 'persistent-choice' : 'immediate-choice'
    });
  }

  discardCardsTurn(): boolean {
    return this.turns && this.turns.find(turn => turn === TurnType[TurnType.DISCARD_CARDS])?.length > 0;
  }

  crisisVpToTokenTurn(): boolean {
    return this.turns && this.turns.find(turn => turn === TurnType[TurnType.CRISIS_VP_TO_TOKEN])?.length > 0;
  }

  skipTurn(): boolean {
    return this.turns && this.turns.find(turn => turn === TurnType[TurnType.SKIP_TURN])?.length > 0;
  }

  resolveOceanDetrimentTurn(): boolean {
    return this.turns && this.turns.find(turn => turn === TurnType[TurnType.RESOLVE_OCEAN_DETRIMENT])?.length > 0;
  }

  resolveImmediateWithChoiceAction(): boolean {
    return this.turns && this.turns.some(turn => turn === TurnType.RESOLVE_IMMEDIATE_WITH_CHOICE);
  }

  crisisChooseDummyHandTurn(): boolean {
    return this.turns && this.turns.some(turn => turn === TurnType.CRISIS_CHOOSE_DUMMY_HAND);
  }

  resolvePersistentWithChoiceAction(): boolean {
    return this.turns && this.turns.some(turn => turn === TurnType.RESOLVE_PERSISTENT_WITH_CHOICE);
  }

  resolvePersistentAllAction(): boolean {
    return this.turns && this.turns.some(turn => turn === TurnType.RESOLVE_PERSISTENT_ALL);
  }

  resolveImmediateAllAction(): boolean {
    return this.turns && this.turns.some(turn => turn === TurnType.RESOLVE_IMMEDIATE_ALL);
  }

  getCrysisCardsWithoutPersistentChoice(): CrysisCard[] {
    return this.game.crysisDto.openedCards.filter(crysisCard => !crysisCard.persistentEffectRequiresChoice
      && crysisCard.cardAction !== CrysisCardAction.PLAY_AND_DISCARD_CRYSIS);
  }

  sellCardsTurn(): boolean {
    return this.turns && this.turns.find(turn => turn === TurnType[TurnType.SELL_CARDS])?.length > 0;
  }

  getCrysisCardWithChoice(): CrysisCard {
    if (!this.game) {
      return null;
    }
    return this.game.crysisDto.openedCards.find(crysisCard => crysisCard.id === this.game.player.nextTurn?.card);
  }

  expectsSeismicAftershockGreenCardInput(): boolean {
    return this.getCrysisCardWithChoice()?.cardAction === CrysisCardAction.SEISMIC_AFTERSHOCKS;
  }

  expectsHandCardInput(): boolean {
    return this.getCrysisCardWithChoice()?.cardAction === CrysisCardAction.DUST_CLOUDS;
  }

  getCardWithImmediateAllEffect(): CrysisCard {
    if (!this.game) {
      return null;
    }
    return this.game.crysisDto.openedCards[this.game.crysisDto.openedCards.length - 1];
  }

  getPlayerHand(): Card[] {
    return this.game?.player.hand;
  }

  getGreenPlayedPlayerHand(): Card[] {
    return this.game?.player.played.filter(card => card.cardColor === CardColor.GREEN);
  }

  selectProject(id: number) {
    if (this.selectedProject && this.selectedProject === id) {
      this.selectedProject = null;
    } else {
      this.selectedProject = id;
    }
  }

  selectedProjectClass(id: number): string {
    if (this.selectedProject && this.selectedProject === id) {
      return 'clicked-card';
    }
    return '';
  }

  getDiscardCards(): Card[] {
    const nextTurn = this.game.player.nextTurn as DiscardCardsTurn;
    if (nextTurn.onlyFromSelectedCards) {
      return this.game.player.nextTurn.cards;
    } else {
      return this.game.player?.hand;
    }
  }

  getCrisisCardsWithTokens(): CrysisCard[] {
    const crysisDto = this.game.crysisDto;
    const cardToTokensCountEntries = Object.entries(crysisDto.cardToTokensCount);
    return crysisDto.openedCards.filter(
      crysisCard => cardToTokensCountEntries.find(entry => entry[0] === crysisCard.id.toString())?.[1] > 0
    );
  }

  clickProjectToDiscard(card: Card) {
    if (!this.projectsToDiscard) {
      this.projectsToDiscard = [];
    }
    const index = this.projectsToDiscard.indexOf(card.id, 0);
    if (index > -1) {
      this.projectsToDiscard.splice(index, 1);
    } else {
      this.projectsToDiscard.push(card.id);
    }
  }

  getAllowedPhases(): string[] {
    if (this.crisisDummySoloChoice && this.crisisDummySoloChoice.length !== 0) {
      return this.crisisDummySoloChoice;
    }
    const result = [];
    if (!this.game.otherPlayers || this.game.otherPlayers.length === 0) {
      for (const currentDummyCard of this.game.crysisDto.currentDummyCards) {
        switch (currentDummyCard) {
          case CrisisDummyCard.SOLO_3_4:
            result.push('3_4');
            break;
          case CrisisDummyCard.SOLO_5_3:
            result.push('5_3');
            break;
          case CrisisDummyCard.SOLO_4_3:
            result.push('4_3');
            break;
          case CrisisDummyCard.SOLO_4_2:
            result.push('4_2');
            break;
          case CrisisDummyCard.SOLO_4_1:
            result.push('4_1');
            break;
        }
      }
    }
    return result;
  }

  clickPhase(index: number) {
    if (!this.crisisDummySoloChoice || this.crisisDummySoloChoice.length === 0) {
      this.crisisDummySoloChoice = this.getAllowedPhases();
    }
    const phaseId = this.crisisDummySoloChoice[index];
    this.crisisDummySoloChoice[index] = phaseId[2] + phaseId[1] + phaseId[0];
  }

  selectedProjectToDiscardClass(id: number): string {
    if (this.projectsToDiscard && this.projectsToDiscard.some(element => element === id)) {
      return 'clicked-card';
    }
    return '';
  }

  firstPhaseForbiddenByDetriment(): boolean {
    return this.game.crysisDto?.detrimentTokens.some(token => token === DetrimentToken.OXYGEN_RED);
  }

  firstPhaseNoBonusByDetriment(): boolean {
    return this.game.crysisDto?.detrimentTokens.some(token => token === DetrimentToken.OXYGEN_YELLOW);
  }

  oceanDetrimentCount(): number {
    if (this.game.crysisDto?.detrimentTokens.some(token => token === DetrimentToken.OCEAN_YELLOW)) {
      return 1;
    } else {
      return 2;
    }
  }

  resetAllInputs() {
    this.selectedProject = null;
    this.parentForm.patchValue({choice: ''}, {onlySelf: true, emitEvent: false});
  }

  submitForm(formGroup: FormGroup) {
    this.errorMessage = null;
    if (!formGroup.valid) {
      console.log('form invalid');
      return false;
    } else {
      if (formGroup.value.turn === 'sellCards') {
        this.sellCardsService.sellCards(this.game, data => this.sendToParent(data));
      } else if (formGroup.value.turn === 'discardCards') {
        if (!this.projectsToDiscard || this.projectsToDiscard.length !== this.game.player.nextTurn.size) {
          this.errorMessage = 'Invalid number of cards to discard';
        } else {
          this.gameRepository.discardCards(this.game.player.playerUuid, this.projectsToDiscard).subscribe(
            data => {
              this.sendToParent(data);
              this.selectedProject = null;
              this.projectsToDiscard = [];
              this.errorMessage = null;
            },
            error => {
              this.errorMessage = error;
            }
          );
        }
        this.parentForm?.patchValue({turn: ''}, {onlySelf: true, emitEvent: false});
      } else if (this.expectsHandCardInput()) {
        if (this.game.player.hand.length > 0 && !this.selectedProject) {
          this.errorMessage = 'Choose a Card to discard';
        } else {
          const inputParams = new Map<number, number[]>();
          inputParams[InputFlag.CRYSIS_INPUT_FLAG.valueOf()] = [this.selectedProject];

          if (this.resolveImmediateWithChoiceAction()) {
            this.gameRepository.crysisImmediateChoice(new CrysisChoiceRequest(
              this.game.player.playerUuid, this.getCrysisCardWithChoice().id, inputParams,
            )).subscribe(data => {
              this.sendToParent(data);
            }, error => {
              this.errorMessage = error;
            });
          }
        }

      } else if (this.resolveImmediateWithChoiceAction() && formGroup.value.turn === 'immediate-choice'
        || this.resolvePersistentWithChoiceAction() && formGroup.value.turn === 'persistent-choice') {
        if (formGroup.value.choice !== 'first' && formGroup.value.choice !== 'second') {
          this.errorMessage = 'Choose one of the crysis options';
        } else {
          const inputParams = new Map<number, number[]>();

          inputParams[InputFlag.CRYSIS_INPUT_FLAG.valueOf()] = [
            formGroup.value.choice === 'first'
              ? InputFlag.CRYSIS_INPUT_OPTION_1.valueOf()
              : InputFlag.CRYSIS_INPUT_OPTION_2.valueOf()
          ];

          if (this.expectsSeismicAftershockGreenCardInput() && this.parentForm.value?.choice === 'first') {
            if (!this.selectedProject) {
              this.errorMessage = 'Choose a Card to discard';
              this.scrollService.scrollToPlayerChoice();
              return;
            }
            inputParams[InputFlag.CRYSIS_INPUT_FLAG.valueOf()].push(this.selectedProject);
          }

          if (this.resolveImmediateWithChoiceAction()) {
            this.gameRepository.crysisImmediateChoice(new CrysisChoiceRequest(
              this.game.player.playerUuid, this.getCrysisCardWithChoice().id, inputParams,
            )).subscribe(data => {
              this.sendToParent(data);
            }, error => {
              this.errorMessage = error;
            });
          }
          if (this.resolvePersistentWithChoiceAction()) {
            this.gameRepository.crysisPersistentChoice(new CrysisChoiceRequest(
              this.game.player.playerUuid, this.getCrysisCardWithChoice().id, inputParams,
            )).subscribe(data => {
              this.sendToParent(data);
            }, error => {
              this.errorMessage = error;
            });
          }
        }
      } else if (this.resolveOceanDetrimentTurn()) {
        if (formGroup.value.choice !== 'first' && formGroup.value.choice !== 'second') {
          this.errorMessage = 'Choose one of the crysis options';
        } else {
          const inputParams = new Map<number, number[]>();
          inputParams[InputFlag.CRYSIS_INPUT_FLAG.valueOf()] = [
            formGroup.value.choice === 'first'
              ? InputFlag.CRYSIS_INPUT_OPTION_1.valueOf()
              : InputFlag.CRYSIS_INPUT_OPTION_2.valueOf()
          ];

          this.gameRepository.resolveOceanDetrimentTurn(
            this.game.player.playerUuid, inputParams
          ).subscribe(data => {
            this.sendToParent(data);
          }, error => {
            this.errorMessage = error;
          });
        }
      } else if (this.resolvePersistentAllAction()) {
        this.gameRepository.crysisPersistentAll(this.game.player.playerUuid)
          .subscribe(data => {
            this.sendToParent(data);
          }, error => {
            this.errorMessage = error;
          });
      } else if (this.crisisChooseDummyHandTurn()) {
        if (!this.crisisDummySoloChoice || this.crisisDummySoloChoice.length === 0) {
          this.crisisDummySoloChoice = this.getAllowedPhases();
        }
        this.gameRepository.crisisDummyChoiceTurn(this.game.player.playerUuid, this.crisisDummySoloChoice)
          .subscribe(data => {
            this.sendToParent(data);
          }, error => {
            this.errorMessage = error;
          });
      } else if (this.resolveImmediateAllAction()) {
        this.gameRepository.crysisImmediateAll(this.game.player.playerUuid)
          .subscribe(data => {
            this.sendToParent(data);
          }, error => {
            this.errorMessage = error;
          });
      } else if (formGroup.value.turn === 'skipTurn') {
        this.gameRepository.skipTurn(this.game.player.playerUuid).subscribe(
          data => this.sendToParent(data)
        );
      } else if (this.parentForm.value?.turn === 'vpToToken') {
        if (!this.selectedProject) {
          this.errorMessage = 'Choose a Card to remove a Crisis token from';
          this.scrollService.scrollToPlayerChoice();
          return;
        }
        this.gameRepository.crisisVpToTokenTurn(this.game.player.playerUuid, [this.selectedProject]).subscribe(
          data => this.sendToParent(data),
          error => {
            this.errorMessage = error;
          }
        );
      }
      this.resetAllInputs();
      this.scrollService.scrollToPlayerChoice();
    }
  }

  sendToParent(data: any) {
    this.outputToParent.emit(data);
  }
}
