import {Component, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {Game} from '../../data/Game';
import {GameRepository} from '../../model/gameRepository.model';
import {TurnType} from '../../data/TurnType';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {Card} from '../../data/Card';
import {CardColor} from '../../data/CardColor';
import {DiscountComponent} from '../../discount/discount.component';
import {BuildProjectRequest} from '../../data/BuildProjectRequest';
import {Payment} from '../../data/Payment';
import {PaymentType} from '../../data/PaymentType';
import {SellCardsComponent} from '../sellCards/sellCards.component';
import {CardAction} from '../../data/CardAction';
import {Tag} from '../../data/Tag';
import {InputFlag} from '../../data/InputFlag';
import {CardResource} from '../../data/CardResource';
import {DiscardCardsTurn} from "../../data/DiscardCardsTurn";

@Component({
  selector: 'app-second-phase',
  templateUrl: './secondPhase.component.html',
  styleUrls: ['../turns.component.css']
})
export class SecondPhaseComponent implements OnInit {
  public errorMessage: string;
  isSubmitted = false;
  selectedProject: Card;
  projectsToDiscard: number[];
  viralEnhancersTargetCards: number[];
  onBuildResourceChoice = null;
  importedHydrogenMicrobeAnimal = null;
  importedNitrogenMicrobeCard = null;
  importedNitrogenAnimalCard = null;
  largeConvoyAnimalCard = null;
  localHeatTrappingCard = null;
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
      mcPrice: [''],
      heatPrice: 0,
      anaerobicMicroorganisms: [false],
      marsUniversityDiscardLess: [false],
      takeMicrobes: 0,
      takeCards: 0,
      restructuredResources: [false],
      viralEnhancersPlantInput: 0,
      importedHydrogenForm: 'plants',
      largeConvoyForm: 'plants'
    });
  }

  sendToParent(data: any) {
    this.outputToParent.emit(data);
  }

  buildBlueRedProjectTurn(): boolean {
    return this.nextTurns
      && this.nextTurns.find(turn => turn === TurnType[TurnType.BUILD_BLUE_RED_PROJECT])?.length > 0
      && this.game.player.hand.some(card => card.cardColor === CardColor.BLUE || card.cardColor === CardColor.RED);
  }

  discardCardsTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.DISCARD_CARDS])?.length > 0;
  }

  sellCardsTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.SELL_CARDS])?.length > 0;
  }

  pickExtraCardTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.PICK_EXTRA_CARD])?.length > 0;
  }

  skipTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.SKIP_TURN])?.length > 0;
  }

  getPlayerHand(): Card[] {
    return this.game?.player.hand;
  }

  getPlayerHandWithoutSelectedCard(): Card[] {
    return this.game.player.hand.filter(
      card => card.id !== this.selectedProject.id
    );
  }

  getMicrobeAnimalPlayedCardsWithSelected(): Card[] {
    const playedCopy = [this.selectedProject];
    this.game?.player.played.forEach(e => playedCopy.push(e));
    return playedCopy.filter(card => card.cardResource === CardResource[CardResource.ANIMAL]
      || card.cardResource === CardResource[CardResource.MICROBE]);
  }

  getMicrobeAnimalPlayedCards(): Card[] {
    return this.game?.player.played.filter(card => card.cardResource === CardResource[CardResource.ANIMAL]
      || card.cardResource === CardResource[CardResource.MICROBE]);
  }

  getAnimalPlayedCards(): Card[] {
    return this.game?.player.played.filter(card => card.cardResource === CardResource[CardResource.ANIMAL]);
  }

  getBlueRedPlayerHand(): Card[] {
    return this.game?.player.hand.filter(
      card => card.cardColor === CardColor[CardColor.BLUE] || card.cardColor === CardColor[CardColor.RED]
    );
  }

  getDiscardCards(): Card[] {
    const nextTurn = this.game.player.nextTurn as DiscardCardsTurn;
    if (nextTurn.onlyFromSelectedCards) {
      return this.game.player.nextTurn.cards;
    } else {
      return this.game.player?.hand;
    }
  }

  clickProjectToBuild(card: Card) {
    if (this.selectedProject && this.selectedProject.id === card.id) {
      this.selectedProject = null;
    } else {
      this.selectedProject = card;
      this.parentForm.controls.mcPrice.setValue(
        this.getDiscountedMcPriceWithEffectsApplied(
          this.parentForm.value.anaerobicMicroorganisms,
          this.parentForm.value.restructuredResources)
      );
    }
  }

  importedNitrogenClick(card: Card) {
    if (card.cardResource === CardResource[CardResource.MICROBE]) {
      if (this.importedNitrogenMicrobeCard && this.importedNitrogenMicrobeCard.id === card.id) {
        this.importedNitrogenMicrobeCard = null;
      } else {
        this.importedNitrogenMicrobeCard = card;
      }
    }
    if (card.cardResource === CardResource[CardResource.ANIMAL]) {
      if (this.importedNitrogenAnimalCard && this.importedNitrogenAnimalCard.id === card.id) {
        this.importedNitrogenAnimalCard = null;
      } else {
        this.importedNitrogenAnimalCard = card;
      }
    }
  }

  importedNitrogenCardClass(card: Card) {
    if (this.importedNitrogenMicrobeCard && this.importedNitrogenMicrobeCard.id === card.id
      || this.importedNitrogenAnimalCard && this.importedNitrogenAnimalCard.id === card.id) {
      return 'clicked-card';
    } else {
      return '';
    }
  }

  anaerobicMicroorganismsClicked($event: any) {
    if ($event.target.checked) {
      this.parentForm.controls.mcPrice.setValue(
        this.getDiscountedMcPriceWithEffectsApplied(
          true,
          this.parentForm.value.restructuredResources)
      );
    } else {
      this.parentForm.controls.mcPrice.setValue(
        this.getDiscountedMcPriceWithEffectsApplied(
          false,
          this.parentForm.value.restructuredResources)
      );
    }
  }

  restructuredResourcesClicked($event: any) {
    if ($event.target.checked) {
      this.parentForm.controls.mcPrice.setValue(
        this.getDiscountedMcPriceWithEffectsApplied(
          this.parentForm.value.anaerobicMicroorganisms,
          true)
      );
    } else {
      this.parentForm.controls.mcPrice.setValue(
        this.getDiscountedMcPriceWithEffectsApplied(
          this.parentForm.value.anaerobicMicroorganisms,
          false)
      );
    }
  }

  getDiscountedMcPriceWithEffectsApplied(anaerobicMicroorganisms: boolean, restructuredResources: boolean): number {
    return Math.max(
      0, this.getDiscountedMcPriceOfSelectedProject() -
      (anaerobicMicroorganisms ? 10 : 0) -
      (restructuredResources ? 5 : 0)
    );
  }

  selectedProjectToBuildClass(card: Card): string {
    if (this.selectedProject && this.selectedProject.id === card.id) {
      return 'clicked-card';
    }
    return '';
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

  getDiscountedMcPriceOfSelectedProject(): number {
    if (this.selectedProject) {
      return this.selectedProject.price - this.discountService.getDiscount(this.selectedProject, this.game);
    } else {
      return 0;
    }
  }

  resetAllInputs() {
    this.selectedProject = null;
    this.onBuildResourceChoice = null;
    this.importedHydrogenMicrobeAnimal = null;
    this.importedNitrogenAnimalCard = null;
    this.importedNitrogenMicrobeCard = null;
    this.largeConvoyAnimalCard = null;
    this.localHeatTrappingCard = null;
  }

  anaerobicMicroorganismsCardAction(): boolean {
    if (!this.selectedProject) {
      return false;
    }
    const anaerobicMicroorganismsCard = this.game.player.played.find(
      card => card.cardAction === CardAction[CardAction.ANAEROBIC_MICROORGANISMS]
    );
    if (!anaerobicMicroorganismsCard) {
      return false;
    }
    return this.game.player.cardResources[anaerobicMicroorganismsCard.id] >= 2;
  }

  restructuredResourcesCardAction(): boolean {
    if (!this.selectedProject) {
      return false;
    }
    const restructuredResourcesCard = this.game.player.played.find(
      card => card.cardAction === CardAction[CardAction.RESTRUCTURED_RESOURCES]
    );
    if (!restructuredResourcesCard) {
      return false;
    }
    return this.game.player.plants > 0;
  }

  marsUniversityEffect(): boolean {
    return (this.selectedProject.cardAction === CardAction[CardAction.MARS_UNIVERSITY]
      || this.game.player.played.some(card => card.cardAction === CardAction[CardAction.MARS_UNIVERSITY]))
      && this.selectedProject.tags.some(tag => tag === Tag[Tag.SCIENCE]);
  }

  viralEnhancersEffect(): boolean {
    return (this.selectedProject.cardAction === CardAction[CardAction.VIRAL_ENHANCERS]
      || this.game.player.played.some(card => card.cardAction === CardAction[CardAction.VIRAL_ENHANCERS]))
      && this.selectedProject.tags.some(tag =>
        tag === Tag[Tag.PLANT] || tag === Tag[Tag.MICROBE] || tag === Tag[Tag.ANIMAL]
      );
  }

  importedHydrogenEffect(): boolean {
    return this.selectedProject.cardAction === CardAction[CardAction.IMPORTED_HYDROGEN];
  }

  localHeatTrappingEffect(): boolean {
    return this.selectedProject.cardAction === CardAction[CardAction.LOCAL_HEAT_TRAPPING];
  }

  importedNitrogenEffect(): boolean {
    return this.selectedProject.cardAction === CardAction[CardAction.IMPORTED_NITROGEN];
  }

  largeConvoyEffect(): boolean {
    return this.selectedProject.cardAction === CardAction[CardAction.LARGE_CONVOY];
  }

  expectsDecomposersInput(): boolean {
    return this.selectedProject.cardAction === CardAction[CardAction.DECOMPOSERS]
      ||
      this.game.player.played.some(card => card.cardAction === CardAction[CardAction.DECOMPOSERS])
      && (this.selectedProject.tags.some(tag =>
        tag === Tag[Tag.ANIMAL] || tag === Tag[Tag.MICROBE] || tag === Tag[Tag.PLANT]));
  }

  decomposersCanTakeCard(): boolean {
    const microbeCount = this.game.player.cardResources[this.game.player.played.find(
      card => card.cardAction === CardAction[CardAction.DECOMPOSERS]
    )?.id];
    return this.selectedProject.tags.filter(tag =>
      tag === Tag[Tag.ANIMAL] || tag === Tag[Tag.MICROBE] || tag === Tag[Tag.PLANT])?.length > 1
      || (microbeCount && microbeCount >= 1);
  }

  canPayWithHeat(): boolean {
    return this.game.player.played.some(card => card.cardAction === CardAction.HELION_CORPORATION)
      && this.game.player.heat > 0;
  }

  expectsResourceInputOnBuild(): boolean {
    return this.selectedProject && this.selectedProject?.resourcesOnBuild.some(resource =>
      resource.type === CardResource[CardResource.ANY]
    );
  }

  getResourcePlayedCards(): Card[] {
    return this.game?.player.played.filter(card =>
      card.cardResource
      && (card.cardResource !== CardResource[CardResource.NONE])
    );
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
      } else if (formGroup.value.turn === 'pickExtraCard') {
        this.gameRepository.pickCard(this.game.player.playerUuid).subscribe(
          data => this.sendToParent(data)
        );
      } else if (formGroup.value.turn === 'sellCards') {
        this.sellCardsService.sellCards(this.game, data => this.sendToParent(data));
      } else if (formGroup.value.turn === 'blueRedProject' && formGroup.value.mcPrice !== null) {
        const inputParams = new Map<number, number[]>();
        if (this.marsUniversityEffect()) {
          const scienceTagsCount = this.selectedProject.tags.filter(tag => tag === Tag[Tag.SCIENCE]).length;
          if (this.projectsToDiscard && this.projectsToDiscard.length > scienceTagsCount) {
            this.errorMessage = 'Mars University may only discard ' + scienceTagsCount + ' cards';
            return;
          }
          if ((!this.projectsToDiscard || this.projectsToDiscard.length < scienceTagsCount)
            && !this.parentForm.value.marsUniversityDiscardLess) {
            this.errorMessage = 'You should either select ' + scienceTagsCount + ' cards to discard or mark the Discard Less checkbox';
            return;
          }
          if (!this.projectsToDiscard || this.projectsToDiscard.length < scienceTagsCount) {
            inputParams[InputFlag.MARS_UNIVERSITY_CARD.valueOf()] = [InputFlag.SKIP_ACTION.valueOf()];
          } else {
            inputParams[InputFlag.MARS_UNIVERSITY_CARD.valueOf()] = this.projectsToDiscard;
          }
        }

        if (this.expectsDecomposersInput()) {
          const expectedInputSum = this.selectedProject.tags.filter(
            tag => tag === Tag[Tag.ANIMAL] || tag === Tag[Tag.MICROBE] || tag === Tag[Tag.PLANT]
          ).length;
          const takeMicrobes = this.parentForm.value.takeMicrobes ? this.parentForm.value.takeMicrobes : 0;
          const takeCards = this.parentForm.value.takeCards ? this.parentForm.value.takeCards : 0;

          if (expectedInputSum !== (takeMicrobes + takeCards)) {
            this.errorMessage = 'Sum of taken microbes and cards doesn\'t correspond to tag sum';
            return;
          }
          inputParams[InputFlag.DECOMPOSERS_TAKE_MICROBE.valueOf()] = [takeMicrobes];
          inputParams[InputFlag.DECOMPOSERS_TAKE_CARD.valueOf()] = [takeCards];
        }

        if (this.expectsResourceInputOnBuild()) {
          if (!this.onBuildResourceChoice) {
            this.errorMessage = 'You need to choose a resource card';
            return;
          }

          const paramId = this.selectedProject?.resourcesOnBuild.find(resource =>
            resource.type === CardResource[CardResource.ANY]
          ).paramId;

          inputParams[paramId] = [this.onBuildResourceChoice.id];
        }

        if (this.viralEnhancersEffect()) {
          const expectedInputSum = this.selectedProject.tags.filter(
            tag => tag === Tag[Tag.ANIMAL] || tag === Tag[Tag.MICROBE] || tag === Tag[Tag.PLANT]
          ).length;
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

        if (this.importedHydrogenEffect()) {
          if (this.parentForm.value.importedHydrogenForm === 'microbeAnimal' && !this.importedHydrogenMicrobeAnimal) {
            this.errorMessage = 'You need to choose an Animal/Microbe card';
            return;
          }

          if (this.parentForm.value.importedHydrogenForm === 'plants') {
            inputParams[InputFlag.IMPORTED_HYDROGEN_PICK_PLANT.valueOf()] = [];
          } else {
            inputParams[InputFlag.IMPORTED_HYDROGEN_PUT_RESOURCE.valueOf()] = [this.importedHydrogenMicrobeAnimal.id];
          }
        }

        if (this.importedNitrogenEffect()) {

          inputParams[InputFlag.IMPORTED_NITROGEN_ADD_MICROBES.valueOf()] = [
            this.importedNitrogenMicrobeCard ? this.importedNitrogenMicrobeCard.id : InputFlag.SKIP_ACTION.valueOf()
          ];

          inputParams[InputFlag.IMPORTED_NITROGEN_ADD_ANIMALS.valueOf()] = [
            this.importedNitrogenAnimalCard ? this.importedNitrogenAnimalCard.id : InputFlag.SKIP_ACTION.valueOf()
          ];
        }

        if (this.largeConvoyEffect()) {

          if (this.parentForm.value.largeConvoyForm === 'animal' && !this.largeConvoyAnimalCard) {
            this.errorMessage = 'You need to choose an Animal card';
            return;
          }

          if (this.parentForm.value.largeConvoyForm === 'plants') {
            inputParams[InputFlag.LARGE_CONVOY_PICK_PLANT.valueOf()] = [];
          } else {
            inputParams[InputFlag.LARGE_CONVOY_ADD_ANIMAL.valueOf()] = [this.largeConvoyAnimalCard.id];
          }
        }

        if (this.localHeatTrappingEffect()) {
          inputParams[InputFlag.LOCAL_HEAT_TRAPPING_PUT_RESOURCE.valueOf()] = [
            this.localHeatTrappingCard ? this.localHeatTrappingCard.id : InputFlag.SKIP_ACTION.valueOf()
          ];
        }

        const payments = [new Payment(formGroup.value.mcPrice, PaymentType.MEGACREDITS)];
        if (this.canPayWithHeat() && formGroup.value.heatPrice > 0) {
          payments.push(new Payment(formGroup.value.heatPrice, PaymentType.HEAT));
        }

        if (this.parentForm.value.anaerobicMicroorganisms) {
          payments.push(new Payment(2, PaymentType.ANAEROBIC_MICROORGANISMS));
        }

        if (this.parentForm.value.restructuredResources) {
          payments.push(new Payment(1, PaymentType.RESTRUCTURED_RESOURCES));
        }

        const request = new BuildProjectRequest(
          this.game.player.playerUuid,
          this.selectedProject.id,
          payments,
          inputParams
        );

        this.gameRepository.buildBlueRedProject(request).subscribe(data => {
          this.sendToParent(data);
          this.selectedProject = null;
        }, error => {
          this.errorMessage = error;
        });
      } else if (formGroup.value.turn === 'discardCards') {
        if (!this.projectsToDiscard || this.projectsToDiscard.length !== this.game.player.nextTurn.size) {
          this.errorMessage = 'Invalid number of cards to discard';
        } else {
          this.gameRepository.discardCards(this.game.player.playerUuid, this.projectsToDiscard).subscribe(
            data => {
              this.sendToParent(data);
              this.selectedProject = null;
              this.errorMessage = null;
            },
            error => {
              this.errorMessage = error;
            }
          );
        }
      }
    }
  }

}
