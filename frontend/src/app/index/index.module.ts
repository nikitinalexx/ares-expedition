import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {ModelModule} from '../model/model.module';
import {IndexComponent} from './index.component';
import {RouterModule} from '@angular/router';

@NgModule({
  imports: [BrowserModule, ModelModule, RouterModule],
  declarations: [IndexComponent],
  exports: [IndexComponent]
})
export class IndexModule {

}
