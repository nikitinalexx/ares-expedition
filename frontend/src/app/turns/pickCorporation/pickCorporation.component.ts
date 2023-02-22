import {Component, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {Game} from '../../data/Game';
import {Card} from '../../data/Card';
import {GameRepository} from '../../model/gameRepository.model';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {TurnType} from '../../data/TurnType';
import {DiscardCardsTurn} from '../../data/DiscardCardsTurn';
import {CardColor} from '../../data/CardColor';
import {BuildGreenComponent} from '../greenProject/buildGreen.component';
import {SellCardsComponent} from '../sellCards/sellCards.component';
import {BuildBlueRedComponent} from "../blueRedProject/buildBlueRed.component";
import {CardAction} from "../../data/CardAction";
import {InputFlag} from "../../data/InputFlag";
import {Tag} from "../../data/Tag";

@Component({
  selector: 'app-pick-corporation',
  templateUrl: './pickCorporation.component.html'
})
export class PickCorporationComponent implements OnInit {
  allTags = [Tag.SPACE, Tag.EARTH, Tag.EVENT, Tag.SCIENCE, Tag.PLANT,
    Tag.ENERGY, Tag.BUILDING, Tag.ANIMAL, Tag.JUPITER, Tag.MICROBE];
  public corporationInput: Card;
  public errorMessage: string;
  projectsToDiscard: number[];
  phaseInput = 0;
  phaseUpgradeType = -1;
  tagInput = -1;
  milestoneInput = -1;

  @ViewChild(BuildGreenComponent) buildGreenService;
  @ViewChild(BuildBlueRedComponent) buildBlueRedService;
  @ViewChild(SellCardsComponent) sellCardsService;
  parentForm: FormGroup;


  @Input()
  game: Game;
  @Input()
  nextTurns: TurnType[];
  @Output() outputToParent = new EventEmitter<any>();

  constructor(private gameRepository: GameRepository,
              private formBuilder: FormBuilder) {

  }

  ngOnInit() {
    this.parentForm = this.formBuilder.group({
      turn: ['pickCorporation', Validators.required],

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

    if (this.mulliganTurn()) {
      this.parentForm.controls.turn.setValue('sellCards');
    }

    if (this.discardCardsTurn()) {
      this.parentForm.controls.turn.setValue('discardCards');
    }
  }

  getCorporationCards(): Card[] {
    return this.game?.player.corporations;
  }

  getProjects(): Card[] {
    return this.game?.player.hand;
  }

  clickCorporation(card: Card) {
    if (this.parentForm.value?.turn === 'pickCorporation' && !this.game.player.corporationId) {
      this.corporationInput = card;
      this.errorMessage = null;
      if (this.upgradePhaseCardEffect()) {
        this.phaseInput = this.getUpgradePhasesArray()[0] - 1;
      }
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

  pickCorporationTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.PICK_CORPORATION])?.length > 0;
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

  mulliganTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.MULLIGAN])?.length > 0;
  }

  skipTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.SKIP_TURN])?.length > 0;
  }

  resetAllInputs() {
    if (this.buildGreenService) {
      this.buildGreenService.resetAllInputs();
    }
    if (this.buildBlueRedService) {
      this.buildBlueRedService.resetAllInputs();
    }
    if (this.sellCardsService) {
      this.sellCardsService.resetAllInputs();
    }
  }

  getDiscardCards(): Card[] {
    const nextTurn = this.game.player.nextTurn as DiscardCardsTurn;
    if (nextTurn.onlyFromSelectedCards) {
      return this.game.player.nextTurn.cards;
    } else {
      return this.game.player?.hand;
    }
  }

  upgradePhaseCardEffect(): boolean {
    return this.corporationInput?.cardAction === CardAction[CardAction.SULTIRA_CORPORATION]
      || this.corporationInput?.cardAction === CardAction[CardAction.HYPERION_SYSTEMS_CORPORATION]
      || this.corporationInput?.cardAction === CardAction[CardAction.EXOCORP_CORPORATION]
      || this.corporationInput?.cardAction === CardAction[CardAction.APOLLO_CORPORATION];
  }

  updatePhaseInput(newPhaseInput: number) {
    this.phaseInput = newPhaseInput;
  }

  updatePhaseUpgradeTypeInput(newPhaseUpgradeType: number) {
    this.phaseUpgradeType = newPhaseUpgradeType;
  }

  getUpgradePhasesArray(): number[] {
    if (this.corporationInput?.cardAction === CardAction.SULTIRA_CORPORATION) {
      return [1];
    }
    if (this.corporationInput?.cardAction === CardAction.APOLLO_CORPORATION) {
      return [2];
    }
    if (this.corporationInput?.cardAction === CardAction.HYPERION_SYSTEMS_CORPORATION) {
      return [3];
    }
    if (this.corporationInput?.cardAction === CardAction.EXOCORP_CORPORATION) {
      return [5];
    }
    return [1, 2, 3, 4, 5];
  }

  expectsTagInput(): boolean {
    return this.corporationInput?.cardAction === CardAction[CardAction.AUSTELLAR_CORPORATION];
  }

  expectsMilestoneInput(): boolean {
    return this.corporationInput?.cardAction === CardAction[CardAction.AUSTELLAR_CORPORATION];
  }

  getAllTagsArray(): Tag[] {
    return this.allTags;
  }

  getTagClasses(tagNumber: number): string {
    return 'tag-' + this.allTags[tagNumber].toString().toLowerCase();
  }


  submitForm(formGroup: FormGroup) {
    this.errorMessage = null;
    if (!formGroup.valid) {
      console.log('form invalid');
      return false;
    } else {
      if (formGroup.value.turn === 'pickCorporation') {
        if (!this.corporationInput) {
          this.errorMessage = 'Pick corporation';
        } else {
          const inputParams = new Map<number, number[]>();

          if (this.expectsTagInput()) {
            if (this.tagInput < 0) {
              this.errorMessage = 'Choose a tag to put on a Card';
              return;
            }
            inputParams[InputFlag.TAG_INPUT.valueOf()] = [this.tagInput];
          }

          if (this.expectsMilestoneInput()) {
            if (this.milestoneInput < 0) {
              this.errorMessage = 'Choose a milestone';
              return;
            }
            inputParams[InputFlag.AUSTELLAR_CORPORATION_MILESTONE.valueOf()] = [this.milestoneInput];
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
          this.gameRepository.pickCorporation(this.game.player.playerUuid, this.corporationInput.id, inputParams)
            .subscribe(data => this.sendToParent(data), error => this.errorMessage = error);
        }
      } else if (formGroup.value.turn === 'discardCards') {
        if (!this.projectsToDiscard || this.projectsToDiscard.length !== this.game.player.nextTurn.size) {
          this.errorMessage = 'Invalid number of cards to discard';
        } else {
          this.gameRepository.discardCards(this.game.player.playerUuid, this.projectsToDiscard).subscribe(
            data => {
              this.sendToParent(data);
              this.projectsToDiscard = [];
              this.errorMessage = null;
            },
            error => {
              this.errorMessage = error;
            }
          );
        }
      } else if (formGroup.value.turn === 'greenProject' && formGroup.value.mcPrice !== null) {
        this.buildGreenService.buildGreenProject(data => this.sendToParent(data));
      } else if (formGroup.value.turn === 'blueRedProject' && formGroup.value.mcPrice !== null) {
        this.buildBlueRedService.buildBlueRedProject(data => this.sendToParent(data));
      } else if (formGroup.value.turn === 'skipTurn') {
        this.gameRepository.skipTurn(this.game.player.playerUuid).subscribe(
          data => this.sendToParent(data)
        );
      } else if (formGroup.value.turn === 'sellCards') {
        // mulligan
        this.sellCardsService.sellCards(this.game, data => {
          this.sendToParent(data);
          this.parentForm.patchValue({
            turn: 'pickCorporation'
          });
        });
      }
    }
  }

  sendToParent(data: any) {
    this.outputToParent.emit(data);
  }

  isSelectedCard(card: Card): string {
    if (this.corporationInput && card.id === this.corporationInput.id) {
      return 'clicked-card';
    }
    return '';
  }

}
