import {NgModule} from '@angular/core';
import {CardServiceComponent} from './cardService.component';
import {BrowserModule} from '@angular/platform-browser';
import {ModelModule} from '../model/model.module';
import {CardTemplateComponent} from './cardTemplate.template';
import {DiscountComponent} from '../discount/discount.component';
import {RequirementsComponent} from '../requirements/requirements.component';
import {PhaseCardTemplateComponent} from './phase/phaseCardTemplate.template';
import {CrysisCardTemplateComponent} from './crysis/crysisCardTemplate.template';
import {RouterModule} from "@angular/router";

@NgModule({
    imports: [BrowserModule, ModelModule, RouterModule],
  declarations: [CardServiceComponent, CardTemplateComponent, CrysisCardTemplateComponent, PhaseCardTemplateComponent],
  exports: [CardServiceComponent, CardTemplateComponent, CrysisCardTemplateComponent, PhaseCardTemplateComponent],
  providers: [DiscountComponent, RequirementsComponent]
})
export class CardServiceModule {

}
