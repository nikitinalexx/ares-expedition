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

@NgModule({
  imports: [BrowserModule, ModelModule, ReactiveFormsModule, FormsModule, CardServiceModule],
  declarations: [GameComponent, PickCorporationComponent, PickPhaseComponent,
    FirstPhaseComponent, SecondPhaseComponent, SellCardsComponent, FourthPhaseComponent
  ],
  exports: [GameComponent, PickCorporationComponent, PickPhaseComponent,
    FirstPhaseComponent, SecondPhaseComponent, SellCardsComponent, FourthPhaseComponent],
  providers: [SellCardsComponent]
})
export class GameModule {

}
