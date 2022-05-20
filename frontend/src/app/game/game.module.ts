import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {ModelModule} from '../model/model.module';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {GameComponent} from './game.component';
import {CardServiceModule} from '../cards/cardService.module';
import {PickCorporationComponent} from '../turns/pickCorporation/pickCorporation.component';
import {PickPhaseComponent} from '../turns/pickPhase/pickPhase.component';
import {FirstPhaseComponent} from '../turns/firstPhase/firstPhase.component';
import {SellCardsComponent} from '../turns/sellCards/sellCards.component';
import {SecondPhaseComponent} from '../turns/secondPhase/secondPhase.component';
import {FourthPhaseComponent} from '../turns/fourthPhase/fourthPhase.component';
import {ThirdPhaseComponent} from '../turns/thirdPhase/thirdPhase.component';
import {FifthPhaseComponent} from '../turns/fifthPhase/fifthPhase.component';
import {EndRoundComponent} from '../turns/endRoundPhase/endRound.component';
import {NavbarComponent} from "../navbar/navbar.component";

@NgModule({
  imports: [BrowserModule, ModelModule, ReactiveFormsModule, FormsModule, CardServiceModule],
  declarations: [GameComponent, PickCorporationComponent, PickPhaseComponent,
    FirstPhaseComponent, SecondPhaseComponent, SellCardsComponent, FourthPhaseComponent,
    ThirdPhaseComponent, FifthPhaseComponent, EndRoundComponent, NavbarComponent
  ],
  exports: [GameComponent, PickCorporationComponent, PickPhaseComponent, ThirdPhaseComponent,
    FirstPhaseComponent, SecondPhaseComponent, SellCardsComponent, FourthPhaseComponent,
    FifthPhaseComponent, EndRoundComponent, NavbarComponent],
  providers: [SellCardsComponent]
})
export class GameModule {

}
