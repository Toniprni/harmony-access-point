import {
  AfterViewChecked,
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  OnInit,
  TemplateRef,
  ViewChild
} from '@angular/core';
import {AlertService} from '../common/alert/alert.service';
import mix from '../common/mixins/mixin.utils';
import BaseListComponent from '../common/mixins/base-list.component';
import {ClientPageableListMixin} from '../common/mixins/pageable-list.mixin';
import * as moment from 'moment';
import {MatDialog} from '@angular/material';
import {ApplicationContextService} from '../common/application-context.service';
import {ComponentName} from '../common/component-name-decorator';
import {ClientSortableListMixin} from '../common/mixins/sortable-list.mixin';
import {DomainService} from '../security/domain.service';

/**
 * @author Ion Perpegel
 * @since 5.0
 *
 * Domains management page
 */
@Component({
  moduleId: module.id,
  templateUrl: 'domains.component.html',
  styleUrls: ['domains.component.css'],
  providers: []
})
@ComponentName('Domains')
export class DomainsComponent extends mix(BaseListComponent).with(ClientPageableListMixin, ClientSortableListMixin)
  implements OnInit, AfterViewInit, AfterViewChecked {

  @ViewChild('rowActions', {static: false}) rowActions: TemplateRef<any>;
  @ViewChild('monitorStatus', {static: false}) statusTemplate: TemplateRef<any>;

  constructor(private alertService: AlertService, private domainService: DomainService, private changeDetector: ChangeDetectorRef) {
    super();
  }

  ngOnInit() {
    super.ngOnInit();

    this.loadServerData();
  }

  ngAfterViewInit() {
    this.initColumns();
  }

  ngAfterViewChecked() {
    this.changeDetector.detectChanges();
  }

  private async getDataAndSetResults() {
    let rows = await this.domainService.getAllDomains();
    super.rows = rows;
    super.count = this.rows.length;
  }

  private initColumns() {

    this.columnPicker.allColumns = [
      {
        name: 'Domain Code',
        prop: 'code',
        width: 10
      },
      {
        name: 'Domain Name',
        prop: 'name',
        width: 10
      },
      {
        cellTemplate: this.statusTemplate,
        name: 'Active',
        prop: 'active',
        width: 20,
        canAutoResize: true,
        sortable: false
      },
      {
        cellTemplate: this.rowActions,
        name: 'Actions',
        prop: 'actions',
        width: 60,
        canAutoResize: true,
        sortable: false
      }
    ];
    this.columnPicker.selectedColumns = this.columnPicker.allColumns;
  }

  formatDate(dt) {
    return dt ? moment(dt).fromNow() : '';
  }

  async toggleConnectionMonitor(row: any) {

    // let newMonitoredValue = row.monitored;
    // let newMonitorState = `${(newMonitoredValue ? 'enabled' : 'disabled')}`;
    //
    // try {
    //   await this.connectionsMonitorService.setMonitorState(row.partyId, newMonitoredValue);
    //   row.monitored = newMonitoredValue;
    //   this.alertService.success(`Monitoring ${newMonitorState} for <b>${row.partyId}</b>`);
    // } catch (err) {
    //   row.monitored = !newMonitoredValue;
    //   this.alertService.exception(`Monitoring could not be ${newMonitorState} for <b>${row.partyId}</b>:<br>`, err);
    // }

  }


}
