import {Injectable} from '@angular/core';
import {Card} from '../data/Card';
import {SpecialEffect} from '../data/SpecialEffect';
import {Player} from '../data/Player';
import {Tag} from '../data/Tag';
import {CardColor} from '../data/CardColor';

@Injectable()
export class ScrollComponent {

  scrollToPlayerChoice() {
    const topOfElement = document.getElementById('displayPhaseContainer').offsetTop - 110;
    window.scroll({ top: topOfElement, behavior: 'smooth' });
  }

}

