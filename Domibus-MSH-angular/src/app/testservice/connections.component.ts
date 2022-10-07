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
import {ConnectionMonitorEntry, ConnectionsMonitorService} from './support/connectionsmonitor.service';
import {MatDialog} from '@angular/material';
import {ConnectionDetailsComponent} from './connection-details/connection-details.component';
import {ApplicationContextService} from '../common/application-context.service';
import {ComponentName} from '../common/component-name-decorator';
import {PartyResponseRo} from '../party/support/party';
import {MatSelectChange} from '@angular/material/select';

/**
 * @author Ion Perpegel
 * @since 4.2
 *
 * Connections monitor form.
 */
@Component({
  moduleId: module.id,
  templateUrl: 'connections.component.html',
  styleUrls: ['connections.component.css'],
  providers: [ConnectionsMonitorService]
})
@ComponentName('Connection Monitoring')
export class ConnectionsComponent extends mix(BaseListComponent).with(ClientPageableListMixin)
  implements OnInit, AfterViewInit, AfterViewChecked {

  @ViewChild('rowActions', {static: false}) rowActions: TemplateRef<any>;
  @ViewChild('monitorStatus', {static: false}) monitorStatusTemplate: TemplateRef<any>;
  @ViewChild('connectionStatus', {static: false}) connectionStatusTemplate: TemplateRef<any>;
  currentSenderPartyId: any;
  sender: PartyResponseRo;

  constructor(private applicationService: ApplicationContextService, private connectionsMonitorService: ConnectionsMonitorService,
              private alertService: AlertService, private dialog: MatDialog, private changeDetector: ChangeDetectorRef) {
    super();
  }

  async ngOnInit() {
    super.ngOnInit();

    this.sender = await this.connectionsMonitorService.getSenderParty();
    let partyIds = this.sender.identifiers;
    partyIds.sort((id1, id2) => id1.partyId.localeCompare(id2.partyId));
    this.setCurrentSenderPartyId(partyIds[0].partyId);

    this.loadServerData();
  }

  ngAfterViewInit() {
    this.initColumns();
  }

  ngAfterViewChecked() {
    this.changeDetector.detectChanges();
  }

  private async getDataAndSetResults() {
    let rows: ConnectionMonitorEntry[] = await this.connectionsMonitorService.getMonitors(this.currentSenderPartyId);
    rows.forEach(entry => {
      entry.senderPartyId = this.sender.name + '(' + this.currentSenderPartyId + ')';
    });
    super.rows = rows;
    super.count = this.rows.length;
  }

  private initColumns() {

    this.columnPicker.allColumns = [
      {
        name: 'Sender Party',
        prop: 'senderPartyId',
        width: 10
      },
      {
        name: 'Responder Party',
        prop: 'partyName',
        width: 10
      },
      {
        cellTemplate: this.monitorStatusTemplate,
        name: 'Monitoring',
        prop: 'monitorStatus',
        width: 20,
        canAutoResize: true,
        sortable: false
      },
      {
        cellTemplate: this.connectionStatusTemplate,
        name: 'Connection Status',
        prop: 'connectionStatus',
        width: 170,
        canAutoResize: true,
        sortable: false
      },
      {
        cellTemplate: this.rowActions,
        name: 'Actions',
        prop: 'actions',
        width: 30,
        canAutoResize: true,
        sortable: false
      }
    ];
    this.columnPicker.selectedColumns = this.columnPicker.allColumns;
  }

  formatDate(dt) {
    return dt ? moment(dt).fromNow() : '';
  }

  async toggleConnectionMonitor(row: ConnectionMonitorEntry) {

    let newMonitoredValue = row.monitored;
    let newMonitorState = `${(newMonitoredValue ? 'enabled' : 'disabled')}`;

    try {
      await this.connectionsMonitorService.setMonitorState(this.currentSenderPartyId, row.partyId, newMonitoredValue);
      row.monitored = newMonitoredValue;
      this.alertService.success(`Monitoring ${newMonitorState} for <b>${row.partyId}</b>`);
    } catch (err) {
      row.monitored = !newMonitoredValue;
      this.alertService.exception(`Monitoring could not be ${newMonitorState} for <b>${row.partyId}</b>:<br>`, err);
    }
  }

  async sendTestMessage(row: ConnectionMonitorEntry) {
    row.status = 'PENDING';
    let messageId = await this.connectionsMonitorService.sendTestMessage(row.partyId, this.currentSenderPartyId);
    await this.refreshMonitor(row);
  }

  async refreshMonitor(row: ConnectionMonitorEntry) {
    let refreshedRow = await this.connectionsMonitorService.getMonitor(this.currentSenderPartyId, row.partyId);
    Object.assign(row, refreshedRow);

    if (row.status == 'PENDING') {
      setTimeout(() => this.refreshMonitor(row), 1500);
    }
  }

  openDetails(row: ConnectionMonitorEntry) {
    this.dialog.open(ConnectionDetailsComponent, {
      data: {
        senderPartyId: this.currentSenderPartyId,
        partyId: row.partyId
      }
    }).afterClosed().subscribe(result => {
      this.refreshMonitor(row);
    });
  }

  onCurrentSenderPartyId($event: MatSelectChange) {
    console.log($event)
    this.setCurrentSenderPartyId($event.value);
  }

  private async setCurrentSenderPartyId(value: any) {
    this.currentSenderPartyId = value;
    await this.getDataAndSetResults();

    // this.rows.forEach(entry => {
    //   entry.senderPartyId = this.sender.name + '(' + this.currentSenderPartyId + ')';
    // });
    // will read the monitor enabled properties for this party id
  }
}
