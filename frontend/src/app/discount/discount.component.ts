import {Injectable} from '@angular/core';
import {Card} from '../data/Card';
import {SpecialEffect} from '../data/SpecialEffect';
import {Player} from '../data/Player';
import {Tag} from '../data/Tag';
import {CardColor} from '../data/CardColor';

@Injectable()
export class DiscountComponent {

  isDiscountApplicable(card: Card, player: Player): boolean {
    if (!player) {
      return false;
    }
    return this.getDiscount(card, player) > 0;
  }

  getDiscount(card: Card, player: Player): number {
    let discount = 0;

    const ownsAdvancedAlloys = this.ownsSpecialEffect(player, SpecialEffect.ADVANCED_ALLOYS);
    const ownsPhobolog = this.ownsSpecialEffect(player, SpecialEffect.PHOBOLOG);

    if (this.cardHasTag(card, Tag.BUILDING)) {
      discount += player.steelIncome * (2 + (ownsAdvancedAlloys ? 1 : 0));
    }

    if (this.cardHasTag(card, Tag.SPACE)) {
      discount += player.titaniumIncome * (3 + (ownsAdvancedAlloys ? 1 : 0) + (ownsPhobolog ? 1 : 0));
    }

    if (card.cardColor === CardColor.GREEN && player.phase === 1) {
      discount += 3;
    }

    if (this.ownsSpecialEffect(player, SpecialEffect.EARTH_CATAPULT_DISCOUNT_2)) {
      discount += 2;
    }

    if (this.cardHasTag(card, Tag.ENERGY) && this.ownsSpecialEffect(player, SpecialEffect.ENERGY_SUBSIDIES_DISCOUNT_4)) {
      discount += 4;
    }

    if (this.ownsSpecialEffect(player, SpecialEffect.INTERPLANETARY_CONFERENCE)) {
      if (this.cardHasTag(card, Tag.EARTH)) {
        discount += 3;
      }
      if (this.cardHasTag(card, Tag.JUPITER)) {
        discount += 3;
      }
    }

    if (this.cardHasTag(card, Tag.EVENT) && this.ownsSpecialEffect(player, SpecialEffect.MEDIA_GROUP)) {
      discount += 5;
    }

    if (this.cardHasTag(card, Tag.ENERGY) && this.ownsSpecialEffect(player, SpecialEffect.TORGATE_ENERGY_DISCOUNT)) {
      discount += 3;
    }

    if (this.cardHasTag(card, Tag.EARTH) && this.ownsSpecialEffect(player, SpecialEffect.TERACTOR_EARTH_DISCOUNT)) {
      discount += 3;
    }

    if (this.ownsSpecialEffect(player, SpecialEffect.RESEARCH_OUTPOST_DISCOUNT_1)) {
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

    if (player.builtWorkCrewsLastTurn) {
      discount += 11;
    }

    if (player.canBuildAnotherGreenWith9Discount && card.price < 10) {
      discount += 9;
    }

    if (player.assortedEnterprisesDiscount) {
      discount += 2;
    }

    if (player.selfReplicatingDiscount) {
      discount += 25;
    }

    return Math.min(card.price, discount);
  }

  private ownsSpecialEffect(player: Player, targetEffect: SpecialEffect): boolean {
    return player.played?.some(card => card?.specialEffects.some(effect => effect === targetEffect));
  }

  private cardHasTag(card: Card, targetTag: Tag): boolean {
    return card.tags?.some(tag => tag === targetTag);
  }

}

