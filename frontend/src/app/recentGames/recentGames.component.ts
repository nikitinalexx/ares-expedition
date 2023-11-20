import {Component, Inject, InjectionToken, OnInit} from '@angular/core';
import {GameRepository} from '../model/gameRepository.model';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {Subscription} from 'rxjs';
import {RecentGameDto} from "../data/RecentGameDto";

export const BASE_URL = new InjectionToken('rest_url');

@Component({
  selector: 'app-recent-games',
  templateUrl: './recentGames.component.html',
  styleUrls: ['./recentGames.component.css']
})
export class RecentGamesComponent implements OnInit {
  recentGames: RecentGameDto[];
  errorMessage: string;
  loading: boolean;
  loadingResult: Subscription;

  parentForm: FormGroup;

  constructor(private model: GameRepository,
              private formBuilder: FormBuilder,
              @Inject(BASE_URL) private url: string) {
  }

  ngOnInit(): void {
    this.parentForm = this.formBuilder.group({
      playerCount: ['1', Validators.required]
    });

    this.loadRecords();
  }


  loadRecords() {
    this.recentGames = null;
    this.loading = true;
    this.loadingResult?.unsubscribe();
    const observable = this.model.getRecentGames();
    this.loadingResult = observable.subscribe(response => {
      if (response) {
        this.loading = false;
        this.recentGames = response;
      }
    }, error => {
      this.loading = false;
      this.errorMessage = error;
    });
  }

  getPlayerLink(player: string): string {
    return this.url + '/game/' + player;
  }

}
