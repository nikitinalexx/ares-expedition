import {Injectable} from '@angular/core';
import {RestDataSource} from './rest.datasource';
import {ProjectCard} from './projectCard.model';

@Injectable()
export class Model {
  private projects: ProjectCard[] = new Array<ProjectCard>();

  constructor(private dataSource: RestDataSource) {
    this.dataSource.getData().subscribe(data => this.projects = data);
  }

  getProjectCards(): ProjectCard[] {
    return this.projects;
  }

}
