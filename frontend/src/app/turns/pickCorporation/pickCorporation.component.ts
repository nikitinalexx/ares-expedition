import {Component, EventEmitter, Input, Output} from '@angular/core';
import {Game} from '../../data/Game';
import {Card} from '../../data/Card';
import {GameRepository} from '../../model/gameRepository.model';

@Component({
  selector: 'app-pick-corporation',
  templateUrl: './pickCorporation.component.html'
})
export class PickCorporationComponent {
  public corporationInput: number;
  public errorMessage: string;


  @Input()
  game: Game;
  @Output() outputToParent = new EventEmitter<any>();

  constructor(private gameRepository: GameRepository) {

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

  chooseCorporation(corporation: number) {
    if (!this.corporationInput) {
      this.errorMessage = 'Pick corporation';
    } else {
      this.gameRepository.pickCorporation(this.game.player.playerUuid, this.corporationInput)
        .subscribe(data => this.sendToParent(data));
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
