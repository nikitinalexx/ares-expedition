import {NgModule} from '@angular/core';
import {CardServiceComponent} from './cardService.component';
import {BrowserModule} from '@angular/platform-browser';
import {ModelModule} from '../model/model.module';
import {CardTemplateComponent} from './cardTemplate.template';
import {DiscountComponent} from '../discount/discount.component';
import {RequirementsComponent} from '../requirements/requirements.component';

@NgModule({
  imports: [BrowserModule, ModelModule],
  declarations: [CardServiceComponent, CardTemplateComponent],
  exports: [CardServiceComponent, CardTemplateComponent],
  providers: [DiscountComponent, RequirementsComponent]
})
export class CardServiceModule {

}
