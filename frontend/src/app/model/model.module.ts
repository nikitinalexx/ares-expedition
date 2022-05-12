import { NgModule } from '@angular/core';
import { HttpClientModule, HttpClientJsonpModule } from '@angular/common/http';
import {CardRepository} from './cardRepository.model';
import {REST_URL, RestDataSource} from './rest.datasource';
import {NewGameRepository} from './newGameRepository.model';
import {GameRepository} from "./gameRepository.model";

@NgModule({
  imports: [HttpClientModule, HttpClientJsonpModule],
  providers: [CardRepository, RestDataSource, NewGameRepository, GameRepository,
    { provide: REST_URL, useValue: `http://${location.hostname}:8080` }]
})
export class ModelModule { }
