import {Component, Input, OnInit} from '@angular/core';
import BaseListComponent from '../mixins/base-list.component';
import {IFilterableList} from '../mixins/ifilterable-list';

@Component({
  selector: 'filter-area-footer',
  templateUrl: './filter-area-footer.component.html',
  // styleUrls: ['./grid-header.component.css']
})
export class FilterAreaFooterComponent implements OnInit {

  constructor() {
  }

  @Input()
  parent: BaseListComponent<any> & IFilterableList;

  ngOnInit() {
  }

  toggleAdvancedSearch() {
    this.parent.advancedSearch = true;
    return false; // to prevent default navigation
  }

  toggleBasicSearch() {
    this.parent.advancedSearch = false;

    this.parent.resetAdvancedSearchParams();
    return false;
  }

}
