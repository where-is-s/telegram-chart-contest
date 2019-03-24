const fs = require('fs');
let content = fs.readFileSync(process.argv[2]);
let json = JSON.parse(content);

console.log(`package contest.example;

import java.util.Arrays;

import contest.datasource.ChartDataSource;
import contest.datasource.ColumnDataSource;
import contest.datasource.ColumnType;
import contest.datasource.DateColumnDataSource;
import contest.datasource.IntegerColumnDataSource;
import contest.datasource.SimpleChartDataSource;

/**
 * Generated automatically from chart_data.json
 */
public class Data {
`);
console.log('    public static final ChartDataSource chartDataSources[] = new ChartDataSource[]{');

for (var i = 0; i < json.length; ++i) {
  let chart = json[i];
  console.log('        new SimpleChartDataSource(Arrays.<ColumnDataSource>asList(');
  for (var c = 0; c < chart.columns.length; ++c) {
    let column = chart.columns[c];
    let id = column[0];
    let type = chart.types[id];
    let name = chart.names[id];
    let color = chart.colors[id];
    if (color) {
      color = '0xFF' + color.substring(1);
    } else {
      color = '0';
    }
    console.log('                new ' + (type == 'x' ? 'Date': 'Integer') + 'ColumnDataSource(' + (type == 'x' ? 'ColumnType.X' : 'ColumnType.LINE') + ', "' + name + '", ' + color + ', new long[]{');
    var s = column[1];
    for (var v = 2; v < column.length; ++v) {
      s += 'L, ' + column[v];
    }
    s += 'L})';
    if (c < chart.columns.length - 1) {
      s += ',';
    }
    console.log('                    ' + s);
  }
  console.log('        ))' + ((i < json.length - 1) ? ',' : ''));
}
console.log(`    };
}
`);