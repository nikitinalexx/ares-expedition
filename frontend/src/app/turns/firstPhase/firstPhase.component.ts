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
import {CardResource} from '../../data/CardResource';
import {CardAction} from "../../data/CardAction";

@Component({
  selector: 'app-first-phase',
  templateUrl: './firstPhase.component.html',
  styleUrls: ['../turns.component.css']
})
export class FirstPhaseComponent implements OnInit {
  public errorMessage: string;
  isSubmitted = false;
  selectedProject: Card;
  onBuildMicrobeChoice = null;
  onBuildAnimalChoice = null;
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
      onBuildMicrobeEffectChoice: ['chooseMicrobe'],
      onBuildAnimalEffectChoice: ['chooseAnimal'],
      anaerobicMicroorganisms: [false]
    });
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

  getMicrobePlayedCards(): Card[] {
    return this.game?.player.played.filter(card => card.cardResource === CardResource[CardResource.MICROBE]);
  }

  getAnimalPlayedCards(): Card[] {
    return this.game?.player.played.filter(card => card.cardResource === CardResource[CardResource.ANIMAL]);
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

  selectedProjectToBuildClass(card: Card): string {
    if (this.selectedProject && this.selectedProject.id === card.id) {
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


  resetAllInputs() {
    this.selectedProject = null;
    this.onBuildMicrobeChoice = null;
    this.onBuildAnimalChoice = null;
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
      } else if (formGroup.value.turn === 'greenProject' && formGroup.value.mcPrice !== null) {
        const inputParams = new Map<number, number[]>();

        if (this.expectsMicrobeOnBuildEffectInput()) {
          if (this.parentForm.value.onBuildMicrobeEffectChoice === 'chooseMicrobe') {
            if (!this.onBuildMicrobeChoice) {
              this.errorMessage = 'You need to choose a microbe card';
              return;
            }
            inputParams[this.getMicrobeOnBuildEffectInputParamId()] = [this.onBuildMicrobeChoice.id];
          } else if (this.parentForm.value.onBuildMicrobeEffectChoice === 'skipMicrobe') {
            inputParams[this.getMicrobeOnBuildEffectInputParamId()] = [-1];
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
