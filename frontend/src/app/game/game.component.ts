import {Component} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {GameRepository} from '../model/gameRepository.model';
import {Game} from '../data/Game';


@Component({
  selector: 'app-game',
  templateUrl: './game.component.html'
})
export class GameComponent {
  private playerUuid: string;
  private game: Game;

  constructor(private route: ActivatedRoute, private model: GameRepository) {
    this.route.params.subscribe(playerUuid => this.playerUuid = playerUuid.playerUuid);
    console.log(this.playerUuid);
    model.getGame(this.playerUuid).subscribe(data => {
      this.game = data;
      console.log(this.game);
    });
  }

}
