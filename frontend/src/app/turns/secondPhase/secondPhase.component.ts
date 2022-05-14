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

@Component({
  selector: 'app-second-phase',
  templateUrl: './secondPhase.component.html',
  styleUrls: ['../turns.component.css']
})
export class SecondPhaseComponent implements OnInit {
  public errorMessage: string;
  isSubmitted = false;
  selectedProject: Card;
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
      mcPrice: ['']
    });
  }

  sendToParent(data: any) {
    this.outputToParent.emit(data);
  }

  buildBlueRedProjectTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.BUILD_BLUE_RED_PROJECT])?.length > 0;
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

  getBlueRedPlayerHand(): Card[] {
    return this.game?.player.hand.filter(
      card => card.cardColor === CardColor[CardColor.BLUE] || card.cardColor === CardColor[CardColor.RED]
    );
  }

  clickProjectToBuild(card: Card) {
    if (this.selectedProject && this.selectedProject.id === card.id) {
      this.selectedProject = null;
    } else {
      this.selectedProject = card;
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


  onValueClick() {
    this.selectedProject = null;
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
        console.log(formGroup.value);
        const request = new BuildProjectRequest(
          this.game.player.playerUuid,
          this.selectedProject.id,
          [new Payment(formGroup.value.mcPrice, PaymentType.MEGACREDITS)],
          null
        );
        console.log(request);

        this.gameRepository.buildBlueRedProject(request).subscribe(data => {
          this.sendToParent(data);
          this.selectedProject = null;
        }, error => {
          this.errorMessage = error;
        });
      }
    }
  }

}
