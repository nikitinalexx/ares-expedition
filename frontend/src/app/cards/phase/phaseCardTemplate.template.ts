import {Component, Input} from '@angular/core';
import {BasePlayer} from '../../data/BasePlayer';
import {PhaseDescription} from '../../data/PhaseDescription';

@Component({
  selector: 'app-phase-card-template',
  templateUrl: 'phaseCardTemplate.template.html',
  styleUrls: ['phaseCardTemplate.template.css']
})
export class PhaseCardTemplateComponent {
  @Input()
  phase: number;
  @Input()
  player: BasePlayer;
  @Input()
  selected: boolean;

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
    const phaseBonus = this.player.phaseCards[phase - 1];
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
