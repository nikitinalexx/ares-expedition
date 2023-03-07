import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {ModelModule} from '../model/model.module';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {BASE_URL, CrisisLeaderboardComponent} from './crisisLeaderboard.component';
import {RouterModule} from "@angular/router";

@NgModule({
  imports: [BrowserModule, ModelModule, ReactiveFormsModule, FormsModule, RouterModule],
  declarations: [CrisisLeaderboardComponent],
  exports: [CrisisLeaderboardComponent],
  providers: [{provide: BASE_URL, useValue: `http://${location.hostname}:${location.port}`}]
})
export class CrisisLeaderboardModule {

}
