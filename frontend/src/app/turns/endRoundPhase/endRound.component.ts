import {Component, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {Game} from '../../data/Game';
import {GameRepository} from '../../model/gameRepository.model';
import {TurnType} from '../../data/TurnType';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {SellCardsComponent} from '../sellCards/sellCards.component';

@Component({
  selector: 'app-end-round',
  templateUrl: './endRound.component.html'
})
export class EndRoundComponent implements OnInit {
  public errorMessage: string;
  isSubmitted = false;
  projectsToDiscard: number[];
  parentForm: FormGroup;

  @Input()
  game: Game;
  @Input()
  nextTurns: TurnType[];
  @Output() outputToParent = new EventEmitter<any>();
  @ViewChild(SellCardsComponent) sellCardsService;

  constructor(private gameRepository: GameRepository,
              private formBuilder: FormBuilder) {

  }

  ngOnInit() {
    this.parentForm = this.formBuilder.group({
      turn: ['sellCards', Validators.required]
    });
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
      console.log('form invalid');
      return false;
    } else {
      if (formGroup.value.turn === 'sellCards') {
        this.sellCardsService.sellCards(this.game, data => this.sendToParent(data));
      }
    }
  }

}
