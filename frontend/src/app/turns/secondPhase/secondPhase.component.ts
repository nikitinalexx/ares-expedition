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
import {CardAction} from "../../data/CardAction";
import {Tag} from "../../data/Tag";
import {InputFlag} from "../../data/InputFlag";

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
      anaerobicMicroorganisms: [false],
      marsUniversityDiscardLess: [false],
      takeMicrobes: 0,
      takeCards: 0
    });
  }

  sendToParent(data: any) {
    this.outputToParent.emit(data);
  }

  buildBlueRedProjectTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.BUILD_BLUE_RED_PROJECT])?.length > 0;
  }

  discardCardsTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.DISCARD_CARDS])?.length > 0;
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

  getPlayerHandWithoutSelectedCard(): Card[] {
    return this.game.player.hand.filter(
      card => card.id !== this.selectedProject.id
    );
  }

  getBlueRedPlayerHand(): Card[] {
    return this.game?.player.hand.filter(
      card => card.cardColor === CardColor[CardColor.BLUE] || card.cardColor === CardColor[CardColor.RED]
    );
  }

  getDiscardCards(): Card[] {
    return this.game.player.nextTurn.cards;
  }

  clickProjectToBuild(card: Card) {
    if (this.selectedProject && this.selectedProject.id === card.id) {
      this.selectedProject = null;
    } else {
      this.selectedProject = card;
      this.parentForm.controls.mcPrice.setValue(Math.max(
        0, this.getDiscountedMcPriceOfSelectedProject() - (this.parentForm.value.anaerobicMicroorganisms ? 10 : 0)
      ));
    }
  }

  anaerobicMicroorganismsClicked($event: any) {
    if ($event.target.checked) {
      this.parentForm.controls.mcPrice.setValue(Math.max(0, this.getDiscountedMcPriceOfSelectedProject() - 10));
    } else {
      this.parentForm.controls.mcPrice.setValue(this.getDiscountedMcPriceOfSelectedProject());
    }
  }

  marsUniversityDiscardLessClicked($event: any) {
    if ($event.target.checked) {
      this.parentForm.controls.mcPrice.setValue(Math.max(0, this.getDiscountedMcPriceOfSelectedProject() - 10));
    } else {
      this.parentForm.controls.mcPrice.setValue(this.getDiscountedMcPriceOfSelectedProject());
    }
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

  getDiscountedMcPriceOfSelectedProject(): number {
    if (this.selectedProject) {
      return this.selectedProject.price - this.discountService.getDiscount(this.selectedProject, this.game);
    } else {
      return 0;
    }
  }

  onValueClick() {
    this.selectedProject = null;
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

  marsUniversityEffect(): boolean {
    return (this.selectedProject.cardAction === CardAction[CardAction.MARS_UNIVERSITY]
      || this.game.player.played.some(card => card.cardAction === CardAction[CardAction.MARS_UNIVERSITY]))
      && this.selectedProject.tags.some(tag => tag === Tag[Tag.SCIENCE]);
  }

  expectsDecomposersInput(): boolean {
    return this.selectedProject.cardAction === CardAction[CardAction.DECOMPOSERS]
      ||
      this.game.player.played.some(card => card.cardAction === CardAction[CardAction.DECOMPOSERS])
      && (this.game.player.played.some(card => card.tags.some(tag =>
          tag === Tag[Tag.ANIMAL] || tag === Tag[Tag.MICROBE] || tag === Tag[Tag.PLANT]))
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
      } else if (formGroup.value.turn === 'sellCards') {
        this.sellCardsService.sellCards(this.game);
        this.sendToParent(null);
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

        const payments = [new Payment(formGroup.value.mcPrice, PaymentType.MEGACREDITS)];

        if (this.parentForm.value.anaerobicMicroorganisms) {
          payments.push(new Payment(2, PaymentType.ANAEROBIC_MICROORGANISMS));
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
