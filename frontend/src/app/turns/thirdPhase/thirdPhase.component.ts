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
import {CardColor} from '../../data/CardColor';
import {BuildGreenComponent} from '../greenProject/buildGreen.component';
import {DiscardCardsTurn} from '../../data/DiscardCardsTurn';
import {ScrollComponent} from '../../scroll/scroll.component';
import {BuildBlueRedComponent} from '../blueRedProject/buildBlueRed.component';
import {Tag} from '../../data/Tag';
import {DiscardCardsRequest} from "../../data/DiscardCardsRequest";

@Component({
  selector: 'app-third-phase',
  templateUrl: './thirdPhase.component.html',
  styleUrls: ['../turns.component.css']
})
export class ThirdPhaseComponent implements OnInit {
  allTags = [Tag.SPACE, Tag.EARTH, Tag.EVENT, Tag.SCIENCE, Tag.PLANT,
    Tag.ENERGY, Tag.BUILDING, Tag.ANIMAL, Tag.JUPITER, Tag.MICROBE];

  public errorMessage: string;
  isSubmitted = false;
  selectedProject: Card;
  actionTargetCards = [];
  projectsToDiscard: number[];
  phaseInput = 0;
  phaseUpgradeType = -1;
  tagInput = -1;
  viralEnhancersTargetCards: number[];

  @ViewChild(SellCardsComponent) sellCardsService;
  @ViewChild(BuildGreenComponent) buildGreenService;
  @ViewChild(BuildBlueRedComponent) buildBlueRedService;

  parentForm: FormGroup;

  @Input()
  game: Game;
  @Input()
  nextTurns: TurnType[];
  @Output() outputToParent = new EventEmitter<any>();

  constructor(private gameRepository: GameRepository,
              private discountService: DiscountComponent,
              private formBuilder: FormBuilder,
              private scrollService: ScrollComponent) {

  }

  ngOnInit() {
    this.parentForm = this.formBuilder.group({
      turn: ['blueAction', Validators.required],
      heatInput: [''],
      addOrUseMicrobe: ['addMicrobe'],
      gainPlantOrMicrobe: ['gainPlant'],
      standardProject: ['ocean'],
      heatExchangeInput: 0,

      // build green-blue-red projects params
      mcPrice: [''],
      heatPrice: 0,
      onBuildMicrobeEffectChoice: ['chooseMicrobe'],
      onBuildAnimalEffectChoice: ['chooseAnimal'],
      anaerobicMicroorganisms: [false],
      marsUniversityDiscardLess: [false],
      takeMicrobes: 0,
      takeCards: 0,
      restructuredResources: [false],
      viralEnhancersPlantInput: 0,
      importedHydrogenForm: 'plants',
      largeConvoyForm: 'plants',
      biomedicalImports: null
    });
  }

  updatePhaseInput(newPhaseInput: number) {
    this.phaseInput = newPhaseInput;
  }

  updatePhaseUpgradeTypeInput(newPhaseUpgradeType: number) {
    this.phaseUpgradeType = newPhaseUpgradeType;
  }

  sendToParent(data: any) {
    this.outputToParent.emit(data);
  }

  discardCardsTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.DISCARD_CARDS])?.length > 0;
  }

  buildBlueRedProjectTurn(): boolean {
    return this.nextTurns
      && this.nextTurns.find(turn => turn === TurnType[TurnType.BUILD_BLUE_RED_PROJECT])?.length > 0
      && this.game.player.hand.some(card => card.cardColor === CardColor.BLUE || card.cardColor === CardColor.RED);
  }

  buildGreenProjectTurn(): boolean {
    return this.nextTurns
      && this.nextTurns.find(turn => turn === TurnType[TurnType.BUILD_GREEN_PROJECT])?.length > 0
      && this.game.player.hand.some(card => card.cardColor === CardColor.GREEN);
  }

  sellCardsTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.SELL_CARDS])?.length > 0;
  }

  unmiRtTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.UNMI_RT])?.length > 0;
  }

  sellVpTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.SELL_VP])?.length > 0
      && this.game?.player.winPoints > 0;
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

  plantsToCrisisTokenTurn(): boolean {
    return this.nextTurns
      && this.nextTurns.find(turn => turn === TurnType[TurnType.PLANTS_TO_CRISIS_TOKEN])?.length > 0
      && this.game?.player.plants >= 4;
  }

  heatToCrisisTokenTurn(): boolean {
    return this.nextTurns
      && this.nextTurns.find(turn => turn === TurnType[TurnType.HEAT_TO_CRISIS_TOKEN])?.length > 0
      && this.game?.player.heat >= 6;
  }

  cardsToCrisisTokenTurn(): boolean {
    return this.nextTurns
      && this.nextTurns.find(turn => turn === TurnType[TurnType.CARDS_TO_CRISIS_TOKEN])?.length > 0
      && this.game?.player.hand.length >= 3;
  }

  standardProjectTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.STANDARD_PROJECT])?.length > 0;
  }

  standardOceanAvailable(): boolean {
    return (this.game.phaseOceans < 9 || this.game.crysisDto) && this.nextTurns.find(turn => turn === TurnType[TurnType.STANDARD_PROJECT])?.length > 0;
  }

  standardInfrastructureAvailable(): boolean {
    return (this.game.phaseInfrastructure < 100 && !this.game.crysisDto) && this.nextTurns.find(turn => turn === TurnType[TurnType.STANDARD_PROJECT])?.length > 0;
  }

  standardTemperatureAvailable(): boolean {
    return (this.game.phaseTemperature < 8 || this.game.crysisDto) && this.nextTurns.find(turn => turn === TurnType[TurnType.STANDARD_PROJECT])?.length > 0;
  }

  canPlayExtraBlueAction(): boolean {
    const exhaustedResearchGrant = this.game?.player.played.some(card =>
      card.cardAction === CardAction.RESEARCH_GRANT && this.game.player.cardToTag[card.id].every(tag => tag !== Tag.DYNAMIC)
    );

    if (exhaustedResearchGrant) {
      const exhaustedResearchCard = this.game?.player.played.find(card => card.cardAction === CardAction.RESEARCH_GRANT);
      if (this.game.player.activatedBlueCards?.length === 1
        && this.game.player.activatedBlueCards.find(cardId => cardId === exhaustedResearchCard.id)) {
        return false;
      }
    }

    return this.game.player.phase === 3
      && this.game.player.blueActionExtraActivationsLeft > 0
      && this.game.player.activatedBlueCards?.length > 0;
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

  expectsTagInput(): boolean {
    return this.selectedProject?.cardAction === CardAction[CardAction.RESEARCH_GRANT];
  }

  getAllTagsArray(): Tag[] {
    return this.allTags;
  }

  getTagClasses(tagNumber: number): string {
    return 'tag-' + this.allTags[tagNumber].toString().toLowerCase();
  }

  getActiveCards(): Card[] {
    return this.game?.player.played.filter(
      card => card.active
        && !this.game.player.activatedBlueCards.find(abc => abc === card.id)
        && !(card.cardAction === CardAction.RESEARCH_GRANT && this.game.player.cardToTag[card.id].every(tag => tag !== Tag.DYNAMIC))
    );
  }

  getPlayedActiveCards(): Card[] {
    return this.game?.player.played.filter(card =>
      card.active && this.game.player.activatedBlueCards?.find(playedCardId => playedCardId === card.id)
      && !(card.cardAction === CardAction.RESEARCH_GRANT && this.game.player.cardToTag[card.id].every(tag => tag !== Tag.DYNAMIC))
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

  expectsAnyPhaseUpgradeActionInput(): boolean {
    return this.selectedProject
      && (this.selectedProject.cardAction === CardAction.EXPERIMENTAL_TECHNOLOGY
        || this.selectedProject.cardAction === CardAction.VIRTUAL_EMPLOYEE_DEVELOPMENT
        || this.selectedProject.cardAction === CardAction.FIBROUS_COMPOSITE_MATERIAL
        && this.parentForm.value?.addOrUseMicrobe === 'useMicrobe');
  }

  getUpgradePhasesArray(): number[] {
    return [1, 2, 3, 4, 5];
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
    const nextTurn = this.game.player.nextTurn as DiscardCardsTurn;
    if (nextTurn.onlyFromSelectedCards) {
      return this.game.player.nextTurn.cards;
    } else {
      return this.game.player?.hand;
    }
  }

  selectProject(card: Card) {
    if (this.selectedProject && this.selectedProject.id === card.id) {
      this.resetAllInputs();
    } else {
      this.selectedProject = card;
      if (this.expectsAnyPhaseUpgradeActionInput()) {
        this.phaseInput = this.getUpgradePhasesArray()[0] - 1;
      }
    }
  }

  selectedProjectClass(card: Card): string {
    if (this.selectedProject && this.selectedProject.id === card.id) {
      return 'clicked-card';
    }
    return '';
  }

  resetAllInputs(scroll: boolean = true) {
    this.selectedProject = null;
    this.actionTargetCards = [];
    this.errorMessage = null;
    this.projectsToDiscard = [];
    if (this.buildGreenService) {
      this.buildGreenService.resetAllInputs();
    }
    if (this.buildBlueRedService) {
      this.buildBlueRedService.resetAllInputs();
    }
    if (scroll) {
      this.scrollService.scrollToPlayerChoice();
    }
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

  selectedProjectToDiscardClass(card: Card): string {
    if (this.projectsToDiscard && this.projectsToDiscard.some(element => element === card.id)) {
      return 'clicked-card';
    }
    return '';
  }

  canExchangeHeat(): boolean {
    return this.game.player.played.some(card => card.cardAction === CardAction.HELION_CORPORATION)
      && this.game.player.heat > 0
      && this.nextTurns
      && this.nextTurns.find(turn => turn === TurnType[TurnType.EXCHANGE_HEAT])?.length > 0;
  }

  hasStandardTechnology(): boolean {
    return this.game.player.played.some(card => card.cardAction === CardAction.STANDARD_TECHNOLOGY);
  }

  expectsDecomposersInput(): boolean {
    return this.expectsTagInput() && this.tagInput >= 0 && this.tagInput < this.allTags.length
      && (this.allTags[this.tagInput] === Tag.ANIMAL
        || this.allTags[this.tagInput] === Tag.MICROBE
        || this.allTags[this.tagInput] === Tag.PLANT)
      && this.game?.player.played.some(card => card.cardAction === CardAction.DECOMPOSERS);
  }

  expectsMarsUniversityInput(): boolean {
    return this.expectsTagInput() && this.tagInput >= 0 && this.tagInput < this.allTags.length
      && (this.allTags[this.tagInput] === Tag.SCIENCE)
      && this.game?.player.played.some(card => card.cardAction === CardAction.MARS_UNIVERSITY);
  }

  expectsViralEnhancersInput(): boolean {
    return this.expectsTagInput() && this.tagInput >= 0 && this.tagInput < this.allTags.length
      && (this.allTags[this.tagInput] === Tag.ANIMAL
        || this.allTags[this.tagInput] === Tag.MICROBE
        || this.allTags[this.tagInput] === Tag.PLANT)
      && this.game?.player.played.some(card => card.cardAction === CardAction.VIRAL_ENHANCERS);
  }

  getMicrobeAnimalPlayedCards(): Card[] {
    return this.game?.player.played.filter(card => card.cardResource === CardResource[CardResource.ANIMAL]
      || card.cardResource === CardResource[CardResource.MICROBE]);
  }

  clickViralEnhancersTargetCard(card: Card) {
    if (!this.viralEnhancersTargetCards) {
      this.viralEnhancersTargetCards = [];
    }
    const index = this.viralEnhancersTargetCards.indexOf(card.id, 0);
    if (index > -1) {
      this.viralEnhancersTargetCards.splice(index, 1);
    } else {
      this.viralEnhancersTargetCards.push(card.id);
    }
  }

  selectedViralEnhancersCardClass(card: Card): string {
    if (this.viralEnhancersTargetCards && this.viralEnhancersTargetCards.some(element => element === card.id)) {
      return 'clicked-card';
    }
    return '';
  }

  getPlayerHand(): Card[] {
    return this.game.player.hand;
  }

  decomposersCanTakeCard(): boolean {
    const microbeCount = this.game.player.cardResources[this.game.player.played.find(
      card => card.cardAction === CardAction[CardAction.DECOMPOSERS]
    )?.id];
    return microbeCount && microbeCount >= 1;
  }

  submitForm(formGroup: FormGroup) {
    this.errorMessage = null;
    this.isSubmitted = true;
    if (!formGroup.valid) {
      console.log('form invalid');
      return false;
    } else {
      if (formGroup.value.turn === 'discardCards') {
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
      } else if (formGroup.value.turn === 'blueRedProject' && formGroup.value.mcPrice !== null) {
        this.buildBlueRedService.buildBlueRedProject(data => this.sendToParent(data));
      } else if (formGroup.value.turn === 'greenProject' && formGroup.value.mcPrice !== null) {
        this.buildGreenService.buildGreenProject(data => this.sendToParent(data));
      } else if (formGroup.value.turn === 'confirmGameEnd') {
        this.gameRepository.confirmGameEnd(this.game.player.playerUuid).subscribe(
          data => this.sendToParent(data),
          error => this.errorMessage = error
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
      } else if (formGroup.value.turn === 'heatToCrisisToken') {
        this.gameRepository.heatToCrisisToken(this.game.player.playerUuid).subscribe(
          data => this.sendToParent(data)
        );
      } else if (formGroup.value.turn === 'cardsToCrisisToken') {
        if (!this.projectsToDiscard || this.projectsToDiscard.length !== 3) {
          this.errorMessage = 'Invalid number of cards to discard, 3 expected';
        } else {
          this.gameRepository.cardsToCrisisToken(new DiscardCardsRequest(this.game.player.playerUuid, this.projectsToDiscard)).subscribe(
            data => this.sendToParent(data)
          );
        }
      } else if (formGroup.value.turn === 'plantsToCrisisToken') {
        this.gameRepository.plantsToCrisisToken(this.game.player.playerUuid).subscribe(
          data => this.sendToParent(data)
        );
      } else if (formGroup.value.turn === 'skipTurn') {
        this.gameRepository.skipTurn(this.game.player.playerUuid).subscribe(
          data => this.sendToParent(data), error => {
            this.sendToParent(null);
          }
        );
      } else if (formGroup.value.turn === 'unmiRaiseRt') {
        this.gameRepository.raiseUnmiRt(this.game.player.playerUuid).subscribe(
          data => this.sendToParent(data)
        );
      } else if (formGroup.value.turn === 'sellVpTurn') {
        this.gameRepository.sellVp(this.game.player.playerUuid).subscribe(
          data => this.sendToParent(data),
          error => {
            this.errorMessage = error;
          }
        );
      } else if (formGroup.value.turn === 'sellCards') {
        this.sellCardsService.sellCards(this.game, data => this.sendToParent(data));
      } else if (formGroup.value.turn === 'blueAction' || formGroup.value.turn === 'extraBlueAction') {
        if (!this.selectedProject) {
          this.errorMessage = 'Select a blue card with action';
          return;
        }
        const inputParams = new Map<number, number[]>();
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
          inputParams[InputFlag.CARD_CHOICE.valueOf()] = this.actionTargetCards;
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
          inputParams[InputFlag.DISCARD_HEAT.valueOf()] = [inputValue];
        }

        if (this.expectsAnyPhaseUpgradeActionInput()) {
          if (this.phaseInput < 0 || this.phaseInput > 4) {
            this.errorMessage = 'Pick the phase you want to upgrade';
            return;
          }
          if (this.phaseUpgradeType !== 0 && this.phaseUpgradeType !== 1) {
            this.errorMessage = 'Choose the type of phase upgrade';
            return;
          }
          inputParams[InputFlag.PHASE_UPGRADE_CARD.valueOf()] = [this.phaseInput * 2 + this.phaseUpgradeType];
        }

        if (this.expectsAddDiscardMicrobeInput()) {
          const inputConfig = this.selectedProject.actionInputData.find(data =>
            data.type === ActionInputDataType[ActionInputDataType.ADD_DISCARD_MICROBE]
          );
          inputParams[InputFlag.ADD_DISCARD_MICROBE.valueOf()] =
            [this.parentForm.value.addOrUseMicrobe === 'addMicrobe' ? inputConfig.min : inputConfig.max];
        }

        if (this.expectsExtremeColdFungusInput()) {
          if (this.parentForm.value.gainPlantOrMicrobe === 'gainPlant') {
            inputParams[InputFlag.EXTEME_COLD_FUNGUS_PICK_PLANT.valueOf()] = [0];
          } else {
            if (!this.actionTargetCards || this.actionTargetCards.length !== 1) {
              this.errorMessage = 'One card is expected to be selected';
              return;
            }
            inputParams[InputFlag.EXTREME_COLD_FUNGUS_PUT_MICROBE.valueOf()] = [this.actionTargetCards[0]];

          }
        }

        if (this.expectsTagInput()) {
          if (this.tagInput < 0) {
            this.errorMessage = 'Choose a tag to put on a Card';
            return;
          }
          inputParams[InputFlag.TAG_INPUT.valueOf()] = [this.tagInput];
        }

        if (this.expectsDecomposersInput()) {
          const takeMicrobes = this.parentForm.value.takeMicrobes ? this.parentForm.value.takeMicrobes : 0;
          const takeCards = this.parentForm.value.takeCards ? this.parentForm.value.takeCards : 0;

          if (1 !== (takeMicrobes + takeCards)) {
            this.errorMessage = 'Sum of taken microbes and cards doesn\'t correspond to tag sum';
            return;
          }

          if (takeMicrobes > 0) {
            inputParams[InputFlag.DECOMPOSERS_TAKE_MICROBE.valueOf()] = [1];
          } else if (takeCards > 0) {
            inputParams[InputFlag.DECOMPOSERS_TAKE_CARD.valueOf()] = [1];
          }
        }

        if (this.expectsMarsUniversityInput()) {
          if (this.projectsToDiscard && this.projectsToDiscard.length > 1) {
            this.errorMessage = 'Mars University may only discard ' + 1 + ' cards';
            return;
          }
          if ((!this.projectsToDiscard || this.projectsToDiscard.length < 1)
            && !this.parentForm.value.marsUniversityDiscardLess) {
            this.errorMessage = 'You should either select ' + 1 + ' cards to discard or mark the Discard Less checkbox';
            return;
          }
          if (!this.projectsToDiscard || this.projectsToDiscard.length < 1) {
            inputParams[InputFlag.MARS_UNIVERSITY_CARD.valueOf()] = [InputFlag.SKIP_ACTION.valueOf()];
          } else {
            inputParams[InputFlag.MARS_UNIVERSITY_CARD.valueOf()] = this.projectsToDiscard;
          }
        }

        if (this.expectsViralEnhancersInput()) {
          const expectedInputSum = 1;
          const takePlants = this.parentForm.value.viralEnhancersPlantInput ? this.parentForm.value.viralEnhancersPlantInput : 0;
          const cardsSelected = this.viralEnhancersTargetCards ? this.viralEnhancersTargetCards.length : 0;
          if (takePlants + cardsSelected > expectedInputSum) {
            this.errorMessage = 'Only ' + expectedInputSum + ' choices available';
            return;
          }
          if (cardsSelected === 0 && takePlants < expectedInputSum) {
            this.errorMessage = 'Too few options taken, number of affected tags ' + expectedInputSum;
            return;
          }

          if (takePlants < 0) {
            this.errorMessage = 'Number of plants can\'t be negative';
            return;
          }
          inputParams[InputFlag.VIRAL_ENHANCERS_TAKE_PLANT.valueOf()] = [takePlants];

          if (this.viralEnhancersTargetCards) {
            const cardsInput = [];
            this.viralEnhancersTargetCards.forEach(val => cardsInput.push(val));
            if (takePlants + cardsSelected < expectedInputSum) {
              const leftOvers = expectedInputSum - (takePlants + cardsSelected);
              for (let i = 0; i < leftOvers; i++) {
                cardsInput.push(this.viralEnhancersTargetCards[0]);
              }
            }
            inputParams[InputFlag.VIRAL_ENHANCERS_PUT_RESOURCE.valueOf()] = cardsInput;
          }
        }

        const request = new BlueActionRequest(
          this.game.player.playerUuid,
          this.selectedProject.id,
          inputParams
        );

        this.gameRepository.blueAction(request).subscribe(data => {
          this.sendToParent(data);
          this.resetAllInputs(false);

          this.parentForm.get('turn').setValue('blueAction');
        }, error => {
          this.errorMessage = error;
        });
      }
      if (formGroup.value.turn !== 'plantForest'
        && formGroup.value.turn !== 'increaseTemperature'
        && formGroup.value.turn !== 'plantsToCrisisToken'
        && formGroup.value.turn !== 'standardProject'
        && formGroup.value.turn !== 'blueAction'
        && formGroup.value.turn !== 'extraBlueAction') {
        this.scrollService.scrollToPlayerChoice();
      }
    }
  }

}
