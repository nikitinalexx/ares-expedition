import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {ModelModule} from '../model/model.module';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {RouterModule} from '@angular/router';
import {BASE_URL, CrisisRulesComponent} from './crisisRules.component';

@NgModule({
  imports: [BrowserModule, ModelModule, ReactiveFormsModule, FormsModule, RouterModule],
  declarations: [CrisisRulesComponent],
  exports: [CrisisRulesComponent],
  providers: [{provide: BASE_URL, useValue: `http://${location.hostname}:${location.port}`}]
})
export class CrisisRulesModule {

}
