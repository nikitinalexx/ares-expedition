import {Component, EventEmitter, Input, Output} from '@angular/core';
import {Game} from '../../data/Game';
import {GameRepository} from '../../model/gameRepository.model';

@Component({
  selector: 'app-pick-phase',
  templateUrl: './pickPhase.component.html'
})
export class PickPhaseComponent {
  public phaseInput: number;
  public errorMessage: string;


  @Input()
  game: Game;
  @Output() outputToParent = new EventEmitter<any>();

  constructor(private gameRepository: GameRepository) {

  }

  getAllowedPhases(): number[] {
    const result = [];
    for (let i = 1; i <= 5; i++) {
      if (i !== this.game?.player.previousPhase) {
        result.push(i);
      }
    }
    return result;
  }

  clickPhase(id: number) {
    this.phaseInput = id;
  }

  choosePhase(corporation: number) {
    if (!this.phaseInput) {
      this.errorMessage = 'Pick phase';
    } else {
      this.gameRepository.pickPhase(this.game.player.playerUuid, this.phaseInput)
        .subscribe(data => this.sendToParent(data));
    }
  }

  sendToParent(data: any) {
    this.outputToParent.emit(data);
  }

  isSelectedPhase(phaseId: number): string {
    if (this.phaseInput && phaseId === this.phaseInput) {
      return 'clicked-card';
    }
    return '';
  }

}
