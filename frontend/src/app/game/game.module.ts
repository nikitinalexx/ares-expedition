import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {ModelModule} from '../model/model.module';
import {FormsModule} from '@angular/forms';
import {GameComponent} from './game.component';

@NgModule({
  imports: [BrowserModule, ModelModule, FormsModule],
  declarations: [GameComponent],
  exports: [GameComponent]
})
export class GameModule {

}
