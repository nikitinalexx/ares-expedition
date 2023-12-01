import {Component, Input, OnInit} from '@angular/core';
import {Game} from '../../data/Game';
import {GameRepository} from '../../model/gameRepository.model';
import {Card} from '../../data/Card';
import {FormGroup} from '@angular/forms';
import {ScrollComponent} from '../../scroll/scroll.component';

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
  @Input()
  lastTurn: boolean;

  constructor(private gameRepository: GameRepository,
              private scrollService: ScrollComponent) {

  }
  confirmGameEndTurn(): boolean {
    return this.lastTurn;
  }

  ngOnInit(): void {
    this.parentForm.valueChanges.subscribe(val => {
      this.parentForm.patchValue(val, {emitEvent: false});
    });
  }

  getPlayerHand(): Card[] {
    return this.game?.player.hand;
  }



  selectAllCards(event: Event) {
    const click = event.target as HTMLInputElement;

    const checked = click.checked;

    const playerHand = this.getPlayerHand();

    if (playerHand && checked) {
      playerHand.forEach(card => this.cardsToCell.push(card.id)); // Выбрать все карты, если флажок установлен
    }else {
      playerHand.forEach(card => {
        const index = this.cardsToCell.indexOf(card.id, 0);
        if (index > -1) {
          this.cardsToCell.splice(index, 1);
        }
      });

    }
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
    this.scrollService.scrollToPlayerChoice();
  }

  sellCards(game: Game, callback: (value: any) => void) {
    let sellCardsFunc = this.gameRepository.sellCards;
    if (this.finalTurn) {
      sellCardsFunc = this.gameRepository.sellCardsFinalTurn;
    }
    if (this.mulliganTurn) {
      sellCardsFunc = this.gameRepository.mulliganCards;
      if (!this.cardsToCell) {
        this.cardsToCell = [];
      }
    }
    sellCardsFunc(game.player.playerUuid, this.cardsToCell).subscribe(
      data => {
        callback(data);
        this.cardsToCell = [];
        this.scrollService.scrollToPlayerChoice();
      },
      error => {
        this.errorMessage = error;
      }
    );
  }

}
