import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Game} from '../../data/Game';
import {GameRepository} from '../../model/gameRepository.model';
import {TurnType} from '../../data/TurnType';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {Card} from "../../data/Card";

@Component({
  selector: 'app-fifth-phase',
  templateUrl: './fifthPhase.component.html'
})
export class FifthPhaseComponent implements OnInit {
  public errorMessage: string;
  isSubmitted = false;
  projectsToSelect: number[];
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
      turn: ['discardCards', Validators.required]
    });
  }

  sendToParent(data: any) {
    this.outputToParent.emit(data);
  }

  discardCardsTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.DISCARD_DRAFTED_CARDS])?.length > 0;
  }

  getDiscardCards(): Card[] {
    return this.game.player.nextTurn?.cards;
  }

  clickProjectToSelect(card: Card) {
    if (!this.projectsToSelect) {
      this.projectsToSelect = [];
    }
    const index = this.projectsToSelect.indexOf(card.id, 0);
    if (index > -1) {
      this.projectsToSelect.splice(index, 1);
    } else {
      this.projectsToSelect.push(card.id);
    }
  }

  selectedProjectToSelectClass(card: Card): string {
    if (this.projectsToSelect && this.projectsToSelect.some(element => element === card.id)) {
      return 'clicked-card';
    }
    return '';
  }

  submitForm(formGroup: FormGroup) {
    this.isSubmitted = true;
    if (!formGroup.valid) {
      console.log('form invalid');
      return false;
    } else {
      if (formGroup.value.turn === 'discardCards') {
        if (!this.projectsToSelect
          || this.projectsToSelect.length !== (this.game.player.nextTurn.cards.length - this.game.player.nextTurn.size)) {
          this.errorMessage = 'Invalid number of cards to take';
        } else {
          const resultArray = [];
          for (const card of this.game.player.nextTurn.cards) {
            if (this.projectsToSelect.every(id => id !== card.id)) {
              resultArray.push(card.id);
            }
          }
          this.gameRepository.discardDraftedCards(this.game.player.playerUuid, resultArray).subscribe(
            data => {
              this.sendToParent(data);
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
