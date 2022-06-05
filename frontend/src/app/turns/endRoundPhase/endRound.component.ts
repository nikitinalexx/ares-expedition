import {Component, EventEmitter, HostListener, Input, OnInit, Output, ViewChild} from '@angular/core';
import {Game} from '../../data/Game';
import {GameRepository} from '../../model/gameRepository.model';
import {TurnType} from '../../data/TurnType';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {SellCardsComponent} from '../sellCards/sellCards.component';

@Component({
  selector: 'app-end-round',
  templateUrl: './endRound.component.html',
  styleUrls: ['endRound.component.css']
})
export class EndRoundComponent implements OnInit {
  public errorMessage: string;
  isSubmitted = false;
  parentForm: FormGroup;
  hideCardsTooltip = true;

  @ViewChild(SellCardsComponent) sellCardsService;

  @Input()
  game: Game;
  @Input()
  nextTurns: TurnType[];
  @Output() outputToParent = new EventEmitter<any>();

  @HostListener('window:scroll', [])
  onWindowScroll() {
    this.hideCardsTooltip = window.scrollY < 250;
  }


  constructor(private gameRepository: GameRepository,
              private formBuilder: FormBuilder) {

  }

  ngOnInit() {
    this.parentForm = this.formBuilder.group({
      turn: ['sellCards', Validators.required]
    });
  }

  selectedEnoughCardsToDiscard(): boolean {
    if (!this.sellCardsService?.cardsToCell) {
      return false;
    }

    return this.sellCardsService.cardsToCell.length >= (this.game.player.hand.length - 10);
  }

  dynamicClasses(): string[] {
    const classes = [];
    const enoughCards = this.selectedEnoughCardsToDiscard();
    classes.push(enoughCards ? 'bg-success' : 'bg-warning');
    classes.push(enoughCards ? 'text-white' : 'text-black');
    if (this.hideCardsTooltip || this.isSubmitted) {
      classes.push('hidden');
    }
    return classes;
  }

  sendToParent(data: any) {
    this.outputToParent.emit(data);
  }

  sellCardsTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(
      turn => turn === TurnType.SELL_CARDS || turn === TurnType.SELL_CARDS_LAST_ROUND
    )?.length > 0;
  }

  submitForm(formGroup: FormGroup) {
    this.isSubmitted = true;
    if (!formGroup.valid) {
      return false;
    } else {
      if (formGroup.value.turn === 'sellCards') {
        this.sellCardsService.sellCards(this.game, data => this.sendToParent(data));
      }
    }
  }

}
