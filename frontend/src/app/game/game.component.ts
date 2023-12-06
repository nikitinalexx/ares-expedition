import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {GameRepository} from '../model/gameRepository.model';
import {Game} from '../data/Game';
import {Card} from '../data/Card';
import {TurnType} from '../data/TurnType';
import {Subscription, timer} from 'rxjs';
import {FormBuilder, FormGroup} from '@angular/forms';
import {BasePlayer} from '../data/BasePlayer';
import {CardColor} from '../data/CardColor';
import {Tag} from '../data/Tag';
import {Player} from "../data/Player";
import {DiscardCardsTurn} from "../data/DiscardCardsTurn";
import {Award} from "../data/Award";
import {AwardType} from "../data/AwardType";
import {Milestone} from "../data/Milestone";
import {MilestoneType} from "../data/MilestoneType";
import {DetrimentToken} from "../data/DetrimentToken";


@Component({
  selector: 'app-game',
  templateUrl: './game.component.html',
  styleUrls: ['./game.component.css']
})
export class GameComponent implements OnInit {
  public errorMessage: string;
  private playerUuid: string;
  private playersToNextActions: Map<string, string>;
  public game: Game;
  public nextTurns: TurnType[];
  private subscription: Subscription;
  private thirdPhaseSubscription: Subscription;
  private audio: HTMLAudioElement;
  private alertAlwaysOn: boolean;
  parentForm: FormGroup;

  constructor(private route: ActivatedRoute,
              private model: GameRepository,
              private formBuilder: FormBuilder) {
    this.audio = new Audio('../../assets/sound/notification.mp3');
    this.route.params.subscribe(playerUuid => {
      this.playerUuid = playerUuid.playerUuid;
      model.getGame(this.playerUuid).subscribe(data => {
        this.game = data;
        this.identifyNextAction();
      });
    });
  }

  ngOnInit(): void {
    this.parentForm = this.formBuilder.group({
      player: 'You'
    });
  }

  yourTurnInfo(): boolean {
    if (!this.playersToNextActions) {
      return true;
    }
    const playerUuid = this.game.player.playerUuid;
    return this.playersToNextActions[playerUuid] !== 'TURN';
  }

  otherPlayerTurnInfo(anotherPlayer: BasePlayer): boolean {
    if (!this.playersToNextActions) {
      return true;
    }
    const currentPlayer = this.game.player.playerUuid;
    if (this.playersToNextActions[currentPlayer] === 'TURN') {
      return false;
    }
    if (this.playersToNextActions[anotherPlayer.playerUuid] === 'TURN') {
      return false;
    } else {
      return true;
    }
  }

  getCorporationCards(): Card[] {
    return this.game?.player.corporations;
  }

  getPlayerHand(player: Player): Card[] {
    if (!player) {
      return [];
    }
    const nextTurn = player.nextTurn as DiscardCardsTurn;
    if (nextTurn && nextTurn.cards) {
      return player.hand.filter(card => !nextTurn.cards.some(c => c.id === card.id));
    } else {
      return player.hand;
    }
  }

  getPlayerPlayedCards(player: BasePlayer): Card[] {
    return player?.played;
  }

  getPlayedBlueCardsWithCorporation(player: BasePlayer): Card[] {
    return player?.played.filter(card => card.cardColor === CardColor.BLUE || card.cardColor === CardColor.CORPORATION);
  }

  getPlayedBlueCards(player: BasePlayer): Card[] {
    return player?.played.filter(card => card.cardColor === CardColor.BLUE);
  }

  getPlayedRedCards(player: BasePlayer): Card[] {
    return player?.played.filter(card => card.cardColor === CardColor.RED);
  }

  getPlayedGreenCards(player: BasePlayer): Card[] {
    return player?.played.filter(card => card.cardColor === CardColor.GREEN);
  }

  pickCorporationTurn(): boolean {
    return this.game && this.game.phase === 7;
  }

  pickPhaseTurn(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.PICK_PHASE])?.length > 0;
  }

  crisisTurn(): boolean {
    return this.nextTurns && this.nextTurns.some(turn => turn === TurnType.RESOLVE_IMMEDIATE_WITH_CHOICE
      || turn === TurnType.RESOLVE_IMMEDIATE_ALL
      || turn === TurnType.RESOLVE_PERSISTENT_WITH_CHOICE
      || turn === TurnType.RESOLVE_PERSISTENT_ALL
      || turn === TurnType.CRISIS_CHOOSE_DUMMY_HAND
      || turn === TurnType.CRISIS_VP_TO_TOKEN
      || turn === TurnType.RESOLVE_OCEAN_DETRIMENT
      || turn === TurnType.DISCARD_CARDS && this.game.phase === -1);
  }

  dummyCards(): number[] {
    if (this.game?.crysisDto) {
      return this.game?.crysisDto.chosenDummyPhases;
    } else {
      return this.game?.usedDummyHand;
    }
  }

  firstPhaseTurn(): boolean {
    return this.game && this.game.phase === 1 && this.playersToNextActions[this.game.player.playerUuid] === 'TURN';
  }

  secondPhaseTurn(): boolean {
    return this.game && this.game.phase === 2 && this.playersToNextActions[this.game.player.playerUuid] === 'TURN';
  }

  thirdPhaseTurn(): boolean {
    return this.game && this.game.phase === 3 && this.playersToNextActions[this.game.player.playerUuid] === 'TURN';
  }

  fourthPhaseTurn(): boolean {
    return this.game && this.game.phase === 4 && this.playersToNextActions[this.game.player.playerUuid] === 'TURN';
  }

  fifthPhaseTurn(): boolean {
    return this.game && this.game.phase === 5 && this.playersToNextActions[this.game.player.playerUuid] === 'TURN';
  }

  sixthPhaseTurn(): boolean {
    return this.game && this.game.phase === 6 && this.playersToNextActions[this.game.player.playerUuid] === 'TURN';
  }

  gameEnd(): boolean {
    return this.nextTurns && this.nextTurns.find(turn => turn === TurnType[TurnType.GAME_END])?.length > 0;
  }

  GetOutputVal(newX) {
    this.checkNextTurns(true);
  }

  phaseChosenByAnyone(phase: number): boolean {
    return this.game?.player.phase === phase ||
      this.game?.otherPlayers?.some(p => p.phase === phase);
  }

  chosenPhases(): number[] {
    const phases = [];
    for (let i = 1; i <= 5; i++) {
      if (this.phaseChosenByAnyone(i) && (!this.game.crysisDto || this.game.phase > 0)
        || this.game?.dummyHandMode && this.game?.usedDummyHand && this.game.usedDummyHand.length > 0
        && this.game.usedDummyHand[this.game.usedDummyHand.length - 1] === i
        || this.game?.crysisDto
        && this.game.crysisDto.chosenDummyPhases
        && this.game.crysisDto.chosenDummyPhases.find(phase => phase === i)) {
        phases.push(i);
      }
    }
    return phases;
  }

  currentPhase(phase: number): boolean {
    return this.game?.phase === phase;
  }

  sumTerraformingRatingForCrisis(): number {
    let sum = this.game.player.terraformingRating;

    for (const otherPlayer of this.game?.otherPlayers) {
      sum += otherPlayer.terraformingRating;
    }
    return sum;
  }

  gameFinishedClass(): string {
    if (this.game.crysisDto) {
      if (this.game.crysisDto.wonGame) {
        return 'bg-success text-white';
      } else {
        return 'bg-danger text-white';
      }
    } else {
      if (!this.game.otherPlayers || this.game.otherPlayers.length === 0) {
        return 'text-success';
      } else {
        const playerScore = this.game.player.winPoints;
        for (const otherPlayer of this.game.otherPlayers) {
          if (otherPlayer.winPoints > playerScore) {
            return 'bg-danger text-white';
          }
        }
        return 'bg-success text-white';
      }
    }
  }

  getOxygenDetrimentToken(): number {
    if (!this.game?.crysisDto || !this.game.crysisDto.detrimentTokens) {
      return undefined;
    }
    const detrimentTokens = this.game.crysisDto.detrimentTokens;

    if (detrimentTokens.find(token => token === DetrimentToken.OXYGEN_YELLOW)) {
      return 0;
    } else if (detrimentTokens.find(token => token === DetrimentToken.OXYGEN_RED)) {
      return 1;
    }
    return 2;
  }

  oxygenTitleClass(): string {
    if (this.getOxygenDetrimentToken() === 2) {
      return 'text-danger';
    } else {
      return '';
    }
  }

  temperatureTitleClass(): string {
    if (this.getTemperatureDetrimentToken() === 2) {
      return 'text-danger';
    } else {
      return '';
    }
  }

  oceanTitleClass(): string {
    if (this.getOceanDetrimentToken() === 2) {
      return 'text-danger';
    } else {
      return '';
    }
  }

  getTemperatureDetrimentToken(): number {
    if (!this.game?.crysisDto || !this.game.crysisDto.detrimentTokens) {
      return undefined;
    }
    const detrimentTokens = this.game.crysisDto.detrimentTokens;

    if (detrimentTokens.find(token => token === DetrimentToken.TEMPERATURE_YELLOW)) {
      return 0;
    } else if (detrimentTokens.find(token => token === DetrimentToken.TEMPERATURE_RED)) {
      return 1;
    }
    return 2;
  }

  getOceanDetrimentToken(): number {
    if (!this.game?.crysisDto || !this.game.crysisDto.detrimentTokens) {
      return undefined;
    }
    const detrimentTokens = this.game.crysisDto.detrimentTokens;

    if (detrimentTokens.find(token => token === DetrimentToken.OCEAN_YELLOW)) {
      return 0;
    } else if (detrimentTokens.find(token => token === DetrimentToken.OCEAN_RED)) {
      return 1;
    }
    return 2;
  }

  displayRedCubeOnMilestone(milestoneIndex: number): boolean {
    if (!this.game) {
      return false;
    }
    if (this.game.player.austellarMilestone === milestoneIndex) {
      return true;
    }
    if (this.game.otherPlayers.find(p => p.austellarMilestone === milestoneIndex)) {
      return true;
    }
    return false;
  }

  phaseDisplayStyles(phase: number): string[] {
    const result = [];
    if (!this.currentPhase(phase)) {
      result.push('phase-transparent');
    }

    if (this.game?.player?.phase === phase) {
      result.push('selectedPhase');
    }

    return result;
  }

  playerPhaseInfoDisplayStyles(phase: number, player: BasePlayer): string[] {
    const result = [];
    if (player?.phase !== phase) {
      result.push('greyed-phase');
    }
    if (player?.phaseCards[phase - 1] !== 0) {
      result.push('upgraded-phase');
    }
    if (this.game?.crysisDto &&
      Object.entries(this.game.crysisDto.forbiddenPhases).find(entry => entry[0] === this.game?.player.playerUuid)?.[1] === phase) {
      result.push('forbidden-phase');
    }
    return result;
  }

  milestoneMagnate(milestone: Milestone): boolean {
    return milestone.type === MilestoneType.MAGNATE;
  }

  milestoneTerraformer(milestone: Milestone): boolean {
    return milestone.type === MilestoneType.TERRAFORMER;
  }

  milestoneGardener(milestone: Milestone): boolean {
    return milestone.type === MilestoneType.GARDENER;
  }

  milestoneBuilder(milestone: Milestone): boolean {
    return milestone.type === MilestoneType.BUILDER;
  }

  milestoneSpaceBaron(milestone: Milestone): boolean {
    return milestone.type === MilestoneType.SPACE_BARON;
  }

  milestoneEnergizer(milestone: Milestone): boolean {
    return milestone.type === MilestoneType.ENERGIZER;
  }

  milestoneFarmer(milestone: Milestone): boolean {
    return milestone.type === MilestoneType.FARMER;
  }

  milestoneTycoon(milestone: Milestone): boolean {
    return milestone.type === MilestoneType.TYCOON;
  }

  milestonePlanner(milestone: Milestone): boolean {
    return milestone.type === MilestoneType.PLANNER;
  }

  milestoneDiversifier(milestone: Milestone): boolean {
    return milestone.type === MilestoneType.DIVERSIFIER;
  }

  milestoneLegend(milestone: Milestone): boolean {
    return milestone.type === MilestoneType.LEGEND;
  }

  awardTypeIndustrialist(award: Award): boolean {
    return award.type === AwardType.INDUSTRIALIST;
  }

  awardTypeProjectManager(award: Award): boolean {
    return award.type === AwardType.PROJECT_MANAGER;
  }

  awardTypeGenerator(award: Award): boolean {
    return award.type === AwardType.GENERATOR;
  }

  awardTypeCelebrity(award: Award): boolean {
    return award.type === AwardType.CELEBRITY;
  }

  awardTypeCollector(award: Award): boolean {
    return award.type === AwardType.COLLECTOR;
  }

  awardTypeResearcher(award: Award): boolean {
    return award.type === AwardType.RESEARCHER;
  }

  updateGameShort() {
    this.model.getShortGame(this.playerUuid).subscribe(shortGame => {
      this.game.temperature = shortGame.temperature;
      this.game.phaseTemperature = shortGame.phaseTemperature;
      this.game.phaseInfrastructure = shortGame.phaseInfrastructure;
      this.game.phaseOxygen = shortGame.phaseOxygen;
      this.game.oxygen = shortGame.oxygen;
      this.game.oceans = shortGame.oceans;
      this.game.infrastructure = shortGame.infrastructure;
      this.game.phaseOceans = shortGame.phaseOceans;
      this.game.otherPlayers = shortGame.otherPlayers;
    });
  }

  getPlayerToShow(): BasePlayer {
    if (this.parentForm.value?.player === 'You') {
      return this.game?.player;
    } else {
      const anotherPlayerId = this.parentForm.value.player as number;
      return this.game?.otherPlayers[anotherPlayerId];
    }
  }

  generateNonZeroArray(size: number): number[] {
    const result = [];
    for (let i = 1; i <= size; i++) {
      result.push(i);
    }
    return result;
  }

  oceanTrackerStyle(min: number, max: number): string {
    const oceanCount = this.game?.oceans.filter(ocean => ocean.revealed)?.length;
    if (oceanCount >= min && oceanCount <= max) {
      return '';
    } else {
      return 'ocean-counter-grey';
    }
  }

  countRevealedOceans(): number {
    return this.game?.oceans.filter(ocean => ocean.revealed)?.length;
  }

  anyParameterIsVeryLowInCrisis(): boolean {
    if (!this.game || !this.game.crysisDto || this.gameEnd()) {
      return;
    }
    const oceanCount = this.game?.oceans.filter(ocean => ocean.revealed)?.length;
    if (oceanCount <= 1) {
      return true;
    }
    if (this.game.temperature <= -20) {
      return true;
    }
    if (this.game.oxygen <= 2) {
      return true;
    }
    return false;
  }

  parameterVeryLowInCrisisMessage(): string {
    if (!this.game || !this.game.crysisDto) {
      return '';
    }
    let message = '';
    const oceanCount = this.game?.oceans.filter(ocean => ocean.revealed)?.length;
    if (oceanCount <= 1) {
      message += 'Ocean';
    }
    if (this.game.temperature <= -20) {
      if (message.length !== 0) {
        message += ', ';
      }
      message += 'Temperature';
    }
    if (this.game.oxygen <= 2) {
      if (message.length !== 0) {
        message += ', ';
      }
      message += 'Oxygen';
    }
    message += ' level is dangerously low. Terraform before the round ends';
    return message;
  }

  checkNextTurns(callNextTurnOnFail: boolean) {
    this.model.nextTurns(this.playerUuid).subscribe(turns => {
      if (callNextTurnOnFail && !turns?.length) {
        this.identifyNextAction();
        return;
      }
      const previousAction = this.playersToNextActions
        ? this.playersToNextActions[this.game.player.playerUuid]
        : null;

      if (previousAction && previousAction === 'WAIT' && (document.hidden || this.isAlertAlwaysOn())) {
        this.audio.play();
      }

      if (this.subscription) {
        this.subscription.unsubscribe();
      }

      const globalParamReachedMax = !this.nextTurns
        && previousAction === 'WAIT'
        && this.game.phase === 3
        && turns.find(t => t === TurnType.PERFORM_BLUE_ACTION);

      if (globalParamReachedMax) {
        this.errorMessage = 'One of the global parameters reached maximum';
      }
      this.model.getGame(this.playerUuid).subscribe(game => {
        this.nextTurns = turns;
        this.game = game;
        if (!globalParamReachedMax) {
          this.errorMessage = null;
        }
        if (this.game.phase === 3 && (!this.thirdPhaseSubscription || this.thirdPhaseSubscription.closed)) {
          this.thirdPhaseSubscription = timer(2000, 2000).subscribe(
            val => this.updateGameShort()
          );
        } else if (this.game.phase !== 3 && this.thirdPhaseSubscription) {
          this.thirdPhaseSubscription.unsubscribe();
        }
      });
    });
  }

  identifyNextAction() {
    this.model.nextActions(this.playerUuid).subscribe(data => {
      const previousAction = this.playersToNextActions
        ? this.playersToNextActions[this.game.player.playerUuid]
        : null;
      this.playersToNextActions = data.playersToNextActions;
      if (this.playersToNextActions[this.game.player.playerUuid] === 'TURN') {
        if (previousAction && previousAction === 'WAIT' && (document.hidden || this.isAlertAlwaysOn())) {
          this.audio.play();
        }
        this.checkNextTurns(false);
        if (this.subscription) {
          this.subscription.unsubscribe();
        }
      } else if (this.playersToNextActions[this.game.player.playerUuid] === 'WAIT') {
        this.errorMessage = 'Waiting for the other player';
        this.nextTurns = null;
        if (!this.subscription || this.subscription.closed) {
          this.subscription = timer(1000, 2000).subscribe(val => this.identifyNextAction());
        }
      }
    });
  }

  milestoneClass(milestone: Milestone): string {
    if (Array.from(milestone.players.values()).length === 0) {
      return '';
    } else {
      return 'achieved-milestone';
    }
  }

  alertCheckEvent($event: any) {
    localStorage.setItem('alertAlwaysOn', $event.target.checked ? 'yes' : 'no');
  }

  isAlertAlwaysOn(): boolean {
    return localStorage.getItem('alertAlwaysOn') === 'yes';
  }

  otherPlayerBackgroundClass(index: number): string {
    if (index === 0) {
      return 'bg-warning';
    } else if (index === 1) {
      return 'bg-info';
    } else {
      return 'bg-primary';
    }
  }

  milestoneValue(playerIndex: number, milestone: Milestone): number {
    const player = (playerIndex === 0 ? this.game.player : this.game.otherPlayers[playerIndex - 1]);

    if (Array.from(milestone.players.values()).length !== 0) {
      return milestone.playerToValue[player.playerUuid];
    }

    switch (milestone.type) {
      case MilestoneType.MAGNATE:
        return this.getPlayedGreenCards(player)?.length;
      case MilestoneType.TERRAFORMER:
        return player.terraformingRating;
      case MilestoneType.BUILDER:
        return this.countPlayedTags(player, Tag.BUILDING);
      case MilestoneType.SPACE_BARON:
        return this.countPlayedTags(player, Tag.SPACE);
      case MilestoneType.ENERGIZER:
        return player.heatIncome;
      case MilestoneType.FARMER:
        return player.plantsIncome;
      case MilestoneType.TYCOON:
        return this.getPlayedBlueCards(player)?.length;
      case MilestoneType.PLANNER:
        return player.played.length === 0 ? 0 : player.played.length - 1;
      case MilestoneType.DIVERSIFIER:
        return this.getUniqueTags(player).length;
      case MilestoneType.LEGEND:
        return this.getPlayedRedCards(player)?.length;
      case MilestoneType.GARDENER:
        return player.forests > 3 ? 3 : player.forests;
    }

    return 0;
  }

  awardValue(playerIndex: number, award: Award): number {
    const player = (playerIndex === 0 ? this.game.player : this.game.otherPlayers[playerIndex - 1]);

    switch (award.type) {
      case AwardType.INDUSTRIALIST:
        return player.steelIncome + player.titaniumIncome;
      case AwardType.PROJECT_MANAGER:
        return player.played.length === 0 ? 0 : player.played.length - 1;
      case AwardType.GENERATOR:
        return player.heatIncome;
      case AwardType.CELEBRITY:
        return player.mcIncome;
      case AwardType.COLLECTOR:
        return Object.values(player.cardResources).reduce((acc, val) => acc + val, 0);
      case AwardType.RESEARCHER:
        return this.countPlayedTags(player, Tag.SCIENCE);
    }

    return 0;
  }

  getUniqueTags(player: BasePlayer) {
    const tags = this.getPlayedTags(player);

    if (!tags) {
      return [];
    }

    const result = tags?.reduce((a, c) => (a.set(c, (a.get(c) || 0) + 1), a), new Map<Tag, number>());
    return Array.from(result?.entries()).sort((a, b) => b[1] - a[1]);
  }

  countPlayedTags(player: BasePlayer, tag: Tag): number {
    return this.getPlayedTags(player)
      ?.filter(t => t === tag).length;
  }

  getPlayedTags(player: BasePlayer): Tag[] {
    return player?.played
      .map(card => {
        const tagsWithDynamic = [];
        for (let i = 0; i < card.tags.length; i++) {
          if (card.tags[i] !== Tag.DYNAMIC) {
            tagsWithDynamic.push(card.tags[i]);
          } else if (player.cardToTag[card.id][i] !== Tag.DYNAMIC) {
            tagsWithDynamic.push(player.cardToTag[card.id][i]);
          }
        }
        return tagsWithDynamic;
      })
      .reduce((acc, val) => acc.concat(val), []);
  }

  milestoneSuccessClass(milestone: Milestone): string {
    if (Array.from(milestone.players.values()).length !== 0) {
      const playerValue = milestone.playerToValue[this.game.player.playerUuid];
      for (const otherPlayer of this.game.otherPlayers) {
        if (milestone.playerToValue[otherPlayer.playerUuid] > playerValue) {
          return '';
        }
      }
      if (milestone.playerToValue[this.game.player.playerUuid] !== undefined) {
        return 'achievement-success border border-success border-5';
      } else {
        return '';
      }
    }
    const maxValue = this.milestoneValue(0, milestone);
    for (let i = 0; i < this.game.otherPlayers.length; i++) {
      if (this.milestoneValue(i + 1, milestone) > maxValue) {
        return '';
      }
    }
    return 'achievement-success border border-success border-5';
  }

  awardSuccessClass(award: Award): string {
    const maxValue = this.awardValue(0, award);
    for (let i = 0; i < this.game.otherPlayers.length; i++) {
      if (this.awardValue(i + 1, award) > maxValue) {
        return '';
      }
    }
    return 'achievement-success border border-success border-5';
  }
}
