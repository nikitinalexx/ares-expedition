import {Component, Input} from '@angular/core';
import {BasePlayer} from '../data/BasePlayer';
import {Game} from '../data/Game';
import {Tag} from '../data/Tag';

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

  getUniqueTags() {
    const tags = this.player?.played.map(card => card.tags).reduce((acc, val) => acc.concat(val), []);
    if (!tags) {
      return [];
    }

    const result = tags?.reduce((a, c) => (a.set(c, (a.get(c) || 0) + 1), a), new Map<Tag, number>());
    return Array.from(result?.entries());
  }

  currentPhase(phase: number): boolean {
    return this.game?.phase === phase;
  }

}
