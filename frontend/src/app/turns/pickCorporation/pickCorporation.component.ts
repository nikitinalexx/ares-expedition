import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Game} from '../../data/Game';
import {Card} from '../../data/Card';
import {GameRepository} from '../../model/gameRepository.model';
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {TurnType} from "../../data/TurnType";
import {DiscardCardsTurn} from "../../data/DiscardCardsTurn";

@Component({
  selector: 'app-pick-corporation',
  templateUrl: './pickCorporation.component.html'
})
export class PickCorporationComponent implements OnInit {
  public corporationInput: number;
  public errorMessage: string;
  projectsToDiscard: number[];

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
      turn: ['pickCorporation', Validators.required]
    });

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
    if (!this.game.player.corporationId) {
      this.corporationInput = card.id;
      this.errorMessage = null;
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

  getDiscardCards(): Card[] {
    const nextTurn = this.game.player.nextTurn as DiscardCardsTurn;
    if (nextTurn.onlyFromSelectedCards) {
      return this.game.player.nextTurn.cards;
    } else {
      return this.game.player?.hand;
    }
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
          this.gameRepository.pickCorporation(this.game.player.playerUuid, this.corporationInput)
            .subscribe(data => this.sendToParent(data));
        }
      } else if (formGroup.value.turn === 'discardCards') {
        if (!this.projectsToDiscard || this.projectsToDiscard.length !== this.game.player.nextTurn.size) {
          this.errorMessage = 'Invalid number of cards to discard';
        } else {
          this.gameRepository.discardCards(this.game.player.playerUuid, this.projectsToDiscard).subscribe(
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

  sendToParent(data: any) {
    this.outputToParent.emit(data);
  }

  isSelectedCard(card: Card): string {
    if (this.corporationInput && card.id === this.corporationInput) {
      return 'clicked-card';
    }
    return '';
  }

}
