import {Component, Input} from '@angular/core';
import {Card} from '../data/Card';
import {CardColor} from '../data/CardColor';
import {SpecialEffect} from '../data/SpecialEffect';
import {GainType} from '../data/GainType';
import {Gain} from '../data/Gain';
import {CardAction} from '../data/CardAction';
import {PARAMETER_COLORS, ParameterColor} from '../data/ParameterColor';
import {DiscountComponent} from '../discount/discount.component';
import {BasePlayer} from '../data/BasePlayer';
import {Player} from '../data/Player';
import {Expansion} from "../data/Expansion";

@Component({
  selector: 'app-card-template',
  templateUrl: 'cardTemplate.template.html',
  styleUrls: ['cardTemplate.template.css']
})
export class CardTemplateComponent {
  @Input()
  card: Card;
  @Input()
  player: BasePlayer;
  @Input()
  showDiscount: boolean;
  @Input()
  showResources: boolean;

  constructor(private discountService: DiscountComponent) {
  }

  discountApplicable(card: Card): boolean {
    if (!this.showDiscount) {
      return false;
    }
    return this.discountService.isDiscountApplicable(card, this.player as Player);
  }

  getDiscount(card: Card): number {
    return this.discountService.getDiscount(card, this.player as Player);
  }

  getTagClasses(card: Card, tagNumber: number): string {
    if (card.tags[tagNumber]) {
      return 'tag-' + card.tags[tagNumber].toString().toLowerCase();
    }
  }

  getBackgroundColorClass(card: Card): string {
    if (card.name === 'Processing Plant') {
      console.log(card.cardColor);
    }
    switch (card.cardColor) {
      case CardColor.BLUE:
        return 'background-color-active';
      case CardColor.GREEN:
        return 'background-color-automated';
      case CardColor.RED:
        return 'background-color-events';
    }
  }

  getFormattedId(card: Card): string {
    return ('000' + card.id).substr(-3);
  }

  hasAmplifyGlobalRequirements(card: Card): boolean {
    return this.hasSpecialEffect(card, SpecialEffect.AMPLIFY_GLOBAL_REQUIREMENT);
  }

  hasAdvancedAlloysRequirements(card: Card): boolean {
    return this.hasSpecialEffect(card, SpecialEffect.ADVANCED_ALLOYS);
  }

  hasSpecialEffect(card: Card, specialEffect: SpecialEffect): boolean {
    return card.specialEffects.indexOf(SpecialEffect[specialEffect]) > -1;
  }

  hasBonuses(card: Card): boolean {
    return card.bonuses && card.bonuses.length !== 0;
  }

  manyBonuses(card: Card): boolean {
    return card.bonuses.length >= 3;
  }

  hasIncomes(card: Card): boolean {
    return card.incomes && card.incomes.length !== 0;
  }

  hasMcBonus(card: Card): boolean {
    return card.bonuses.find(gain => gain.type === GainType[GainType.MC]) !== undefined;
  }

  getOceanBonuses(card: Card): number[] {
    const bonus = card.bonuses.find(gain => gain.type === GainType[GainType.OCEAN]);
    if (!bonus) {
      return [];
    }
    return this.generateArray(bonus.value);
  }

  getOxygenBonuses(card: Card): number[] {
    const bonus = card.bonuses.find(gain => gain.type === GainType[GainType.OXYGEN]);
    if (!bonus) {
      return [];
    }
    return this.generateArray(bonus.value);
  }

  getForestBonuses(card: Card): number[] {
    const bonus = card.bonuses.find(gain => gain.type === GainType[GainType.FOREST]);
    if (!bonus) {
      return [];
    }
    return this.generateArray(bonus.value);
  }

  getTerraformingBonuses(card: Card): number[] {
    const bonus = card.bonuses.find(gain => gain.type === GainType[GainType.TERRAFORMING_RATING]);
    if (!bonus) {
      return [];
    }
    return this.generateArray(Math.abs(bonus.value));
  }

  getTerraformingBonusSign(card: Card): string {
    const bonus = card.bonuses.find(gain => gain.type === GainType[GainType.TERRAFORMING_RATING]);
    return bonus.value < 0 ? '-' : '';
  }

  getPlantBonuses(card: Card): number[] {
    const bonus = card.bonuses.find(gain => gain.type === GainType[GainType.PLANT]);
    if (!bonus) {
      return [];
    }
    return this.generateArray(bonus.value);
  }

  getHeatBonuses(card: Card): number[] {
    const bonus = card.bonuses.find(gain => gain.type === GainType[GainType.HEAT]);
    if (!bonus) {
      return [];
    }
    return this.generateArray(bonus.value);
  }

  getCardBonuses(card: Card): number[] {
    const bonus = card.bonuses.find(gain => gain.type === GainType[GainType.CARD]);
    if (!bonus) {
      return [];
    }
    return this.generateArray(bonus.value);
  }

  getTemperatureBonuses(card: Card): number[] {
    const bonus = card.bonuses.find(gain => gain.type === GainType[GainType.TEMPERATURE]);
    if (!bonus) {
      return [];
    }
    return this.generateArray(bonus.value);
  }

  getMcBonus(card: Card): number {
    const bonus = card.bonuses.find(gain => gain.type === GainType[GainType.MC]);
    if (!bonus) {
      return null;
    }
    return bonus.value;
  }

  getIncomes(card: Card): Gain[] {
    return card.incomes;
  }

  getIncomeClass(income: Gain): string {
    switch (income.type) {
      case GainType.PLANT:
        return 'plant';
      case GainType.CARD:
        return 'card';
      case GainType.HEAT:
        return 'heat';
      case GainType.STEEL:
        return 'steel';
      case GainType.TITANIUM:
        return 'titanium';
      case GainType.MC:
        return 'money';
      default:
        return '';
    }
  }

  getNameStyle(card: Card): { 'font-size.px': number } {
    if (card.name.length > 14) {
      const proportion = card.name.length / 14;
      return {'font-size.px': 16 / proportion};
    }
    return;
  }

  generateArray(size: number): number[] {
    const result = [];
    for (let i = 0; i < size; i++) {
      result.push(i);
    }
    return result;
  }

  actionScreeningTechnology(card: Card): boolean {
    return card.cardAction === CardAction.SCREENING_TECHNOLOGY;
  }

  actionAiCentral(card: Card): boolean {
    return card.cardAction === CardAction.AI_CENTRAL;
  }

  actionAnaerobicOrganisms(card: Card): boolean {
    return card.cardAction === CardAction.ANAEROBIC_MICROORGANISMS;
  }

  actionAntiGravity(card: Card): boolean {
    return card.cardAction === CardAction.ANTI_GRAVITY_TECH;
  }

  actionAquiferPumping(card: Card): boolean {
    return card.cardAction === CardAction.AQUIFER_PUMPING;
  }

  actionArcticAlgae(card: Card): boolean {
    return card.cardAction === CardAction.ARCTIC_ALGAE;
  }

  actionArtificialJungle(card: Card): boolean {
    return card.cardAction === CardAction.ARTIFICIAL_JUNGLE;
  }

  actionAssemblyLines(card: Card): boolean {
    return card.cardAction === CardAction.ASSEMBLY_LINES;
  }

  actionAssetLiquidation(card: Card): boolean {
    return card.cardAction === CardAction.ASSET_LIQUIDATION;
  }

  actionAddAnimal(card: Card): boolean {
    return card.cardAction === CardAction.ADD_ANIMAL;
  }

  actionFilterFeeders(card: Card): boolean {
    return card.cardAction === CardAction.FILTER_FEEDERS;
  }

  actionBrainstormingSession(card: Card): boolean {
    return card.cardAction === CardAction.BRAINSTORMING_SESSION;
  }

  actionCaretakerContract(card: Card): boolean {
    return card.cardAction === CardAction.CARETAKER_CONTRACT;
  }

  actionCircuitBoard(card: Card): boolean {
    return card.cardAction === CardAction.CIRCUIT_BOARD;
  }

  actionCommunityGardens(card: Card): boolean {
    return card.cardAction === CardAction.COMMUNITY_GARDENS;
  }

  actionCompostingFactory(card: Card): boolean {
    return card.cardAction === CardAction.COMPOSTING_FACTORY;
  }

  actionConservedBiome(card: Card): boolean {
    return card.cardAction === CardAction.CONSERVED_BIOME;
  }

  actionDecomposers(card: Card): boolean {
    return card.cardAction === CardAction.DECOMPOSERS;
  }

  actionDecomposingFungus(card: Card): boolean {
    return card.cardAction === CardAction.DECOMPOSING_FUNGUS;
  }

  actionDevelopedInfrastructure(card: Card): boolean {
    return card.cardAction === CardAction.DEVELOPED_INFRASTRUCTURE;
  }

  actionDevelopmentCenter(card: Card): boolean {
    return card.cardAction === CardAction.DEVELOPMENT_CENTER;
  }

  actionEarthCatapult(card: Card): boolean {
    return card.cardAction === CardAction.EARTH_CATAPULT;
  }

  actionEcologicalZone(card: Card): boolean {
    return card.cardAction === CardAction.ECOLOGICAL_ZONE;
  }

  actionEnergySubsidies(card: Card): boolean {
    return card.cardAction === CardAction.ENERGY_SUBSIDIES;
  }

  actionExtendedResources(card: Card): boolean {
    return card.cardAction === CardAction.EXTENDED_RESOURCES;
  }

  actionExtremeColdFungus(card: Card): boolean {
    return card.cardAction === CardAction.EXTREME_COLD_FUNGUS;
  }

  actionFarmersMarket(card: Card): boolean {
    return card.cardAction === CardAction.FARMERS_MARKET;
  }

  actionFarmingCoops(card: Card): boolean {
    return card.cardAction === CardAction.FARMING_COOPS;
  }

  actionMatterGenerator(card: Card): boolean {
    return card.cardAction === CardAction.MATTER_GENERATOR;
  }

  actionFish(card: Card): boolean {
    return card.cardAction === CardAction.FISH;
  }

  actionGhgProduction(card: Card): boolean {
    return card.cardAction === CardAction.GHG_PRODUCTION;
  }

  actionGreenHouses(card: Card): boolean {
    return card.cardAction === CardAction.GREEN_HOUSES;
  }

  actionHerbivores(card: Card): boolean {
    return card.cardAction === CardAction.HERBIVORES;
  }

  actionHydroElectric(card: Card): boolean {
    return card.cardAction === CardAction.HYDRO_ELECTRIC;
  }

  actionInterns(card: Card): boolean {
    return card.cardAction === CardAction.INTERNS;
  }

  actionInterplanetaryConference(card: Card): boolean {
    return card.cardAction === CardAction.INTERPLANETARY_CONFERENCE;
  }

  actionInterplanetaryRelations(card: Card): boolean {
    return card.cardAction === CardAction.INTERPLANETARY_RELATIONS;
  }

  actionIronWorks(card: Card): boolean {
    return card.cardAction === CardAction.IRON_WORKS;
  }

  actionProgressivePolicies(card: Card): boolean {
    return card.cardAction === CardAction.PROGRESSIVE_POLICIES;
  }

  actionLivestock(card: Card): boolean {
    return card.cardAction === CardAction.LIVESTOCK;
  }

  actionMarsUniversity(card: Card): boolean {
    return card.cardAction === CardAction.MARS_UNIVERSITY;
  }

  actionMatterManufactoring(card: Card): boolean {
    return card.cardAction === CardAction.MATTER_MANUFACTORING;
  }

  actionMediaGroup(card: Card): boolean {
    return card.cardAction === CardAction.MEDIA_GROUP;
  }

  actionNitriteReducting(card: Card): boolean {
    return card.cardAction === CardAction.NITRITE_REDUCTING;
  }

  actionSelfReplicatingBacteria(card: Card): boolean {
    return card.cardAction === CardAction.SELF_REPLICATING_BACTERIA;
  }

  actionOlympusConference(card: Card): boolean {
    return card.cardAction === CardAction.OLYMPUS_CONFERENCE;
  }

  actionOptimalAerobraking(card: Card): boolean {
    return card.cardAction === CardAction.OPTIMAL_AEROBRAKING;
  }

  actionPhysicsComplex(card: Card): boolean {
    return card.cardAction === CardAction.PHYSICS_COMPLEX;
  }

  actionPowerInfrastructure(card: Card): boolean {
    return card.cardAction === CardAction.POWER_INFRASTRUCTURE;
  }

  actionRecycledDetritus(card: Card): boolean {
    return card.cardAction === CardAction.RECYCLED_DETRITUS;
  }

  actionRedraftedContracts(card: Card): boolean {
    return card.cardAction === CardAction.REDRAFTED_CONTRACTS;
  }

  actionRegolithEaters(card: Card): boolean {
    return card.cardAction === CardAction.REGOLITH_EATERS;
  }

  actionResearchOutpost(card: Card): boolean {
    return card.cardAction === CardAction.RESEARCH_OUTPOST;
  }

  actionRestructuredResources(card: Card): boolean {
    return card.cardAction === CardAction.RESTRUCTURED_RESOURCES;
  }

  actionSmallAnimals(card: Card): boolean {
    return card.cardAction === CardAction.SMALL_ANIMALS;
  }

  actionSolarPunk(card: Card): boolean {
    return card.cardAction === CardAction.SOLAR_PUNK;
  }

  actionStandardTechnology(card: Card): boolean {
    return card.cardAction === CardAction.STANDARD_TECHNOLOGY;
  }

  actionSteelworks(card: Card): boolean {
    return card.cardAction === CardAction.STEELWORKS;
  }

  actionSymbioticFungus(card: Card): boolean {
    return card.cardAction === CardAction.SYMBIOTIC_FUNGUD;
  }

  actionAddMicrobe(card: Card): boolean {
    return card.cardAction === CardAction.ADD_MICROBE;
  }

  actionThinktank(card: Card): boolean {
    return card.cardAction === CardAction.THINKTANK;
  }

  actionUnitedPlanetary(card: Card): boolean {
    return card.cardAction === CardAction.UNITED_PLANETARY;
  }

  actionViralEnhancers(card: Card): boolean {
    return card.cardAction === CardAction.VIRAL_ENHANCERS;
  }

  actionVolcanicPools(card: Card): boolean {
    return card.cardAction === CardAction.VOLCANIC_POOLS;
  }

  actionWaterImport(card: Card): boolean {
    return card.cardAction === CardAction.WATER_IMPORT;
  }

  actionWoodBurningStoves(card: Card): boolean {
    return card.cardAction === CardAction.WOOD_BURNING_STOVES;
  }

  capitalizeDescription(card: Card): boolean {
    return card.cardAction === CardAction.CAPITALISE_DESCRIPTION ||
      card.cardAction === CardAction.SYNTHETIC_CATASTROPHE;
  }

  actionImportedHydrogen(card: Card): boolean {
    return card.cardAction === CardAction.IMPORTED_HYDROGEN;
  }

  actionLargeConvoy(card: Card): boolean {
    return card.cardAction === CardAction.LARGE_CONVOY;
  }

  actionProcessedMetals(card: Card): boolean {
    return card.cardAction === CardAction.PROCESSED_METALS;
  }

  actionProcessingPlant(card: Card): boolean {
    return card.cardAction === CardAction.PROCESSING_PLANT;
  }

  actionImportedNitrogen(card: Card): boolean {
    return card.cardAction === CardAction.IMPORTED_NITROGEN;
  }

  actionLocalHeatTrapping(card: Card): boolean {
    return card.cardAction === CardAction.LOCAL_HEAT_TRAPPING;
  }

  actionNitrogenRichAsteroid(card: Card): boolean {
    return card.cardAction === CardAction.NITROGEN_RICH_ASTEROID;
  }

  actionSpecialDesign(card: Card): boolean {
    return card.cardAction === CardAction.SPECIAL_DESIGN;
  }

  actionTerraformingGanymede(card: Card): boolean {
    return card.cardAction === CardAction.TERRAFORMING_GANYMEDE;
  }

  actionWorkCrews(card: Card): boolean {
    return card.cardAction === CardAction.WORK_CREWS;
  }

  actionAstrofarm(card: Card): boolean {
    return card.cardAction === CardAction.ASTROFARM;
  }

  actionHeatEarthIncome(card: Card): boolean {
    return card.cardAction === CardAction.HEAT_EARTH_INCOME;
  }

  actionMcAnimalPlantIncome(card: Card): boolean {
    return card.cardAction === CardAction.MC_ANIMAL_PLANT_INCOME;
  }

  actionCardScienceIncome(card: Card): boolean {
    return card.cardAction === CardAction.CARD_SCIENCE_INCOME;
  }

  actionMcEarthIncome(card: Card): boolean {
    return card.cardAction === CardAction.MC_EARTH_INCOME;
  }

  actionPlantPlantIncome(card: Card): boolean {
    return card.cardAction === CardAction.PLANT_PLANT_INCOME;
  }

  actionMcScienceIncome(card: Card): boolean {
    return card.cardAction === CardAction.MC_SCIENCE_INCOME;
  }

  actionMc2BuildingIncome(card: Card): boolean {
    return card.cardAction === CardAction.MC_2_BUILDING_INCOME;
  }

  actionMcEnergyIncome(card: Card): boolean {
    return card.cardAction === CardAction.MC_ENERGY_INCOME;
  }

  actionMcSpaceIncome(card: Card): boolean {
    return card.cardAction === CardAction.MC_SPACE_INCOME;
  }

  actionHeatSpaceIncome(card: Card): boolean {
    return card.cardAction === CardAction.HEAT_SPACE_INCOME;
  }

  actionMcEventIncome(card: Card): boolean {
    return card.cardAction === CardAction.MC_EVENT_INCOME;
  }

  actionHeatEnergyIncome(card: Card): boolean {
    return card.cardAction === CardAction.HEAT_ENERGY_INCOME;
  }

  actionPlantMicrobeIncome(card: Card): boolean {
    return card.cardAction === CardAction.PLANT_MICROBE_INCOME;
  }

  actionMcForestIncome(card: Card): boolean {
    return card.cardAction === CardAction.MC_FOREST_INCOME;
  }

  actionBiomassCombustors(card: Card): boolean {
    return card.cardAction === CardAction.BIOMASS_COMBUSTORS;
  }

  actionBuildingIndustries(card: Card): boolean {
    return card.cardAction === CardAction.BUILDING_INDUSTRIES;
  }

  actionEnergyStorage(card: Card): boolean {
    return card.cardAction === CardAction.ENERGY_STORAGE;
  }

  actionEosChasma(card: Card): boolean {
    return card.cardAction === CardAction.EOS_CHASMA;
  }

  actionFuelFactory(card: Card): boolean {
    return card.cardAction === CardAction.FUEL_FACTORY;
  }

  actionFoodFactory(card: Card): boolean {
    return card.cardAction === CardAction.FOOD_FACTORY;
  }
  actionMoss(card: Card): boolean {
    return card.cardAction === CardAction.MOSS;
  }

  actionTallStation(card: Card): boolean {
    return card.cardAction === CardAction.TALL_STATION;
  }

  actionAutomatedFactories(card: Card): boolean {
    return card.cardAction === CardAction.AUTOMATED_FACTORIES;
  }

  actionTropicalIsland(card: Card): boolean {
    return card.cardAction === CardAction.TROPICAL_ISLAND;
  }

  isHelionCorporation(card: Card): boolean {
    return card.cardAction === CardAction.HELION_CORPORATION;
  }

  isCelestiorCorporation(card: Card): boolean {
    return card.cardAction === CardAction.CELESTIOR_CORPORATION;
  }

  isDevTechsCorporation(card: Card): boolean {
    return card.cardAction === CardAction.DEVTECHS_CORPORATION;
  }

  isUnmiCorporation(card: Card): boolean {
    return card.cardAction === CardAction.UNMI_CORPORATION;
  }

  isInterplanetaryCinematics(card: Card): boolean {
    return card.cardAction === CardAction.INTERPLANETARY_CINEMATICS;
  }

  isMiningGuildCorporation(card: Card): boolean {
    return card.cardAction === CardAction.MINING_GUILD_CORPORATION;
  }

  isSaturnSystemsCorporation(card: Card): boolean {
    return card.cardAction === CardAction.SATURN_SYSTEMS_CORPORATION;
  }

  isLaunchStarCorporation(card: Card): boolean {
    return card.cardAction === CardAction.LAUNCH_STAR_CORPORATION;
  }

  isZetacellCorporation(card: Card): boolean {
    return card.cardAction === CardAction.ZETACELL_CORPORATION;
  }

  isEcolineCorporation(card: Card): boolean {
    return card.cardAction === CardAction.ECOLINE_CORPORATION;
  }

  isInventrixCorporation(card: Card): boolean {
    return card.cardAction === CardAction.INVENTRIX_CORPORATION;
  }

  isMayNiProductionsCorporation(card: Card): boolean {
    return card.cardAction === CardAction.MAY_NI_PRODUCTIONS_CORPORATION;
  }

  isThorgateCorporation(card: Card): boolean {
    return card.cardAction === CardAction.THORGATE_CORPORATION;
  }

  isTharsisCorporation(card: Card): boolean {
    return card.cardAction === CardAction.THARSIS_CORPORATION;
  }

  isArclightCorporation(card: Card): boolean {
    return card.cardAction === CardAction.ARCLIGHT_CORPORATION;
  }

  isPhobologCorporation(card: Card): boolean {
    return card.cardAction === CardAction.PHOBOLOG_CORPORATION;
  }

  isCredicorCorporation(card: Card): boolean {
    return card.cardAction === CardAction.CREDICOR_CORPORATION;
  }

  isTeractorCorporation(card: Card): boolean {
    return card.cardAction === CardAction.TERACTOR_CORPORATION;
  }

  hasTagRequirements(card: Card): boolean {
    return card.tagReq.length !== 0;
  }

  hasAtLeastFourTagRequirements(card: Card): boolean {
    return card.tagReq.length >= 4;
  }

  hasOneToThreeTagRequirements(card: Card): boolean {
    return card.tagReq.length >= 1 && card.tagReq.length <= 3;
  }

  displayOceanRequirementAsNumber(card: Card): boolean {
    return card.oceanRequirement.minValue === 0 && card.oceanRequirement.maxValue > 3
      || card.oceanRequirement.maxValue === 9 && card.oceanRequirement.minValue > 3;
  }

  getOceanDisplayNumber(card: Card): number {
    if (card.oceanRequirement.minValue === 0) {
      return card.oceanRequirement.maxValue;
    } else {
      return card.oceanRequirement.minValue;
    }
  }

  getOceanRequirementNumber(card: Card): number {
    if (this.displayOceanRequirementAsNumber(card)) {
      return 1;
    } else if (card.oceanRequirement.minValue === 0) {
      return card.oceanRequirement.maxValue;
    } else {
      return card.oceanRequirement.minValue;
    }
  }

  hasTempRequirements(card: Card): boolean {
    return card.tempReq && card.tempReq.length !== 0;
  }

  hasOxygenRequirements(card: Card): boolean {
    return card.oxygenReq && card.oxygenReq.length !== 0;
  }

  hasOceanRequirement(card: Card): boolean {
    return card.oceanRequirement !== null;
  }

  getTemperatureClass(card: Card): string {
    return this.getTemperatureOxygenClass(card.tempReq);
  }

  getOxygenClass(card: Card): string {
    return this.getTemperatureOxygenClass(card.oxygenReq);
  }

  getOceanRequirementValue(card: Card): string {
    if (card.oceanRequirement.minValue === 0) {
      return 'Max ' + card.oceanRequirement.maxValue;
    } else {
      return 'Min ' + card.oceanRequirement.minValue;
    }
  }

  private getTemperatureOxygenClass(req: ParameterColor[]): string {
    if (req.length === 3 && req[0] === PARAMETER_COLORS[ParameterColor.R]) {
      return 'requirements-ryw';
    }
    if (req.length === 2 && req[0] === PARAMETER_COLORS[ParameterColor.Y]) {
      return 'requirements-yw';
    }
    if (req.length === 2 && req[0] === PARAMETER_COLORS[ParameterColor.P]) {
      return 'requirements-pr';
    }
    if (req.length === 1 && req[0] === PARAMETER_COLORS[ParameterColor.W]) {
      return 'requirements-w';
    }
    if (req.length === 1 && req[0] === PARAMETER_COLORS[ParameterColor.P]) {
      return 'requirements-p';
    }
  }

  isBuffedCorporation(card: Card): boolean {
    return card.expansion === Expansion.BUFFED_CORPORATION;
  }

  getTagRequirements(card: Card): string {
    if (card.tagReq.length > 1 && card.tagReq[0] !== card.tagReq[1]) {
      return card.tagReq.map(tag => this.capitalizeFirstLetter(tag.toString().toLowerCase())).join(' ');
    }
    return card.tagReq.length + ' ' + this.capitalizeFirstLetter(card.tagReq[0].toString().toLowerCase());
  }

  capitalizeFirstLetter(s: string): string {
    return s[0].toUpperCase() + s.slice(1);
  }
}
