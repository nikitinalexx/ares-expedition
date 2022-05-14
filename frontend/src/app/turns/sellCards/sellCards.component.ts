import {Component, Input} from '@angular/core';
import {Game} from '../../data/Game';
import {GameRepository} from '../../model/gameRepository.model';
import {Card} from '../../data/Card';
import {FormGroup} from '@angular/forms';

@Component({
  selector: 'app-sell-cards',
  templateUrl: './sellCards.component.html'
})
export class SellCardsComponent {
  cardsToCell: number[];

  @Input()
  game: Game;
  @Input()
  parentForm: FormGroup;

  constructor(private gameRepository: GameRepository) {

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

  onValueClick() {
    this.cardsToCell = [];
  }

  sellCards(game: Game) {
    this.gameRepository.sellCards(game.player.playerUuid, this.cardsToCell).subscribe(
      data => {
        this.cardsToCell = [];
      }
    );
  }

}
