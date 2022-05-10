import {NgModule} from '@angular/core';
import {CardServiceComponent} from './cardService.component';
import {BrowserModule} from '@angular/platform-browser';
import {ModelModule} from '../model/model.module';

@NgModule({
  imports: [BrowserModule, ModelModule],
  declarations: [CardServiceComponent],
  exports: [CardServiceComponent]
})
export class CardServiceModule {

}
