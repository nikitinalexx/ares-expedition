import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {ModelModule} from '../model/model.module';
import {BASE_URL, NewGameCrysisComponent} from './newGameCrysis.component';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {RouterModule} from "@angular/router";

@NgModule({
  imports: [BrowserModule, ModelModule, ReactiveFormsModule, FormsModule, RouterModule],
  declarations: [NewGameCrysisComponent],
  exports: [NewGameCrysisComponent],
  providers: [{provide: BASE_URL, useValue: `http://${location.hostname}:${location.port}`}]
})
export class NewGameCrysisModule {

}
