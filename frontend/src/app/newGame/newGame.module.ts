import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {ModelModule} from '../model/model.module';
import {BASE_URL, NewGameComponent} from './newGame.component';
import {FormsModule} from '@angular/forms';

@NgModule({
  imports: [BrowserModule, ModelModule, FormsModule],
  declarations: [NewGameComponent],
  exports: [NewGameComponent],
  providers: [{provide: BASE_URL, useValue: `http://${location.hostname}:${location.port}`}]
})
export class NewGameModule {

}
