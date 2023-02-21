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
import {CardAction} from '../../data/CardAction';
import {Tag} from '../../data/Tag';
import {InputFlag} from '../../data/InputFlag';
import {CardResource} from '../../data/CardResource';
import {RequirementsComponent} from '../../requirements/requirements.component';

@Component({
  selector: 'app-build-blue-red',
  templateUrl: './buildBlueRed.component.html',
  styleUrls: ['../turns.component.css']
})
export class BuildBlueRedComponent implements OnInit {
  public errorMessage: string;
  allTags = [Tag.SPACE, Tag.EARTH, Tag.EVENT, Tag.SCIENCE, Tag.PLANT,
    Tag.ENERGY, Tag.BUILDING, Tag.ANIMAL, Tag.JUPITER, Tag.MICROBE];
  selectedProject: Card;
  projectsToDiscard: number[];
  viralEnhancersTargetCards: number[];
  onBuildResourceChoice = null;
  importedHydrogenMicrobeAnimal = null;
  importedNitrogenMicrobeCard = null;
  importedNitrogenAnimalCard = null;
  largeConvoyAnimalCard = null;
  localHeatTrappingCard = null;
  phaseInput = 0;
  phaseUpgradeType = -1;
  extraPhaseInput = 0;
  extraPhaseUpgradeType = -1;
  tagInput = -1;

  @Input()
  game: Game;
  @Input()
  parentForm: FormGroup;
  @Output() outputToParent = new EventEmitter<any>();

  constructor(private gameRepository: GameRepository,
              private discountService: DiscountComponent,
              private requirementsService: RequirementsComponent) {

  }

  ngOnInit() {
    this.parentForm.valueChanges.subscribe(val => {
      this.parentForm.patchValue(val, {emitEvent: false});
    });
  }

  sendToParent(data: any) {
    this.outputToParent.emit(data);
  }

  getPlayerHandWithoutSelectedCard(): Card[] {
    return this.game.player.hand.filter(
      card => card.id !== this.selectedProject.id
    );
  }

  getRedPlayedCards(): Card[] {
    return this.game.player.played.filter(
      card => card.cardColor === CardColor.RED
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

  expectsTagInput(): boolean {
    return this.selectedProject?.cardAction === CardAction[CardAction.CHOOSE_TAG] ||
      this.selectedProject?.cardAction === CardAction[CardAction.TOPOGRAPHIC_MAPPING];
  }

  expectsBiomedicalImportsInput(): boolean {
    return this.selectedProject?.cardAction === CardAction[CardAction.BIOMEDICAL_IMPORTS];
  }

  getBlueRedPlayerHand(): Card[] {
    const cards = this.game.player.hand.filter(
      card => card.cardColor === CardColor[CardColor.BLUE] || card.cardColor === CardColor[CardColor.RED]
    );

    this.requirementsService.sortCardsForBuilding(cards, this.game.player, this.game);

    return cards;
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

  clickTagChoice(tagIndex: number) {
    this.tagInput = tagIndex;
    this.parentForm.controls.mcPrice.setValue(
      this.getDiscountedMcPriceWithEffectsApplied(
        this.parentForm.value.anaerobicMicroorganisms,
        this.parentForm.value.restructuredResources)
    );
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

  canBuildCard(card: Card) {
    return this.requirementsService.canBuildCard(card, this.game.player, this.game);
  }

  blueRedCardClass(card: Card): string {
    if (this.selectedProject && this.selectedProject.id === card.id) {
      return 'clicked-card';
    }
    if (!this.canBuildCard(card)) {
      return 'unavailable-card';
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
      return this.selectedProject.price -
        this.discountService.getDiscount(this.selectedProject, this.game?.player, this.expectsTagInput() ? this.tagInput : -1);
    } else {
      return 0;
    }
  }

  resetAllInputs() {
    this.errorMessage = null;
    this.selectedProject = null;
    this.onBuildResourceChoice = null;
    this.importedHydrogenMicrobeAnimal = null;
    this.importedNitrogenAnimalCard = null;
    this.importedNitrogenMicrobeCard = null;
    this.largeConvoyAnimalCard = null;
    this.localHeatTrappingCard = null;
    this.projectsToDiscard = [];
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
      && (this.selectedProject.tags.some(tag => tag === Tag[Tag.SCIENCE] || this.countTagsUsedAsInput([Tag.SCIENCE]) > 0));
  }

  syntheticCatastropheEffect(): boolean {
    return (this.selectedProject.cardAction === CardAction[CardAction.SYNTHETIC_CATASTROPHE]);
  }

  viralEnhancersEffect(): boolean {
    return (this.selectedProject.cardAction === CardAction[CardAction.VIRAL_ENHANCERS]
      || this.game.player.played.some(card => card.cardAction === CardAction[CardAction.VIRAL_ENHANCERS]))
      && this.selectedProject.tags.some(tag =>
        tag === Tag[Tag.PLANT] || tag === Tag[Tag.MICROBE] || tag === Tag[Tag.ANIMAL]
        || this.countTagsUsedAsInput([Tag.PLANT, Tag.MICROBE, Tag.ANIMAL]) > 0
      );
  }

  importedHydrogenEffect(): boolean {
    return this.selectedProject.cardAction === CardAction[CardAction.IMPORTED_HYDROGEN];
  }

  cryogenicShipmentEffect(): boolean {
    return this.selectedProject.cardAction === CardAction[CardAction.CRYOGENIC_SHIPMENT];
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
        tag === Tag[Tag.ANIMAL] || tag === Tag[Tag.MICROBE] || tag === Tag[Tag.PLANT]
        || this.countTagsUsedAsInput([Tag.ANIMAL, Tag.MICROBE, Tag.PLANT]) > 0
      ));
  }

  decomposersCanTakeCard(): boolean {
    const microbeCount = this.game.player.cardResources[this.game.player.played.find(
      card => card.cardAction === CardAction[CardAction.DECOMPOSERS]
    )?.id];
    return microbeCount && microbeCount >= 1;
  }

  canPayWithHeat(): boolean {
    return this.game.player.played.some(card => card.cardAction === CardAction.HELION_CORPORATION)
      && this.game.player.heat > 0;
  }

  getAllTagsArray(): Tag[] {
    return this.allTags;
  }

  getTagClasses(tagNumber: number): string {
    return 'tag-' + this.allTags[tagNumber].toString().toLowerCase();
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

  upgradePhaseCardEffect(): boolean {
    return this.selectedProject.cardAction === CardAction[CardAction.UPDATE_PHASE_CARD]
      || this.selectedProject.cardAction === CardAction[CardAction.UPDATE_PHASE_3_CARD]
      || this.selectedProject.cardAction === CardAction[CardAction.COMMUNICATIONS_STREAMLINING]
      || this.selectedProject.cardAction === CardAction[CardAction.TOPOGRAPHIC_MAPPING]
      || this.selectedProject.cardAction === CardAction[CardAction.UPDATE_PHASE_CARD_TWICE]
      || this.selectedProject.cardAction === CardAction[CardAction.CRYOGENIC_SHIPMENT]
      || this.selectedProject.cardAction === CardAction[CardAction.UPDATE_PHASE_2_CARD]
      || this.expectsBiomedicalImportsInput() && this.parentForm.value.biomedicalImports === 'phase';
  }

  upgradeExtraPhaseCardEffect(): boolean {
    return this.selectedProject.cardAction === CardAction[CardAction.UPDATE_PHASE_CARD_TWICE];
  }

  getUpgradePhasesArray(): number[] {
    if (this.selectedProject.cardAction === CardAction[CardAction.UPDATE_PHASE_3_CARD]
      || this.selectedProject.cardAction === CardAction[CardAction.COMMUNICATIONS_STREAMLINING]) {
      return [3];
    }
    if (this.selectedProject.cardAction === CardAction[CardAction.UPDATE_PHASE_2_CARD]) {
      return [2];
    }
    return [1, 2, 3, 4, 5];
  }

  updatePhaseInput(newPhaseInput: number) {
    this.phaseInput = newPhaseInput;
  }

  updatePhaseUpgradeTypeInput(newPhaseUpgradeType: number) {
    this.phaseUpgradeType = newPhaseUpgradeType;
  }

  updateExtraPhaseInput(newPhaseInput: number) {
    this.extraPhaseInput = newPhaseInput;
  }

  updateExtraPhaseUpgradeTypeInput(newPhaseUpgradeType: number) {
    this.extraPhaseUpgradeType = newPhaseUpgradeType;
  }

  countTagsUsedAsInput(tags: Tag[]): number {
    if (this.expectsTagInput() && this.tagInput >= 0) {
      return tags.filter(t => t === this.allTags[this.tagInput])?.length;
    }
    return 0;
  }

  buildBlueRedProject(callback: (value: any) => void) {
    if (!this.selectedProject) {
      this.errorMessage = 'No project selected';
      return;
    }

    this.errorMessage = null;
    if (!this.parentForm.valid) {
      console.log('form invalid');
      return false;
    }

    if (this.parentForm.value.turn === 'blueRedProject' && this.parentForm.value.mcPrice !== null) {
      const inputParams = new Map<number, number[]>();
      if (this.marsUniversityEffect()) {
        const scienceTagsCount = this.selectedProject.tags.filter(tag => tag === Tag[Tag.SCIENCE]).length
          + this.countTagsUsedAsInput([Tag.SCIENCE]);
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

      if (this.syntheticCatastropheEffect()) {
        if (!this.projectsToDiscard || this.projectsToDiscard.length !== 1) {
          this.errorMessage = 'Synthetic Catastrophe may retrieve only 1 card';
          return;
        }
        inputParams[InputFlag.SYNTHETIC_CATASTROPHE_CARD.valueOf()] = this.projectsToDiscard;
      }

      if (this.expectsDecomposersInput()) {
        let expectedInputSum = this.selectedProject.tags.filter(
          tag => tag === Tag[Tag.ANIMAL] || tag === Tag[Tag.MICROBE] || tag === Tag[Tag.PLANT]
        ).length;
        expectedInputSum += this.countTagsUsedAsInput([Tag.ANIMAL, Tag.MICROBE, Tag.PLANT]);
        const takeMicrobes = this.parentForm.value.takeMicrobes ? this.parentForm.value.takeMicrobes : 0;
        const takeCards = this.parentForm.value.takeCards ? this.parentForm.value.takeCards : 0;

        if (expectedInputSum !== (takeMicrobes + takeCards)) {
          this.errorMessage = 'Sum of taken microbes and cards doesn\'t correspond to tag sum';
          return;
        }
        inputParams[InputFlag.DECOMPOSERS_TAKE_MICROBE.valueOf()] = [takeMicrobes];
        inputParams[InputFlag.DECOMPOSERS_TAKE_CARD.valueOf()] = [takeCards];
      }

      if (this.expectsBiomedicalImportsInput()) {
        if (this.parentForm.value.biomedicalImports !== 'oxygen' && this.parentForm.value.biomedicalImports !== 'phase') {
          this.errorMessage = 'Biomedical Imports expects Oxygen or Update phase input';
        }
        if (this.parentForm.value.biomedicalImports === 'oxygen') {
          inputParams[InputFlag.BIOMEDICAL_IMPORTS_RAISE_OXYGEN.valueOf()] = [-1];
        } else {
          inputParams[InputFlag.BIOMEDICAL_IMPORTS_UPGRADE_PHASE.valueOf()] = [-1];
        }
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

      if (this.expectsTagInput()) {
        if (this.tagInput < 0) {
          this.errorMessage = 'Choose a tag to put on a Card';
          return;
        }
        inputParams[InputFlag.TAG_INPUT.valueOf()] = [this.tagInput];
      }

      if (this.viralEnhancersEffect()) {
        const expectedInputSum = this.selectedProject.tags.filter(
          tag => tag === Tag[Tag.ANIMAL] || tag === Tag[Tag.MICROBE] || tag === Tag[Tag.PLANT]
        ).length + this.countTagsUsedAsInput([Tag.PLANT, Tag.MICROBE, Tag.ANIMAL]);
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

      if (this.cryogenicShipmentEffect()) {
        if (this.parentForm.value.importedHydrogenForm === 'skipMicrobeAnimal') {
          inputParams[InputFlag.CRYOGENIC_SHIPMENT_PUT_RESOURCE.valueOf()] = [InputFlag.SKIP_ACTION.valueOf()];
        } else {
          console.log(this.parentForm.value.importedHydrogenForm);
          if (!this.parentForm.value.importedHydrogenForm
            || this.parentForm.value.importedHydrogenForm !== 'microbeAnimal'
            || this.parentForm.value.importedHydrogenForm === 'microbeAnimal'
            && !this.importedHydrogenMicrobeAnimal) {
            this.errorMessage = 'You need to choose an Animal/Microbe card';
            return;
          }
          inputParams[InputFlag.CRYOGENIC_SHIPMENT_PUT_RESOURCE.valueOf()] = [this.importedHydrogenMicrobeAnimal.id];
        }
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

      if (this.upgradeExtraPhaseCardEffect()) {
        if (this.extraPhaseInput < 0 || this.extraPhaseInput > 4) {
          this.errorMessage = 'Pick the extra phase you want to upgrade';
          return;
        }
        if (this.extraPhaseUpgradeType !== 0 && this.extraPhaseUpgradeType !== 1) {
          this.errorMessage = 'Choose the type of extra phase upgrade';
          return;
        }
        inputParams[InputFlag.PHASE_UPGRADE_CARD.valueOf()].push(this.extraPhaseInput * 2 + this.extraPhaseUpgradeType);
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

      this.gameRepository.buildBlueRedProject(request).subscribe(data => {
        this.sendToParent(data);
        this.resetAllInputs();
        callback(data);
      }, error => {
        this.errorMessage = error;
      });
    }
  }

}
