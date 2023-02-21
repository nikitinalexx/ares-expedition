import {NgModule} from '@angular/core';
import {HttpClientJsonpModule, HttpClientModule} from '@angular/common/http';
import {CardRepository} from './cardRepository.model';
import {RestDataSource} from './rest.datasource';
import {NewGameRepository} from './newGameRepository.model';
import {GameRepository} from './gameRepository.model';
import {LobbyRepository} from "./lobbyRepository.model";

@NgModule({
  imports: [HttpClientModule, HttpClientJsonpModule],
  providers: [CardRepository, RestDataSource, NewGameRepository, GameRepository, LobbyRepository]
})
export class ModelModule {
}
