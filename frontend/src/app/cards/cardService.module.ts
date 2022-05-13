import {NgModule} from '@angular/core';
import {CardServiceComponent} from './cardService.component';
import {BrowserModule} from '@angular/platform-browser';
import {ModelModule} from '../model/model.module';
import {CardTemplateComponent} from './cardTemplate.template';

@NgModule({
  imports: [BrowserModule, ModelModule],
  declarations: [CardServiceComponent, CardTemplateComponent],
  exports: [CardServiceComponent, CardTemplateComponent]
})
export class CardServiceModule {

}
