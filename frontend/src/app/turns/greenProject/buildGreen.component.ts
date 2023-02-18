import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Game} from '../../data/Game';
import {GameRepository} from '../../model/gameRepository.model';
import {FormGroup} from '@angular/forms';
import {Card} from '../../data/Card';
import {CardColor} from '../../data/CardColor';
import {DiscountComponent} from '../../discount/discount.component';
import {BuildProjectRequest} from '../../data/BuildProjectRequest';
import {Payment} from '../../data/Payment';
import {PaymentType} from '../../data/PaymentType';
import {CardResource} from '../../data/CardResource';
import {CardAction} from '../../data/CardAction';
import {Tag} from '../../data/Tag';
import {InputFlag} from '../../data/InputFlag';
import {RequirementsComponent} from '../../requirements/requirements.component';
import {PhaseDescription} from '../../data/PhaseDescription';

@Component({
  selector: 'app-build-green',
  templateUrl: './buildGreen.component.html',
  styleUrls: ['../turns.component.css']
})
export class BuildGreenComponent implements OnInit {
  public errorMessage: string;
  selectedProject: Card;
  onBuildMicrobeChoice = null;
  onBuildAnimalChoice = null;
  projectsToDiscard: number[];
  viralEnhancersTargetCards: number[];
  phaseInput = 0;
  phaseUpgradeType = -1;

  @Input()
  game: Game;
  @Input()
  parentForm: FormGroup;
  @Output() outputToParent = new EventEmitter<any>();

  constructor(private gameRepository: GameRepository,
              private discountService: DiscountComponent,
              private requirementsService: RequirementsComponent) {

  }

  ngOnInit(): void {
    this.parentForm.valueChanges.subscribe(val => {
      this.parentForm.patchValue(val, {emitEvent: false});
    });
  }

  sendToParent(data: any) {
    this.outputToParent.emit(data);
  }

  getGreenPlayerHand(): Card[] {
    let cards;
    if (this.game.player.canBuildAnotherGreenWith9Discount) {
      cards = this.game?.player.hand.filter(card => {
        return card.cardColor === CardColor[CardColor.GREEN] && card.price < 10;
      });
    } else {
      cards = this.game?.player.hand.filter(card => card.cardColor === CardColor[CardColor.GREEN]);
    }

    this.requirementsService.sortCardsForBuilding(cards, this.game.player, this.game);

    return cards;
  }

  canBuildCard(card: Card) {
    return this.requirementsService.canBuildCard(card, this.game.player, this.game);
  }

  getMicrobePlayedCards(): Card[] {
    return this.game?.player.played.filter(card => card.cardResource === CardResource[CardResource.MICROBE]);
  }

  getAnimalPlayedCards(): Card[] {
    return this.game?.player.played.filter(card => card.cardResource === CardResource[CardResource.ANIMAL]);
  }

  getMicrobeAnimalPlayedCardsWithSelected(): Card[] {
    const playedCopy = [this.selectedProject];
    this.game?.player.played.forEach(e => playedCopy.push(e));
    return playedCopy.filter(card => card.cardResource === CardResource[CardResource.ANIMAL]
      || card.cardResource === CardResource[CardResource.MICROBE]);
  }

  expectsMicrobeOnBuildEffectInput(): boolean {
    return this.selectedProject && this.selectedProject?.resourcesOnBuild.some(resource =>
      resource.type === CardResource[CardResource.MICROBE]
    );
  }

  expectsAnimalOnBuildEffectInput(): boolean {
    return this.selectedProject && this.selectedProject?.resourcesOnBuild.some(resource =>
      resource.type === CardResource[CardResource.ANIMAL]
    );
  }

  getMicrobeOnBuildEffectInputParamId(): number {
    return this.selectedProject?.resourcesOnBuild.find(resource =>
      resource.type === CardResource[CardResource.MICROBE]
    ).paramId;
  }

  getAnimalOnBuildEffectInputParamId(): number {
    return this.selectedProject?.resourcesOnBuild.find(resource =>
      resource.type === CardResource[CardResource.ANIMAL]
    ).paramId;
  }

  clickProjectToBuild(card: Card) {
    if (this.selectedProject && this.selectedProject.id === card.id) {
      this.selectedProject = null;
      this.resetAllInputs();
    } else {
      this.selectedProject = card;
      this.parentForm.controls.mcPrice.setValue(
        this.getDiscountedMcPriceWithEffectsApplied(
          this.parentForm.value.anaerobicMicroorganisms,
          this.parentForm.value.restructuredResources)
      );
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

  greenCardClass(card: Card): string {
    if (this.selectedProject && this.selectedProject.id === card.id) {
      return 'clicked-card';
    }
    if (!this.canBuildCard(card)) {
      return 'unavailable-card';
    }
    return '';
  }

  getDiscountedMcPriceOfSelectedProject(): number {
    if (this.selectedProject) {
      return this.selectedProject.price - this.discountService.getDiscount(this.selectedProject, this.game?.player);
    } else {
      return 0;
    }
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

  upgradePhaseCardEffect(): boolean {
    return this.selectedProject.cardAction === CardAction[CardAction.UPDATE_PHASE_CARD];
  }

  viralEnhancersEffect(): boolean {
    return (this.selectedProject.cardAction === CardAction[CardAction.VIRAL_ENHANCERS]
      || this.game.player.played.some(card => card.cardAction === CardAction[CardAction.VIRAL_ENHANCERS]))
      && this.selectedProject.tags.some(tag =>
        tag === Tag[Tag.PLANT] || tag === Tag[Tag.MICROBE] || tag === Tag[Tag.ANIMAL]
      );
  }

  getPlayerHandWithoutSelectedCard(): Card[] {
    return this.game.player.hand.filter(
      card => card.id !== this.selectedProject.id
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

  canPayWithHeat(): boolean {
    return this.game.player.played.some(card => card.cardAction === CardAction.HELION_CORPORATION)
      && this.game.player.heat > 0;
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

  resetAllInputs() {
    this.errorMessage = null;
    this.selectedProject = null;
    this.onBuildMicrobeChoice = null;
    this.onBuildAnimalChoice = null;
    this.viralEnhancersTargetCards = null;
    this.projectsToDiscard = [];
    this.phaseUpgradeType = null;
  }

  phaseCounter(count: number) {
    return new Array(count);
  }

  phaseBonusUp1Description(phase: number) {
    switch (phase) {
      case 1:
        return PhaseDescription.PHASE_1_BONUS[1];
      case 2:
        return PhaseDescription.PHASE_2_BONUS[1];
      case 3:
        return PhaseDescription.PHASE_3_BONUS[1];
      case 4:
        return PhaseDescription.PHASE_4_BONUS[1];
      case 5:
        return PhaseDescription.PHASE_5_BONUS[1];
    }
  }

  phaseBonusUp2Description(phase: number) {
    switch (phase) {
      case 1:
        return PhaseDescription.PHASE_1_BONUS[2];
      case 2:
        return PhaseDescription.PHASE_2_BONUS[2];
      case 3:
        return PhaseDescription.PHASE_3_BONUS[2];
      case 4:
        return PhaseDescription.PHASE_4_BONUS[2];
      case 5:
        return PhaseDescription.PHASE_5_BONUS[2];
    }
  }

  buildGreenProject(callback: (value: any) => void) {
    if (!this.selectedProject) {
      this.errorMessage = 'No project selected';
      return;
    }

    if (this.parentForm.value.turn === 'greenProject' && this.parentForm.value.mcPrice !== null) {
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

      if (this.expectsMicrobeOnBuildEffectInput()) {
        if (this.parentForm.value.onBuildMicrobeEffectChoice === 'chooseMicrobe') {
          if (!this.onBuildMicrobeChoice) {
            this.errorMessage = 'You need to choose a microbe card';
            return;
          }
          inputParams[this.getMicrobeOnBuildEffectInputParamId()] = [this.onBuildMicrobeChoice.id];
        } else if (this.parentForm.value.onBuildMicrobeEffectChoice === 'skipMicrobe') {
          inputParams[this.getMicrobeOnBuildEffectInputParamId()] = [InputFlag.SKIP_ACTION.valueOf()];
        }
      }

      if (this.expectsAnimalOnBuildEffectInput()) {
        if (this.parentForm.value.onBuildAnimalEffectChoice === 'chooseAnimal') {
          if (!this.onBuildAnimalChoice) {
            this.errorMessage = 'You need to choose an animal card';
            return;
          }
          inputParams[this.getAnimalOnBuildEffectInputParamId()] = [this.onBuildAnimalChoice.id];
        } else if (this.parentForm.value.onBuildAnimalEffectChoice === 'skipAnimal') {
          inputParams[this.getAnimalOnBuildEffectInputParamId()] = [-1];
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

      if (this.upgradePhaseCardEffect()) {
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

      const payments = [new Payment(this.parentForm.value.mcPrice, PaymentType.MEGACREDITS)];
      if (this.canPayWithHeat() && this.parentForm.value.heatPrice > 0) {
        payments.push(new Payment(this.parentForm.value.heatPrice, PaymentType.HEAT));
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

      this.gameRepository.buildGreenProject(request).subscribe(data => {
        this.sendToParent(data);
        this.resetAllInputs();
        callback(data);
      }, error => {
        this.errorMessage = error;
      });
    }
  }

}
