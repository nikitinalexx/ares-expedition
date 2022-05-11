import {Component} from '@angular/core';
import {CardRepository} from '../model/cardRepository.model';
import {ProjectCard} from '../data/ProjectCard';
import {CardColor} from '../data/CardColor';
import {SpecialEffect} from '../data/SpecialEffect';
import {CardAction} from "../data/CardAction";
import {ParameterColor} from "../data/ParameterColor";

@Component({
  selector: 'app-project-cards',
  templateUrl: './cardService.component.html'
})
export class CardServiceComponent {

  constructor(private model: CardRepository) {
  }

  getProjectCards(): ProjectCard[] {
    return this.model.getProjectCards();
  }

  getTagClasses(card: ProjectCard, tagNumber: number): string {
    if (card.tags[tagNumber]) {
      return 'tag-' + card.tags[tagNumber].toString().toLowerCase();
    }
  }

  getBackgroundColorClass(card: ProjectCard): string {
    switch (card.cardColor) {
      case CardColor.BLUE:
        return 'background-color-active';
      case CardColor.GREEN:
        return 'background-color-automated';
      case CardColor.RED:
        return 'background-color-events';
    }
  }

  getFormattedId(card: ProjectCard): string {
    return ('000' + card.id).substr(-3);
  }

  hasAmplifyGlobalRequirements(card: ProjectCard): boolean {
    return this.hasSpecialEffect(card, SpecialEffect.AMPLIFY_GLOBAL_REQUIREMENT);
  }

  hasAdvancedAlloysRequirements(card: ProjectCard): boolean {
    return this.hasSpecialEffect(card, SpecialEffect.ADVANCED_ALLOYS);
  }

  hasSpecialEffect(card: ProjectCard, specialEffect: SpecialEffect): boolean {
    return card.specialEffects.indexOf(SpecialEffect[specialEffect]) > -1;
  }

  actionScreeningTechnology(card: ProjectCard): boolean {
    return card.cardAction === CardAction.SCREENING_TECHNOLOGY;
  }

  actionAiCentral(card: ProjectCard): boolean {
    return card.cardAction === CardAction.AI_CENTRAL;
  }

  actionAnaerobicOrganisms(card: ProjectCard): boolean {
    return card.cardAction === CardAction.ANAEROBIC_MICROORGANISMS;
  }

  actionAntiGravity(card: ProjectCard): boolean {
    return card.cardAction === CardAction.ANTI_GRAVITY_TECH;
  }

  actionAquiferPumping(card: ProjectCard): boolean {
    return card.cardAction === CardAction.AQUIFER_PUMPING;
  }

  actionArcticAlgae(card: ProjectCard): boolean {
    return card.cardAction === CardAction.ARCTIC_ALGAE;
  }

  actionArtificialJungle(card: ProjectCard): boolean {
    return card.cardAction === CardAction.ARTIFICIAL_JUNGLE;
  }

  actionAssemblyLines(card: ProjectCard): boolean {
    return card.cardAction === CardAction.ASSEMBLY_LINES;
  }

  actionAssetLiquidation(card: ProjectCard): boolean {
    return card.cardAction === CardAction.ASSET_LIQUIDATION;
  }

  actionAddAnimal(card: ProjectCard): boolean {
    return card.cardAction === CardAction.ADD_ANIMAL;
  }

  actionBrainstormingSession(card: ProjectCard): boolean {
    return card.cardAction === CardAction.BRAINSTORMING_SESSION;
  }

  actionCaretakerContract(card: ProjectCard): boolean {
    return card.cardAction === CardAction.CARETAKER_CONTRACT;
  }

  actionCircuitBoard(card: ProjectCard): boolean {
    return card.cardAction === CardAction.CIRCUIT_BOARD;
  }

  actionCommunityGardens(card: ProjectCard): boolean {
    return card.cardAction === CardAction.COMMUNITY_GARDENS;
  }

  actionCompostingFactory(card: ProjectCard): boolean {
    return card.cardAction === CardAction.COMPOSTING_FACTORY;
  }

  actionConservedBiome(card: ProjectCard): boolean {
    return card.cardAction === CardAction.CONSERVED_BIOME;
  }

  actionDecomposers(card: ProjectCard): boolean {
    return card.cardAction === CardAction.DECOMPOSERS;
  }

  actionDecomposingFungus(card: ProjectCard): boolean {
    return card.cardAction === CardAction.DECOMPOSING_FUNGUS;
  }

  actionDevelopedInfrastructure(card: ProjectCard): boolean {
    return card.cardAction === CardAction.DEVELOPED_INFRASTRUCTURE;
  }

  actionDevelopmentCenter(card: ProjectCard): boolean {
    return card.cardAction === CardAction.DEVELOPMENT_CENTER;
  }

  actionEarthCatapult(card: ProjectCard): boolean {
    return card.cardAction === CardAction.EARTH_CATAPULT;
  }

  actionEcologicalZone(card: ProjectCard): boolean {
    return card.cardAction === CardAction.ECOLOGICAL_ZONE;
  }

  actionEnergySubsidies(card: ProjectCard): boolean {
    return card.cardAction === CardAction.ENERGY_SUBSIDIES;
  }

  actionExtendedResources(card: ProjectCard): boolean {
    return card.cardAction === CardAction.EXTENDED_RESOURCES;
  }

  actionExtremeColdFungus(card: ProjectCard): boolean {
    return card.cardAction === CardAction.EXTREME_COLD_FUNGUS;
  }

  actionFarmersMarket(card: ProjectCard): boolean {
    return card.cardAction === CardAction.FARMERS_MARKET;
  }

  actionFarmingCoops(card: ProjectCard): boolean {
    return card.cardAction === CardAction.FARMING_COOPS;
  }

  actionFish(card: ProjectCard): boolean {
    return card.cardAction === CardAction.FISH;
  }

  actionGhgProduction(card: ProjectCard): boolean {
    return card.cardAction === CardAction.GHG_PRODUCTION;
  }

  actionGreenHouses(card: ProjectCard): boolean {
    return card.cardAction === CardAction.GREEN_HOUSES;
  }

  actionHerbivores(card: ProjectCard): boolean {
    return card.cardAction === CardAction.HERBIVORES;
  }

  actionHydroElectric(card: ProjectCard): boolean {
    return card.cardAction === CardAction.HYDRO_ELECTRIC;
  }

  actionInterns(card: ProjectCard): boolean {
    return card.cardAction === CardAction.INTERNS;
  }

  actionInterplanetaryConference(card: ProjectCard): boolean {
    return card.cardAction === CardAction.INTERPLANETARY_CONFERENCE;
  }

  actionInterplanetaryRelations(card: ProjectCard): boolean {
    return card.cardAction === CardAction.INTERPLANETARY_RELATIONS;
  }

  actionIronWorks(card: ProjectCard): boolean {
    return card.cardAction === CardAction.IRON_WORKS;
  }

  actionLivestock(card: ProjectCard): boolean {
    return card.cardAction === CardAction.LIVESTOCK;
  }

  actionMarsUniversity(card: ProjectCard): boolean {
    return card.cardAction === CardAction.MARS_UNIVERSITY;
  }

  actionMatterManufactoring(card: ProjectCard): boolean {
    return card.cardAction === CardAction.MATTER_MANUFACTORING;
  }

  actionMediaGroup(card: ProjectCard): boolean {
    return card.cardAction === CardAction.MEDIA_GROUP;
  }

  actionNitriteReducting(card: ProjectCard): boolean {
    return card.cardAction === CardAction.NITRITE_REDUCTING;
  }

  actionOlympusConference(card: ProjectCard): boolean {
    return card.cardAction === CardAction.OLYMPUS_CONFERENCE;
  }

  actionOptimalAerobraking(card: ProjectCard): boolean {
    return card.cardAction === CardAction.OPTIMAL_AEROBRAKING;
  }

  actionPhysicsComplex(card: ProjectCard): boolean {
    return card.cardAction === CardAction.PHYSICS_COMPLEX;
  }

  actionPowerInfrastructure(card: ProjectCard): boolean {
    return card.cardAction === CardAction.POWER_INFRASTRUCTURE;
  }

  actionRecycledDetritus(card: ProjectCard): boolean {
    return card.cardAction === CardAction.RECYCLED_DETRITUS;
  }

  actionRedraftedContracts(card: ProjectCard): boolean {
    return card.cardAction === CardAction.REDRAFTED_CONTRACTS;
  }

  actionRegolithEaters(card: ProjectCard): boolean {
    return card.cardAction === CardAction.REGOLITH_EATERS;
  }

  actionResearchOutpost(card: ProjectCard): boolean {
    return card.cardAction === CardAction.RESEARCH_OUTPOST;
  }

  actionRestructuredResources(card: ProjectCard): boolean {
    return card.cardAction === CardAction.RESTRUCTURED_RESOURCES;
  }

  actionSmallAnimals(card: ProjectCard): boolean {
    return card.cardAction === CardAction.SMALL_ANIMALS;
  }

  actionSolarPunk(card: ProjectCard): boolean {
    return card.cardAction === CardAction.SOLAR_PUNK;
  }

  actionStandardTechnology(card: ProjectCard): boolean {
    return card.cardAction === CardAction.STANDARD_TECHNOLOGY;
  }

  actionSteelworks(card: ProjectCard): boolean {
    return card.cardAction === CardAction.STEELWORKS;
  }

  actionSymbioticFungus(card: ProjectCard): boolean {
    return card.cardAction === CardAction.SYMBIOTIC_FUNGUD;
  }

  actionAddMicrobe(card: ProjectCard): boolean {
    return card.cardAction === CardAction.ADD_MICROBE;
  }

  actionThinktank(card: ProjectCard): boolean {
    return card.cardAction === CardAction.THINKTANK;
  }

  actionUnitedPlanetary(card: ProjectCard): boolean {
    return card.cardAction === CardAction.UNITED_PLANETARY;
  }

  actionViralEnhancers(card: ProjectCard): boolean {
    return card.cardAction === CardAction.VIRAL_ENHANCERS;
  }

  actionVolcanicPools(card: ProjectCard): boolean {
    return card.cardAction === CardAction.VOLCANIC_POOLS;
  }

  actionWaterImport(card: ProjectCard): boolean {
    return card.cardAction === CardAction.WATER_IMPORT;
  }

  actionWoodBurningStoves(card: ProjectCard): boolean {
    return card.cardAction === CardAction.WOOD_BURNING_STOVES;
  }

  hasTagRequirements(card: ProjectCard): boolean {
    return card.tagReq.length !== 0;
  }

  hasTempRequirements(card: ProjectCard): boolean {
    return card.tempReq && card.tempReq.length !== 0;
  }

  hasOxygenRequirements(card: ProjectCard): boolean {
    return card.oxygenReq && card.oxygenReq.length !== 0;
  }

  hasOceanRequirement(card: ProjectCard): boolean {
    return card.oceanRequirement !== null;
  }

  getTemperatureClass(card: ProjectCard): string {
    return this.getTemperatureOxygenClass(card.tempReq);
  }

  getOxygenClass(card: ProjectCard): string {
    return this.getTemperatureOxygenClass(card.oxygenReq);
  }

  getOceanRequirementValue(card: ProjectCard): string {
    if (card.oceanRequirement.minValue === 0) {
      return 'Max ' + card.oceanRequirement.maxValue;
    } else {
      return 'Min ' + card.oceanRequirement.minValue;
    }
  }

  private getTemperatureOxygenClass(req: ParameterColor[]): string {
    if (req.length === 3 && req[0] === ParameterColor.RED) {
      return 'requirements-ryw';
    }
    if (req.length === 2 && req[0] === ParameterColor.YELLOW) {
      return 'requirements-yw';
    }
    if (req.length === 1 && req[0] === ParameterColor.WHITE) {
      return 'requirements-w';
    }
    if (req.length === 1 && req[0] === ParameterColor.PURPLE) {
      return 'requirements-p';
    }
  }

  getTagRequirements(card: ProjectCard): string {
    if (card.tagReq.length > 1 && card.tagReq[0] !== card.tagReq[1]) {
      return card.tagReq.map(tag => this.capitalizeFirstLetter(tag.toString().toLowerCase())).join(' ');
    }
    return card.tagReq.length + ' ' + this.capitalizeFirstLetter(card.tagReq[0].toString().toLowerCase());
  }

  capitalizeFirstLetter(s: string): string {
    return s[0].toUpperCase() + s.slice(1);
  }

}
