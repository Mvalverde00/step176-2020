import {PieChartManager} from '../../main/webapp/piechart-manager.js';
import {stubGoogleAPIs} from './stubs/google-charts.js';
import {ChartUtil} from '../../main/webapp/chart-util.js';
jest.mock('../../main/webapp/chart-util.js');

stubGoogleAPIs();

// Used to create a PieChartManager
const MANAGER_CONTAINER_ID = 'chart-1';
const MANAGER_COLS = {'DEVICE_ID': 0, 'DEVICE_COUNT': 0};

beforeEach(() => {
  jest.clearAllMocks();
});

test('Constructor adds piechart to DOM with appropriate settings', () => {
  let wrapperConstructor = jest.spyOn(google.visualization, 'ChartWrapper');
  let manager = createManagerCorrectly();

  // Store as reference, not value
  expect(manager.COLS).toBe(MANAGER_COLS);

  expect(wrapperConstructor.mock.calls.length).toBe(1);
  expect(wrapperConstructor.mock.calls[0][0].containerId).toBe(MANAGER_CONTAINER_ID);
  expect(wrapperConstructor.mock.calls[0][0].options.width).toBe(350);
  expect(wrapperConstructor.mock.calls[0][0].chartType).toBe('PieChart');
});

test('Constructor fails if target container does not exist', () => {
  const cols = {'DEVICE_ID': 0, 'DEVICE_COUNT': 0};
  expect(() => {new PieChartManager('doesnt-exist', cols)}).toThrowError();
});

test('Configures deepest aggregation pie chart correctly', () => {
  let manager = createManagerCorrectly();
  let chart = new google.visualization.ChartWrapper();
  let data = new google.visualization.DataTable();
  let selectorState = ['Location'];

  let title = jest.spyOn(chart, 'setOption');
  let branchControl = jest.spyOn(manager, 'isLastAggregation');

  manager.configurePieChart(chart, data, selectorState, 1, null);
  expect(title.mock.calls[0]).toEqual(['title', 'Devices by ' + selectorState[0]]);

  // Make sure we added the correct event
  expect(branchControl.mock.results[0].value).toBeTruthy();
  expect(ChartUtil.addOverwriteableEvent.mock.calls.length).toBe(1);
});

test('Configures inner aggregation pie chart correctly', () => {
  let manager = createManagerCorrectly();
  let chart = new google.visualization.ChartWrapper();
  let parent = new google.visualization.ChartWrapper();
  let data = new google.visualization.DataTable();
  let selectorState = ['User', 'Location', 'Asset ID'];

  let parentTitle = jest.spyOn(parent, 'getOption');
  let branchControl = jest.spyOn(manager, 'isLastAggregation');

  manager.configurePieChart(chart, data, selectorState, 2, parent);

  // Should be called once in setTitle for non-null parent
  expect(ChartUtil.getSelectedValue.mock.calls.length).toBe(1);

  // Child title should be based on parent title
  expect(parentTitle.mock.calls[0][0]).toEqual('title');

  // Make sure we added the correct event
  expect(branchControl.mock.results[0].value).toBeFalsy();
  expect(ChartUtil.addOverwriteableEvent.mock.calls.length).toBe(1);
});


function createManagerCorrectly() {
  let container = document.createElement('div');
  container.setAttribute('id', MANAGER_CONTAINER_ID);
  document.body.appendChild(container);

  return new PieChartManager(MANAGER_CONTAINER_ID, MANAGER_COLS);
}
