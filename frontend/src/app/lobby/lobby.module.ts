import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {ModelModule} from '../model/model.module';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {BASE_URL, LobbyComponent} from './lobby.component';

@NgModule({
  imports: [BrowserModule, ModelModule, ReactiveFormsModule, FormsModule],
  declarations: [LobbyComponent],
  exports: [LobbyComponent],
  providers: [{provide: BASE_URL, useValue: `http://${location.hostname}:${location.port}`}]
})
export class LobbyModule {

}
