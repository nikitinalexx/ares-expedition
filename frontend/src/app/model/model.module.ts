import { NgModule } from '@angular/core';
import { HttpClientModule, HttpClientJsonpModule } from '@angular/common/http';
import {CardRepository} from './cardRepository.model';
import {REST_URL, RestDataSource} from './rest.datasource';

@NgModule({
  imports: [HttpClientModule, HttpClientJsonpModule],
  providers: [CardRepository, RestDataSource,
    { provide: REST_URL, useValue: `http://${location.hostname}:8080/projects` }]
})
export class ModelModule { }
