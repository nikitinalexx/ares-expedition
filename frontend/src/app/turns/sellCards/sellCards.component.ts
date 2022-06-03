import {Component, Input, OnInit} from '@angular/core';
import {Game} from '../../data/Game';
import {GameRepository} from '../../model/gameRepository.model';
import {Card} from '../../data/Card';
import {FormGroup} from '@angular/forms';

@Component({
  selector: 'app-sell-cards',
  templateUrl: './sellCards.component.html'
})
export class SellCardsComponent implements OnInit {
  public errorMessage: string;
  cardsToCell: number[];

  @Input()
  game: Game;
  @Input()
  parentForm: FormGroup;
  @Input()
  finalTurn: boolean;
  @Input()
  mulliganTurn: boolean;

  constructor(private gameRepository: GameRepository) {

  }

  ngOnInit(): void {
    this.parentForm.valueChanges.subscribe(val => {
      this.parentForm.patchValue(val, {emitEvent: false});
    });
  }

  getPlayerHand(): Card[] {
    return this.game?.player.hand;
  }

  clickSellCard(card: Card) {
    if (!this.cardsToCell) {
      this.cardsToCell = [];
    }
    if (this.cardsToCell.find(element => element === card.id)) {
      const index = this.cardsToCell.indexOf(card.id, 0);
      if (index > -1) {
        this.cardsToCell.splice(index, 1);
      }
    } else {
      this.cardsToCell.push(card.id);
    }
  }

  selectedSellCardClass(card: Card): string {
    if (this.cardsToCell && this.cardsToCell.find(element => element === card.id)) {
      return 'clicked-card';
    }
    return '';
  }

  resetAllInputs() {
    this.cardsToCell = [];
  }

  sellCards(game: Game, callback: (value: any) => void) {
    let sellCardsFunc = this.gameRepository.sellCards;
    if (this.finalTurn) {
      sellCardsFunc = this.gameRepository.sellCardsFinalTurn;
    }
    if (this.mulliganTurn) {
      sellCardsFunc = this.gameRepository.mulliganCards;
    }
    sellCardsFunc(game.player.playerUuid, this.cardsToCell).subscribe(
      data => {
        this.cardsToCell = [];
        callback(data);
      },
      error => {
        this.errorMessage = error;
      }
    );
  }

}
