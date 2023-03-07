import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {ModelModule} from '../model/model.module';
import {BASE_URL, NewGameComponent} from './newGame.component';
import {ReactiveFormsModule, FormsModule} from '@angular/forms';
import {RouterModule} from "@angular/router";

@NgModule({
    imports: [BrowserModule, ModelModule, ReactiveFormsModule, FormsModule, RouterModule],
  declarations: [NewGameComponent],
  exports: [NewGameComponent],
  providers: [{provide: BASE_URL, useValue: `http://${location.hostname}:${location.port}`}]
})
export class NewGameModule {

}
