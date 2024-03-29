import {Component, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {Game} from '../../data/Game';
import {GameRepository} from '../../model/gameRepository.model';
import {TurnType} from '../../data/TurnType';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {Card} from '../../data/Card';
import {CardColor} from '../../data/CardColor';
import {SellCardsComponent} from '../sellCards/sellCards.component';
import {DiscardCardsTurn} from '../../data/DiscardCardsTurn';
import {BuildGreenComponent} from '../greenProject/buildGreen.component';
import {ScrollComponent} from "../../scroll/scroll.component";

@Component({
  selector: 'app-first-phase',
  templateUrl: './firstPhase.component.html',
  styleUrls: ['../turns.component.css']
})
export class FirstPhaseComponent implements OnInit {
  public errorMessage: string;
  isSubmitted = false;
  selectedProject: Card;
  projectsToDiscard: number[];
  @ViewChild(SellCardsComponent) sellCardsService;
  @ViewChild(BuildGreenComponent) buildGreenService;

  parentForm: FormGroup;

  @Input()
  game: Game;
  @Input()
  nextTurns: TurnType[];
  @Output() outputToParent = new EventEmitter<any>();

  constructor(private gameRepository: GameRepository,
              private formBuilder: FormBuilder,
              private scrollService: ScrollComponent) {

  }

  ngOnInit() {
    this.parentForm = this.formBuilder.group({
      turn: ['greenProject', Validators.required],
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
      biomedicalImports: null,
      cargoShipsHeat: [false]
    });
  }

  sendToParent(data: any) {
    this.outputToParent.emit(data);
  }

  buildGreenProjectTurn(): boolean {
    return this.nextTurns
      && this.nextTurns.find(turn => turn === TurnType[TurnType.BUILD_GREEN_PROJECT])?.length > 0
      && this.game.player.hand.some(card => card.cardColor === CardColor.GREEN);
  }

  sellCardsTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.SELL_CARDS])?.length > 0;
  }

  skipTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.SKIP_TURN])?.length > 0;
  }

  discardCardsTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.DISCARD_CARDS])?.length > 0;
  }

  unmiRtTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.UNMI_RT])?.length > 0;
  }

  sellVpTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.SELL_VP])?.length > 0
      && this.game?.player.winPoints > 0;
  }

  getDiscardCards(): Card[] {
    const nextTurn = this.game.player.nextTurn as DiscardCardsTurn;
    if (nextTurn.onlyFromSelectedCards) {
      return this.game.player.nextTurn.cards;
    } else {
      return this.game.player?.hand;
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

  resetAllInputs() {
    this.selectedProject = null;
    if (this.buildGreenService) {
      this.buildGreenService.resetAllInputs();
    }
    this.scrollService.scrollToPlayerChoice();
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
      } else if (formGroup.value.turn === 'skipTurn') {
        this.gameRepository.skipTurn(this.game.player.playerUuid).subscribe(
          data => this.sendToParent(data)
        );
      } else if (formGroup.value.turn === 'sellCards') {
        this.sellCardsService.sellCards(this.game, data => this.sendToParent(data));
      } else if (formGroup.value.turn === 'greenProject' && formGroup.value.mcPrice !== null) {
        this.buildGreenService.buildGreenProject(data => this.sendToParent(data));
      }
      this.scrollService.scrollToPlayerChoice();
    }
  }

}
