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
    this.identifyNextAction();
  }

  phaseChosenByAnyone(phase: number): boolean {
    return this.game?.player.phase === phase ||
      this.game?.otherPlayers?.some(p => p.phase === phase);
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

  currentPhase(phase: number): boolean {
    return this.game?.phase === phase;
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

  milestoneMagnate(milestone: Milestone): boolean {
    return milestone.type === MilestoneType.MAGNATE;
  }

  milestoneTerraformer(milestone: Milestone): boolean {
    return milestone.type === MilestoneType.TERRAFORMER;
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
      this.game.phaseOxygen = shortGame.phaseOxygen;
      this.game.oxygen = shortGame.oxygen;
      this.game.oceans = shortGame.oceans;
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

  getUniqueTags(player: BasePlayer) {
    const tags = player?.played.map(card => card.tags).reduce((acc, val) => acc.concat(val), []);
    if (!tags) {
      return [];
    }

    const result = tags?.reduce((a, c) => (a.set(c, (a.get(c) || 0) + 1), a), new Map<Tag, number>());
    return Array.from(result?.entries());
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
        this.model.nextTurns(this.playerUuid).subscribe(turns => {
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
        return this.countDistinctTags(player);
      case MilestoneType.LEGEND:
        return this.getPlayedRedCards(player)?.length;
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
        return player?.played.map(card => card.tags).reduce((acc, val) => acc.concat(val), [])?.filter(tag => tag === Tag.SCIENCE).length;
    }

    return 0;
  }

  countPlayedTags(player: BasePlayer, tag: Tag): number {
    return player?.played.map(card => card.tags).reduce((acc, val) => acc.concat(val), [])?.filter(t => t === tag).length;
  }

  countDistinctTags(player: BasePlayer): number {
    return [...new Set(player?.played.map(card => card.tags).reduce((acc, val) => acc.concat(val), []))].length;
  }

}
