export class PhaseDescription {
  static readonly PHASE_1_MAIN = 'Each player may play 1 green project card.';
  static readonly PHASE_2_MAIN = 'Each player may play 1 blue/red project card.';
  static readonly PHASE_3_MAIN = 'Each player activates any "Action:" effects and Standard Actions';
  static readonly PHASE_4_MAIN = 'Each player gains mc, heat, plants and cards according to production level and TR.';
  static readonly PHASE_5_MAIN = 'Each player draws 2 cards and keeps 1.';

  static readonly PHASE_1_BONUS = [
    'Pay 3 MC less for the card you play this phase.',
    'Reduce the cost of the card you play this phase by 6 MC',
    'Reduce the cost of the first card by 3 MC. You may play the second green card with a printed cost of 12 MC or less.'
  ];

  static readonly PHASE_2_BONUS = [
    'Draw a card or play an additional 1 blue/red project card.',
    'Play an additional blue or red card OR gain 6 MC.',
    'Draw a card. You may play an additional blue/red project card.'
  ];

  static readonly PHASE_3_BONUS = [
    'You may repeat one of your "Action:" effects.',
    'You may repeat two of your "Action:" effects.',
    'You may repeat one of your "Action:" effects. Reveal top 3 deck cards and take all blue or red cards.'
  ];

  static readonly PHASE_4_BONUS = [
    'Gain 4 MC.',
    'Gain 7 MC.',
    'Gain 1 MC. Choose Green Card that produces twice this phase.'
  ];

  static readonly PHASE_5_BONUS = [
    'Draw an additional 3 cards and keep an additional 1.',
    'Draw an additional 2 cards and keep an additional 2.',
    'Draw an additional 6 cards and keep an additional 1.'
  ];

}
