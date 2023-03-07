import {Component, Inject, InjectionToken, OnInit} from '@angular/core';
import {FormBuilder, FormGroup} from '@angular/forms';


export const BASE_URL = new InjectionToken('rest_url');

@Component({
  selector: 'app-crisis-rules',
  templateUrl: './crisisRules.component.html',
  styleUrls: ['./crisisRules.component.css']
})
export class CrisisRulesComponent implements OnInit {
  public errorMessage: string;

  parentForm: FormGroup;

  constructor(private formBuilder: FormBuilder,
              @Inject(BASE_URL) private url: string) {
  }

  ngOnInit(): void {
    this.parentForm = this.formBuilder.group({});
  }


}
