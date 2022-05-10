import {ProjectCard} from './projectCard.model';

export class Deck {
  constructor(public user: string, private cardsList: ProjectCard[] = []) {

  }

  get cards(): readonly ProjectCard[] {
    return this.cardsList;
  }

  addCard(card: ProjectCard) {
    this.cardsList.push(card);
  }

}
