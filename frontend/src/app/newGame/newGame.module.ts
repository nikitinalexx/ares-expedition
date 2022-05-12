import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {ModelModule} from '../model/model.module';
import {NewGameComponent} from './newGame.component';

@NgModule({
  imports: [BrowserModule, ModelModule],
  declarations: [NewGameComponent],
  exports: [NewGameComponent]
})
export class NewGameModule {

}
