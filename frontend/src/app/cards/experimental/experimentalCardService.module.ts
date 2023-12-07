import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {RouterModule} from "@angular/router";
import {ModelModule} from "../../model/model.module";
import {DiscountComponent} from "../../discount/discount.component";
import {RequirementsComponent} from "../../requirements/requirements.component";
import {ExperimentalCardServiceComponent} from "./experimentalCardService.component";
import {CardServiceModule} from "../cardService.module";

@NgModule({
  imports: [BrowserModule, ModelModule, RouterModule, CardServiceModule],
  declarations: [ExperimentalCardServiceComponent],
  exports: [ExperimentalCardServiceComponent],
  providers: [DiscountComponent, RequirementsComponent]
})
export class ExperimentalCardServiceModule {

}
