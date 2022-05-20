import {Component, Input} from '@angular/core';
import {BasePlayer} from '../data/BasePlayer';
import {Game} from '../data/Game';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./../game/game.component.css']
})
export class NavbarComponent {

  @Input()
  game: Game;
  @Input()
  player: BasePlayer;

  constructor() {

  }

  chosenPhases(): number[] {
    const phases = [];
    for (let i = 1; i <= 5; i++) {
      if (this.phaseChosenByAnyone(i)) {
        phases.push(i);
      }
    }
    return phases;
  }

  phaseChosenByAnyone(phase: number): boolean {
    return this.game?.player.phase === phase ||
      this.game?.otherPlayers?.some(p => p.phase === phase);
  }

  phaseDisplayStyles(phase: number): string {
    let result = '';
    if (!this.currentPhase(phase)) {
      result += 'phase-transparent';
    }

    if (this.player?.phase === phase) {
      result += ' selectedPhase';
    }

    return result;
  }

  currentPhase(phase: number): boolean {
    return this.game?.phase === phase;
  }

}
