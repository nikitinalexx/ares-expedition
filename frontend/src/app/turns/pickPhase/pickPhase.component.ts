import {Component, EventEmitter, Input, Output} from '@angular/core';
import {Game} from '../../data/Game';
import {GameRepository} from '../../model/gameRepository.model';
import {ScrollComponent} from "../../scroll/scroll.component";
import {PhaseConstants} from "../../data/PhaseConstants";
import {DetrimentToken} from "../../data/DetrimentToken";

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

  getAllowedPhases(): string[] {
    const result = [];
    for (let i = 1; i <= 5; i++) {
      if (i !== this.game?.player.previousPhase
        && (!this.game.crysisDto
          || Object.entries(this.game.crysisDto.forbiddenPhases).find(entry => entry[0] === this.game.player.playerUuid)?.[1] !== i)
        && !(this.game.crysisDto?.detrimentTokens.some(token => token === DetrimentToken.OXYGEN_RED) && i === 1)) {
        result.push(i.toString());
      }
    }
    return result;
  }

  clickPhase(id: string) {
    this.phaseInput = parseInt(id, 10);
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

  isSelectedPhase(phaseId: string): boolean {
    return this.phaseInput && parseInt(phaseId, 10) === this.phaseInput;
  }

  phaseDescription(phase: number) {
    switch (phase) {
      case 1:
        return PhaseConstants.PHASE_1_MAIN;
      case 2:
        return PhaseConstants.PHASE_2_MAIN;
      case 3:
        return PhaseConstants.PHASE_3_MAIN;
      case 4:
        return PhaseConstants.PHASE_4_MAIN;
      case 5:
        return PhaseConstants.PHASE_5_MAIN;
    }
  }

  phaseBonusDescription(phase: number) {
    const phaseBonus = this.game?.player.phaseCards[phase - 1];
    switch (phase) {
      case 1:
        return PhaseConstants.PHASE_1_BONUS[phaseBonus];
      case 2:
        return PhaseConstants.PHASE_2_BONUS[phaseBonus];
      case 3:
        return PhaseConstants.PHASE_3_BONUS[phaseBonus];
      case 4:
        return PhaseConstants.PHASE_4_BONUS[phaseBonus];
      case 5:
        return PhaseConstants.PHASE_5_BONUS[phaseBonus];
    }
  }

}
