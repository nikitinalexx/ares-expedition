import {Component, EventEmitter, Input, Output} from '@angular/core';
import {Game} from '../../data/Game';
import {GameRepository} from '../../model/gameRepository.model';
import {ScrollComponent} from "../../scroll/scroll.component";
import {PhaseDescription} from "../../data/PhaseDescription";

@Component({
  selector: 'app-pick-phase',
  templateUrl: './pickPhase.component.html',
  styleUrls: ['../turns.component.css']
})
export class PickPhaseComponent {
  public phaseInput: number;
  public errorMessage: string;


  @Input()
  game: Game;
  @Output() outputToParent = new EventEmitter<any>();

  constructor(private gameRepository: GameRepository,
              private scrollService: ScrollComponent) {

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
    this.scrollService.scrollToPlayerChoice();
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

  phaseDescription(phase: number) {
    switch (phase) {
      case 1:
        return PhaseDescription.PHASE_1_MAIN;
      case 2:
        return PhaseDescription.PHASE_2_MAIN;
      case 3:
        return PhaseDescription.PHASE_3_MAIN;
      case 4:
        return PhaseDescription.PHASE_4_MAIN;
      case 5:
        return PhaseDescription.PHASE_5_MAIN;
    }
  }

  phaseBonusDescription(phase: number) {
    const phaseBonus = this.game?.player.phaseCards[phase - 1];
    switch (phase) {
      case 1:
        return PhaseDescription.PHASE_1_BONUS[phaseBonus];
      case 2:
        return PhaseDescription.PHASE_2_BONUS[phaseBonus];
      case 3:
        return PhaseDescription.PHASE_3_BONUS[phaseBonus];
      case 4:
        return PhaseDescription.PHASE_4_BONUS[phaseBonus];
      case 5:
        return PhaseDescription.PHASE_5_BONUS[phaseBonus];
    }
  }

}
