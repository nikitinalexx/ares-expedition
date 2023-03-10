import {Injectable} from '@angular/core';
import {Card} from '../data/Card';
import {SpecialEffect} from '../data/SpecialEffect';
import {Player} from '../data/Player';
import {Tag} from '../data/Tag';
import {CardColor} from '../data/CardColor';
import {BuildType} from '../data/BuildType';
import {Build} from '../data/Build';
import {Game} from '../data/Game';
import {DetrimentToken} from '../data/DetrimentToken';

@Injectable()
export class DiscountComponent {
  allTags = [Tag.SPACE, Tag.EARTH, Tag.EVENT, Tag.SCIENCE, Tag.PLANT,
    Tag.ENERGY, Tag.BUILDING, Tag.ANIMAL, Tag.JUPITER, Tag.MICROBE];

  isDiscountApplicable(game: Game, card: Card, player: Player, tagInput: number): boolean {
    if (!player) {
      return false;
    }
    return this.getDiscountWithOptimal(game, card, player, tagInput) !== 0;
  }

  getDiscount(game: Game, card: Card, player: Player, tagInput: number): number {
    let discount = 0;

    const ownsAdvancedAlloys = this.ownsSpecialEffect(player, SpecialEffect.ADVANCED_ALLOYS);
    const ownsPhobolog = this.ownsSpecialEffect(player, SpecialEffect.PHOBOLOG);

    if (this.cardHasTag(card, Tag.BUILDING, tagInput)) {
      discount += player.steelIncome * (2 + (ownsAdvancedAlloys ? 1 : 0));
    }

    if (this.cardHasTag(card, Tag.SPACE, tagInput)) {
      discount += player.titaniumIncome * (3 + (ownsAdvancedAlloys ? 1 : 0) + (ownsPhobolog ? 1 : 0));
    }

    if (this.ownsSpecialEffect(player, SpecialEffect.EARTH_CATAPULT_DISCOUNT_2)) {
      discount += 2;
    }

    if (this.ownsSpecialEffect(player, SpecialEffect.ORBITAL_OUTPOST_DISCOUNT) && (!card.tags || card.tags.length <= 1)) {
      discount += 3;
    }

    if (this.cardHasTag(card, Tag.ENERGY, tagInput) && this.ownsSpecialEffect(player, SpecialEffect.ENERGY_SUBSIDIES_DISCOUNT_4)) {
      discount += 4;
    }

    if (this.ownsSpecialEffect(player, SpecialEffect.INTERPLANETARY_CONFERENCE)) {
      if (this.cardHasTag(card, Tag.EARTH, tagInput)) {
        discount += 3;
      }
      if (this.cardHasTag(card, Tag.JUPITER, tagInput)) {
        discount += 3;
      }
    }

    if (this.cardHasTag(card, Tag.EVENT, tagInput) && this.ownsSpecialEffect(player, SpecialEffect.MEDIA_GROUP)) {
      discount += 5;
    }

    if (this.cardHasTag(card, Tag.EVENT, tagInput) && this.ownsSpecialEffect(player, SpecialEffect.INTERPLANETARY_CINEMATICS_DISCOUNT)) {
      discount += 2;
    }

    if (this.cardHasTag(card, Tag.ENERGY, tagInput) && this.ownsSpecialEffect(player, SpecialEffect.TORGATE_ENERGY_DISCOUNT)) {
      discount += 3;
    }

    if (this.cardHasTag(card, Tag.EARTH, tagInput) && this.ownsSpecialEffect(player, SpecialEffect.TERACTOR_EARTH_DISCOUNT)) {
      discount += 3;
    }

    if (this.ownsSpecialEffect(player, SpecialEffect.RESEARCH_OUTPOST_DISCOUNT_1)) {
      discount += 1;
    }

    if (this.ownsSpecialEffect(player, SpecialEffect.HOHMANN_DISCOUNT_1)) {
      discount += 1;
    }

    if (this.ownsSpecialEffect(player, SpecialEffect.DEV_TECHS_DISCOUNT)
      && card.cardColor === CardColor[CardColor.GREEN]) {
      discount += 2;
    }

    if (this.ownsSpecialEffect(player, SpecialEffect.LAUNCH_STAR_DISCOUNT)
      && card.cardColor === CardColor[CardColor.BLUE]) {
      discount += 3;
    }

    if (this.ownsSpecialEffect(player, SpecialEffect.CREDICOR_DISCOUNT) && card.price >= 20) {
      discount += 4;
    }

    if (game?.crysisDto?.detrimentTokens?.some(token => token === DetrimentToken.TEMPERATURE_YELLOW)) {
      discount -= 1;
    }

    if (game?.crysisDto?.detrimentTokens?.some(token => token === DetrimentToken.TEMPERATURE_RED)) {
      discount -= 3;
    }

    return Math.min(card.price, discount);
  }


  getDiscountWithOptimal(game: Game, card: Card, player: Player, tagInput: number): number {
    let discount = this.getDiscount(game, card, player, tagInput);

    const optimalBuild = this.getOptimalBuilding(card, player, discount);

    if (optimalBuild) {
      discount += optimalBuild.extraDiscount;
    }

    return Math.min(card.price, discount);
  }

  getOptimalBuilding(card: Card, player: Player, discount: number): Build {
    let optimalBuild = null;
    for (const build of player.builds) {
      if ((build.priceLimit === 0 || build.priceLimit >= card.price)
        && (build.type === BuildType.GREEN_OR_BLUE
          || (card.cardColor === CardColor.GREEN && build.type === BuildType.GREEN)
          || (card.cardColor !== CardColor.GREEN && (build.type === BuildType.BLUE_RED
            || build.type === BuildType.BLUE_RED_OR_CARD
            || build.type === BuildType.BLUE_RED_OR_MC)))) {
        if (optimalBuild === null) {
          optimalBuild = build;
        }
        const buildRealDiscount = Math.min(card.price - Math.min(card.price, discount), build.extraDiscount);
        const optimalBuildRealDiscount = Math.min(card.price - Math.min(card.price, discount), optimalBuild.extraDiscount);
        if ((buildRealDiscount > optimalBuildRealDiscount || build.priceLimit < optimalBuild.priceLimit)
          || (buildRealDiscount === optimalBuildRealDiscount && build.priceLimit === optimalBuild.priceLimit
            && build.type === BuildType.BLUE_RED)) {
          optimalBuild = build;
        }
      }
    }
    return optimalBuild;
  }

  private ownsSpecialEffect(player: Player, targetEffect: SpecialEffect): boolean {
    return player.played?.some(card => card?.specialEffects.some(effect => effect === targetEffect));
  }

  private cardHasTag(card: Card, targetTag: Tag, tagInput: number): boolean {
    if (card.tags?.some(tag => tag === Tag.DYNAMIC) && tagInput >= 0 && targetTag === this.allTags[tagInput]) {
      return true;
    }
    return card.tags?.some(tag => tag === targetTag);
  }

}

