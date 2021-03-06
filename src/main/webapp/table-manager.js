import {Loading} from './loading.js';

const PAGINATION_ENDPOINT = '/devices';
const BLANK_PAGE_TOKEN = '';
const MAX_DEVICE_COUNT_PARAMETER_NAME = 'maxDeviceCount';
const PAGE_TOKEN_PARAMETER_NAME = 'pageToken';

/*
 * Table Column Organization:
 * Aggregation table has Field 1 | ... | Field n | deviceCount | Button | deviceIds | serialNumbers | Row #
 * Normal view has Field 1 | ... | Field n
 */
class TableManager {
  constructor(containerId) {
    this.container = document.getElementById(containerId);
    this.table = this.createNewTable(containerId);
    this.baseDataTable = new google.visualization.DataTable();
    this.currPage = 0;
    this.pageSize = 10;
    this.aggregating = false;

    this.pageLeft = document.getElementById('page-left');
    this.pageRight = document.getElementById('page-right');
    this.pageNumber = document.getElementById('page-number');
    this.pageSizeSelect = document.getElementById('page-size-select');

    this.nextPageToken = BLANK_PAGE_TOKEN;

    google.visualization.events.addOneTimeListener(this.table, 'ready', () => {
      google.visualization.events.addListener(
          this.table.getChart(), 'page', this.onPageChange.bind(this));
    });

    google.visualization.events.addListener(this.table, 'ready', this.makeTableAccessible.bind(this));

    this.pageLeft.onclick = () => {
      google.visualization.events.trigger(this.table.getChart(), 'page', {'pageDelta': -1});
    };
    this.pageRight.onclick = () => {
      google.visualization.events.trigger(this.table.getChart(), 'page', {'pageDelta': 1});
    };
    this.pageSizeSelect.onchange = () => {
      this.onPageSizeChange(parseInt(this.pageSizeSelect.value));
    };
  }

  draw() {
    this.table.draw();
    this.pageNumber.innerText = this.currPage + 1;

    this.drawArrows();
  }

  drawArrows() {
    if (this.aggregating) {
      this.pageLeft.disabled = true;
      this.pageRight.disabled = true;
    } else {
      this.pageLeft.disabled = this.currPage == 0;
      this.pageRight.disabled = !this.hasNextPage();
    }
  }

  updateAggregation(dataTable) {
    this.currPage = 0;
    this.aggregating = true;
    // Google charts API has a bug where setting pageSize will cause the table to be
    // paginated, regardles of if the 'page' property is set to 'disable'.  Thus, we
    // must remove the pageSize property entirely.
    let currOptions = this.table.getOptions();

    delete currOptions['pageSize'];
    currOptions['page'] = 'disable';

    this.table.setOptions(currOptions);
    this.setTableView(dataTable.getNumberOfColumns());
    this.setDataTable(dataTable);
  }

  async updateNormal() {
    this.aggregating = false;

    this.table.setOption('page', 'event');
    this.table.setOption('pageSize', this.pageSize);

    await this.resetNormalData();
    this.setTableView(this.baseDataTable.getNumberOfColumns());
    this.setDataTable();
  }

  setDataTable(dataTable) {
    if (dataTable != null) {
      this.baseDataTable = dataTable;
    }

    if (this.aggregating) {
      this.table.setDataTable(this.baseDataTable);
    } else {
      let view = new google.visualization.DataView(this.baseDataTable);
      const startIndex = this.currPage * this.pageSize;
      const endIndex = Math.min(
          this.currPage * this.pageSize + this.pageSize - 1,
          this.baseDataTable.getNumberOfRows() - 1);
      view.setRows(startIndex, endIndex);
      this.table.setDataTable(view);
    }
  }

  setTableView(numOfCols) {
    // See guide at top of file for table layout.  In normal view, we want to display all columns;
    // in aggregation view, we ignore the last 4 columns which are used for internal purposes
    let viewableCols;
    if (!this.aggregating) {
      viewableCols = [...Array(numOfCols).keys()];
    } else {
      // Skip over deviceIds, serialNumbers, and Row #
      viewableCols = [...Array(numOfCols - 3).keys()];
    }

    this.table.setView({'columns': viewableCols});
  }

  hasNextPageCached() {
    return (this.currPage + 1) * this.pageSize < this.baseDataTable.getNumberOfRows();
  }
  
  hasNextPageRemote() {
    return this.nextPageToken != BLANK_PAGE_TOKEN;
  }

  hasNextPage() {
    return this.hasNextPageRemote() || this.hasNextPageCached();
  }


  async onPageChange(properties) {
    const pageDelta = properties['pageDelta']; // 1 or -1
    const newPage = this.currPage + pageDelta;

    if (newPage < 0 || (pageDelta > 0 && !this.hasNextPage()) ) {
      return;
    }

    // Check if we have the necessary data to change the page.  If we dont, get it.
    if (pageDelta > 0 && !this.hasNextPageCached()) {
      await new Loading(this.addDataToTable.bind(this), true).load();
    }
    this.currPage = newPage;
    this.setDataTable();

    this.draw();
  }

  async onPageSizeChange(newPageSize) {
    this.pageSize = newPageSize;
    this.table.setOption('pageSize', newPageSize);

    await this.updateNormal();

    this.draw();
  }

  async resetNormalData() {
    this.currPage = 0;
    this.nextPageToken = BLANK_PAGE_TOKEN;

    this.baseDataTable = new google.visualization.DataTable();
    this.baseDataTable.addColumn('string', 'Serial Number');
    this.baseDataTable.addColumn('string', 'Status');
    this.baseDataTable.addColumn('string', 'Asset ID');
    this.baseDataTable.addColumn('string', 'User');
    this.baseDataTable.addColumn('string', 'Location');

    let loader = new Loading(this.addDataToTable.bind(this), true);
    await loader.load();
  }

  async addDataToTable() {
    if (this.aggregating) {
      throw new Error('Method addDataToTable should not be called while table is in aggregation mode');
    }

    let url = this.buildNextPageURL();
    await fetch(url)
        .then(response => response.json())
        .then(json => {
          this.nextPageToken = json.nextPageToken == null ? BLANK_PAGE_TOKEN : json.nextPageToken;
          for (let device of json.chromeosdevices) {
            this.baseDataTable.addRow([
                device.serialNumber,
                device.status,
                device.annotatedAssetId,
                device.annotatedUser,
                device.annotatedLocation]);
          }
        });
  }

  buildNextPageURL() {
    let url = new URL(PAGINATION_ENDPOINT, window.location.href);

    let params = new URLSearchParams();
    params.append(MAX_DEVICE_COUNT_PARAMETER_NAME, this.pageSize);
    params.append(PAGE_TOKEN_PARAMETER_NAME, this.nextPageToken);

    url.search = params;

    return url;
  }

  makeTableAccessible() {
    let table = this.container.getElementsByTagName('table')[0];
    let tableHeaders = table.getElementsByTagName('th');
    let idPrefix = 'tableHeaderSpan';
    for (let [index, th] of Object.entries(tableHeaders)) {
      let nameId = idPrefix + (2 * index);
      let instructionsId = idPrefix + (2 * index + 1);
      let container = document.createElement('span');

      let nameSpanAria = document.createElement('span');
      nameSpanAria.innerText = th.innerText;
      nameSpanAria.setAttribute('id', nameId);
      nameSpanAria.classList.add('hidden');

      let innerContainer = document.createElement('span');
      innerContainer.setAttribute('aria-labelledby', instructionsId);
      innerContainer.setAttribute('role', 'button');
      innerContainer.setAttribute('tabindex', 0);

      let nameSpanDisplay = document.createElement('span');
      nameSpanDisplay.setAttribute('aria-hidden', 'true');
      nameSpanDisplay.innerText = th.innerText;

      let instructionsSpan = document.createElement('span');
      instructionsSpan.innerText = 'Activate to sort by column.';
      instructionsSpan.classList.add('hidden');
      instructionsSpan.setAttribute('id', instructionsId);

      th.innerHTML = '';

      innerContainer.append(nameSpanDisplay, instructionsSpan);
      container.append(nameSpanAria, innerContainer);
      th.appendChild(container);

      th.setAttribute('aria-labelledby', nameId);
      th.setAttribute('role', 'columnheader');
      th.removeAttribute('aria-label');
      th.removeAttribute('tabindex');
    }
  }

  createNewTable(containerId) {
    return new google.visualization.ChartWrapper({
        'chartType': 'Table',
        'containerId': containerId,
        'options': {
            'title': 'Sample Table',
            'page': 'event',
            'pageSize': 10,
            'width': '100%',
            'allowHtml': 'true',
            'cssClassNames': {
                'headerRow': 'table-header-row',
                'tableRow': 'table-row',
                'oddTableRow': 'table-row',
                'tableCell': 'table-cell'
            }
        }
    });
  }
}

export {TableManager};
