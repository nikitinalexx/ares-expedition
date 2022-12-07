import {Injectable} from '@angular/core';
import {Card} from '../data/Card';
import {SpecialEffect} from '../data/SpecialEffect';
import {Player} from '../data/Player';
import {Tag} from '../data/Tag';
import {DiscountComponent} from '../discount/discount.component';
import {ParameterColor} from '../data/ParameterColor';
import {Game} from '../data/Game';
import {CardAction} from '../data/CardAction';

@Injectable()
export class RequirementsComponent {

  constructor(private discountService: DiscountComponent) {

  }

  sortCardsForBuilding(cards: Card[], player: Player, game: Game) {
    cards.sort((a, b) => {
      const canBuildA = this.enoughRequirements(a, player, game);
      const canBuildB = this.enoughRequirements(b, player, game);
      if (canBuildA && !canBuildB) {
        return -1;
      } else if (!canBuildA && canBuildB) {
        return 1;
      }
      const enoughRequirements = canBuildA || canBuildB;

      const enoughMoneyA = this.enoughMoney(a, player);
      const enoughMoneyB = this.enoughMoney(b, player);
      if (!enoughMoneyA && enoughMoneyB) {
        if (enoughRequirements) {
          return 1;
        } else {
          return -1;
        }
      } else if (enoughMoneyA && !enoughMoneyB) {
        if (enoughRequirements) {
          return -1;
        } else {
          return 1;
        }
      }
      return 0;
    });
  }

  enoughMoney(card: Card, player: Player): boolean {
    const discount = this.discountService.getDiscount(card, player);

    return player.mc >= card.price - discount;
  }

  enoughRequirements(card: Card, player: Player, game: Game): boolean {
    if (card.tagReq) {
      const allTags = player.played.map(c => c.tags).reduce((acc, val) => acc.concat(val), []);

      for (const tag of card.tagReq) {
        const index = allTags.indexOf(tag);
        if (index > -1) {
          allTags.splice(index, 1);
        } else {
          return false;
        }
      }
    }

    const playerMayAmplifyGlobalRequirement = this.ownsSpecialEffect(player, SpecialEffect.AMPLIFY_GLOBAL_REQUIREMENT)
      || player.builtSpecialDesignLastTurn;

    if (card.oxygenReq && card.oxygenReq.length > 0) {
      const requirements = playerMayAmplifyGlobalRequirement ? this.amplifyRequirement(card.oxygenReq) : card.oxygenReq;
      if (!requirements.find(req => req === game.phaseOxygenColor)) {
        return false;
      }
    }

    if (card.tempReq && card.tempReq.length > 0) {
      const requirements = playerMayAmplifyGlobalRequirement ? this.amplifyRequirement(card.tempReq) : card.tempReq;
      if (!requirements.find(req => req === game.phaseTemperatureColor)) {
        return false;
      }
    }

    if (card.oceanRequirement) {
      if (game.phaseOceans < card.oceanRequirement.minValue) {
        return false;
      }
      if (game.phaseOceans > card.oceanRequirement.maxValue) {
        return false;
      }
    }

    if (card.cardAction && card.cardAction === CardAction.ENERGY_STORAGE && player.terraformingRating < 7) {
      return false;
    }

    return true;
  }

  canBuildCard(card: Card, player: Player, game: Game): boolean {
    if (!this.enoughMoney(card, player)) {
      return false;
    }

    return this.enoughRequirements(card, player, game);
  }

  getDiscount(card: Card, player: Player): number {
    return 0;
  }

  private ownsSpecialEffect(player: Player, targetEffect: SpecialEffect): boolean {
    return player.played?.some(card => card?.specialEffects.some(effect => effect === targetEffect));
  }

  private cardHasTag(card: Card, targetTag: Tag): boolean {
    return card.tags?.some(tag => tag === targetTag);
  }

  amplifyRequirement(initialRequirement: ParameterColor[]): ParameterColor[] {
    const resultRequirement = Object.assign([], initialRequirement);

    const minRequirement = initialRequirement.map(value => value.valueOf()).reduce((a, b) => (ParameterColor[a] < ParameterColor[b]) ? a : b);
    const maxRequirement = initialRequirement.map(value => value.valueOf()).reduce((a, b) => (ParameterColor[a] > ParameterColor[b]) ? a : b);

    if (ParameterColor[minRequirement] > ParameterColor.P.toString()) {
      resultRequirement.push(ParameterColor[Number.parseInt(ParameterColor[minRequirement], 0) - 1]);
    }
    if (ParameterColor[maxRequirement] < ParameterColor.W.toString()) {
      resultRequirement.push(ParameterColor[Number.parseInt(ParameterColor[maxRequirement], 0) + 1]);
    }

    console.log(resultRequirement);
    return resultRequirement;
  }

  subtract(num: number): number {
    return num - 1;
  }

}

