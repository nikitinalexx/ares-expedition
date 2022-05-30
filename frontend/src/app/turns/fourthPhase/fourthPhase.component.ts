import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Game} from '../../data/Game';
import {GameRepository} from '../../model/gameRepository.model';
import {TurnType} from '../../data/TurnType';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {DiscountComponent} from '../../discount/discount.component';

@Component({
  selector: 'app-fourth-phase',
  templateUrl: './fourthPhase.component.html',
  styleUrls: ['../turns.component.css']
})
export class FourthPhaseComponent implements OnInit {
  isSubmitted = false;
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
      turn: ['collectIncome', Validators.required]
    });
  }

  sendToParent(data: any) {
    this.outputToParent.emit(data);
  }

  collectIncomeTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.COLLECT_INCOME])?.length > 0;
  }

  submitForm(formGroup: FormGroup) {
    this.isSubmitted = true;
    if (!formGroup.valid) {
      console.log('form invalid');
      return false;
    } else {
      if (formGroup.value.turn === 'collectIncome') {
        this.gameRepository.collectIncome(this.game.player.playerUuid).subscribe(
          data => this.sendToParent(data)
        );
      }
    }
  }

}
