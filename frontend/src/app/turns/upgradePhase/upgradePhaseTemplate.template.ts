import {Component, EventEmitter, Input, Output} from '@angular/core';
import {Game} from '../../data/Game';
import {PhaseConstants} from '../../data/PhaseConstants';

@Component({
  selector: 'app-upgrade-phase-template',
  templateUrl: 'upgradePhaseTemplate.template.html'
})
export class UpgradePhaseTemplateComponent {
  @Input()
  phaseInput: number;
  @Input()
  phaseUpgradeTypeInput: number;
  @Input()
  game: Game;

  @Output() phaseOutput = new EventEmitter<number>();
  @Output() phaseUpgradeTypeOutput = new EventEmitter<number>();

  outputPhaseId(phase: number) {
    this.phaseOutput.emit(phase);
  }

  outputPhaseUpgradeType(phase: number) {
    this.phaseUpgradeTypeOutput.emit(phase);
  }

  phaseCounter(count: number) {
    return new Array(count);
  }

  phaseBonusUp1Description(phase: number) {
    switch (phase) {
      case 1:
        return PhaseConstants.PHASE_1_BONUS[1];
      case 2:
        return PhaseConstants.PHASE_2_BONUS[1];
      case 3:
        return PhaseConstants.PHASE_3_BONUS[1];
      case 4:
        return PhaseConstants.PHASE_4_BONUS[1];
      case 5:
        return PhaseConstants.PHASE_5_BONUS[1];
    }
  }

  phaseBonusUp2Description(phase: number) {
    switch (phase) {
      case 1:
        return PhaseConstants.PHASE_1_BONUS[2];
      case 2:
        return PhaseConstants.PHASE_2_BONUS[2];
      case 3:
        return PhaseConstants.PHASE_3_BONUS[2];
      case 4:
        return PhaseConstants.PHASE_4_BONUS[2];
      case 5:
        return PhaseConstants.PHASE_5_BONUS[2];
    }
  }

}
