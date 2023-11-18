import { CrisisRecordEntity } from './CrisisRecordEntity';

export class CrisisRecordsDto {
  constructor() {}
  topTwentyRecordsByPoints: CrisisRecordEntity[];
  topTwentyRecordsByTurns: CrisisRecordEntity[];
}
