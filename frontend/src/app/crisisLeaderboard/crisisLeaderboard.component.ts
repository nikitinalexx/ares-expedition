import {Component, Inject, InjectionToken, OnInit} from '@angular/core';
import {CrisisRecordEntity} from '../data/CrisisRecordEntity';
import {GameRepository} from '../model/gameRepository.model';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {Subscription} from 'rxjs';
import {SoloRecordEntity} from '../data/SoloRecordEntity';

export const BASE_URL = new InjectionToken('rest_url');

@Component({
  selector: 'app-crisis-leaderboard',
  templateUrl: './crisisLeaderboard.component.html',
  styleUrls: ['./crisisLeaderboard.component.css']
})
export class CrisisLeaderboardComponent implements OnInit {
  crisisRecords: CrisisRecordEntity[];
  soloRecords: SoloRecordEntity[];
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
      playerCount: ['1', Validators.required],
      sortType: '1',
      recordType: 'solo'
    });

    this.loadSoloRecords();
  }

  loadCrisisRecords(playerCount: number, sortType: number) {
    this.soloRecords = null;
    this.crisisRecords = null;
    this.loading = true;
    this.loadingResult?.unsubscribe();
    const observable = (sortType === 1
      ? this.model.getCrisisRecordsByPoints(playerCount)
      : this.model.getCrisisRecordsByTurns(playerCount));
    this.loadingResult = observable.subscribe(response => {
      if (response) {
        this.loading = false;
        this.crisisRecords = response;
      }
    }, error => {
      this.loading = false;
      this.errorMessage = error;
    });
  }

  loadSoloRecords() {
    this.soloRecords = null;
    this.crisisRecords = null;
    this.loading = true;
    this.loadingResult?.unsubscribe();
    const observable = this.model.getSoloRecordsByTurns();
    this.loadingResult = observable.subscribe(response => {
      if (response) {
        this.loading = false;
        this.soloRecords = response;
      }
    }, error => {
      this.loading = false;
      this.errorMessage = error;
    });
  }

  clickPlayerCount(playerCount: number) {
    this.loadCrisisRecords(playerCount, this.parentForm.value.sortType);
  }

  clickSortType(sortType: number) {
    this.loadCrisisRecords(this.parentForm.value.playerCount, sortType);
  }

  clickRecordType(type: number) {
    if (type === 1) {
      this.loadSoloRecords();
    } else if (type === 2) {
      this.loadCrisisRecords(this.parentForm.value.playerCount, this.parentForm.value.sortType);
    }
  }

  getPlayerLink(player: string): string {
    return this.url + '/game/' + player;
  }

}
