import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {ModelModule} from '../model/model.module';
import {FormsModule} from '@angular/forms';
import {GameComponent} from './game.component';
import {CardServiceModule} from '../cards/cardService.module';
import {PickCorporationComponent} from '../turns/pickCorporation/pickCorporation.component';
import {PickPhaseComponent} from '../turns/pickPhase/pickPhase.component';
import {FirstPhaseComponent} from '../turns/firstPhase/firstPhase.component';

@NgModule({
  imports: [BrowserModule, ModelModule, FormsModule, CardServiceModule],
  declarations: [GameComponent, PickCorporationComponent, PickPhaseComponent, FirstPhaseComponent],
  exports: [GameComponent, PickCorporationComponent, PickPhaseComponent, FirstPhaseComponent]
})
export class GameModule {

}
