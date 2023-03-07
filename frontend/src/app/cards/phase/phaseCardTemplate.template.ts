import {Component, Input} from '@angular/core';
import {BasePlayer} from '../../data/BasePlayer';
import {PhaseConstants} from '../../data/PhaseConstants';
import {DetrimentToken} from "../../data/DetrimentToken";
import {Game} from "../../data/Game";

@Component({
  selector: 'app-phase-card-template',
  templateUrl: 'phaseCardTemplate.template.html',
  styleUrls: ['phaseCardTemplate.template.css']
})
export class PhaseCardTemplateComponent {
  @Input()
  phase: string;
  @Input()
  player: BasePlayer;
  @Input()
  selected: boolean;
  @Input()
  game: Game;

  phaseDescription(phase: string) {
    switch (phase) {
      case '1':
        return PhaseConstants.PHASE_1_MAIN;
      case '2':
        return PhaseConstants.PHASE_2_MAIN;
      case '3':
        return PhaseConstants.PHASE_3_MAIN;
      case '4':
        return PhaseConstants.PHASE_4_MAIN;
      case '5':
        return PhaseConstants.PHASE_5_MAIN;
    }
  }

  phaseBelowOrEqual3(phase: string): boolean {
    return phase === '1' || phase === '2' || phase === '3'
      || phase === '1_4' || phase === '2_4' || phase === '3_4' || phase === '3_5';
  }

  firstPhaseNoBonusByDetriment(): boolean {
    return this.game?.crysisDto?.detrimentTokens.some(token => token === DetrimentToken.OXYGEN_YELLOW);
  }

  phaseEqual3(phase: string): boolean {
    return phase === '3' || phase === '3_4' || phase === '3_5';
  }

  phaseBonusDescription(phase: string) {
    const phaseBonus = this.player.phaseCards[parseInt(phase, 10) - 1];
    switch (phase) {
      case '1':
        return PhaseConstants.PHASE_1_BONUS[phaseBonus];
      case '2':
        return PhaseConstants.PHASE_2_BONUS[phaseBonus];
      case '3':
        return PhaseConstants.PHASE_3_BONUS[phaseBonus];
      case '4':
        return PhaseConstants.PHASE_4_BONUS[phaseBonus];
      case '5':
        return PhaseConstants.PHASE_5_BONUS[phaseBonus];
    }
  }

  parseInt(input: string): number {
    return parseInt(input, 10);
  }

}
