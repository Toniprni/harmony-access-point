import {HttpClient, HttpParams} from '@angular/common/http';
import {AlertService} from 'app/common/alert/alert.service';
import {Injectable} from '@angular/core';
import {PartyResponseRo} from '../../party/support/party';

/**
 * @Author Dussart Thomas
 * @Since 3.3
 */

@Injectable()
export class ConnectionsMonitorService {

  static readonly ALL_PARTIES_URL: string = 'rest/party/list?pageSize=0';
  static readonly TEST_SERVICE_PARTIES_URL: string = 'rest/testservice/parties';
  static readonly TEST_SERVICE_SENDER_URL: string = 'rest/testservice/sender';
  static readonly CONNECTION_MONITOR_URL: string = 'rest/testservice/connectionmonitor';
  static readonly TEST_SERVICE_URL: string = 'rest/testservice';
  static readonly PROPERTIES_SERVICE_URL: string = 'rest/configuration/properties';

  constructor(private http: HttpClient, private alertService: AlertService) {

  }

  async getMonitors(): Promise<ConnectionMonitorEntry[]> {
    let allParties = await this.http.get<PartyResponseRo[]>(ConnectionsMonitorService.ALL_PARTIES_URL).toPromise();
    if (!allParties || !allParties.length) {
      this.alertService.error('The Pmode is not properly configured.');
      return [];
    }

    let parties = await this.http.get<any[]>(ConnectionsMonitorService.TEST_SERVICE_PARTIES_URL).toPromise();
    if (!parties || !parties.length) {
      this.alertService.error('The test service is not properly configured.');
      return [];
    }
    console.log('parties ', parties)
    let monitors = await this.getMonitorsForParties(parties);
    console.log('monitors ', monitors);

    return allParties.map(p => {
      let m: ConnectionMonitorEntry = new ConnectionMonitorEntry();
      m.partyId = p.identifiers[0].partyId;
      Object.assign(m, monitors[m.partyId]);
      return m;
    });
  }

  async getMonitor(partyId: string): Promise<ConnectionMonitorEntry> {
    let monitors = await this.getMonitorsForParties([partyId]);
    console.log('monitors ', monitors);
    return monitors[partyId];
  }

  private getMonitorsForParties(partyIds: string[]): Promise<Map<string, ConnectionMonitorEntry>> {
    let url = ConnectionsMonitorService.CONNECTION_MONITOR_URL;
    let searchParams = new HttpParams();
    partyIds.forEach(partyId => searchParams = searchParams.append('partyIds', partyId));
    return this.http.get<Map<string, ConnectionMonitorEntry>>(url, {params: searchParams}).toPromise();
  }

  getSenderParty() {
    return this.http.get<string>(ConnectionsMonitorService.TEST_SERVICE_SENDER_URL).toPromise();
  }

  async sendTestMessage(receiverPartyId: string, sender?: string) {
    console.log('sending test message to ', receiverPartyId);

    if (!sender) {
      sender = await this.getSenderParty();
      // TODO: exception handling
    }
    const payload = {sender: sender, receiver: receiverPartyId};
    return await this.http.post<string>(ConnectionsMonitorService.TEST_SERVICE_URL, payload).toPromise();
  }

  async setMonitorState(partyId: string, enabled: boolean) {
    let propName = 'domibus.monitoring.connection.party.enabled';
    let url = ConnectionsMonitorService.PROPERTIES_SERVICE_URL + '?name=' + propName + '&showDomain=true';
    let r = (await this.http.get<any>(url).toPromise()).items[0].value; // TODO
    let enabledParties = r.split(',').map(p => p.trim()).filter(p => p != partyId).join(',');
    if (enabled) enabledParties += ',' + partyId;

    url = ConnectionsMonitorService.PROPERTIES_SERVICE_URL + '/' + propName;
    return await this.http.put(url, enabledParties || " ").toPromise();
  }

}

export class ConnectionMonitorEntry {
  partyId: string;
  testable: boolean;
  monitored: boolean;
  status: string;
  lastSent: any;
  lastReceived: any;
}
