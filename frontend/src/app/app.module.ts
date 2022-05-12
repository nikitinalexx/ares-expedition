import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';

import {AppComponent} from './app.component';
import {CardServiceModule} from './cards/cardService.module';
import {RouterModule} from '@angular/router';
import {CardServiceComponent} from './cards/cardService.component';
import {NewGameComponent} from './newGame/newGame.component';
import {IndexModule} from './index/index.module';
import {NewGameModule} from './newGame/newGame.module';
import {IndexComponent} from './index/index.component';
import {APP_BASE_HREF} from '@angular/common';

@NgModule({
  imports: [
    BrowserModule, CardServiceModule, NewGameModule, IndexModule,
    RouterModule.forRoot([
      {path: 'cards', component: CardServiceComponent},
      {path: 'new', component: NewGameComponent},
      {path: 'index', component: IndexComponent},
      {path: '**', redirectTo: '/index'}
    ])],
  declarations: [
    AppComponent
  ],
  bootstrap: [AppComponent],
  providers: [{provide: APP_BASE_HREF, useValue: '/'}]
})
export class AppModule {
}
