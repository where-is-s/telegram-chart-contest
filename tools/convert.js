#!/usr/local/bin/node

const fs = require('fs');
const path = require('path');
//let content = fs.readFileSync(process.argv[2]);
//let json = JSON.parse(content);

const getAllEndDirs = dir =>
    fs.readdirSync(dir).reduce((files, file) => {
        const name = path.join(dir, file);
        const isDirectory = fs.statSync(name).isDirectory();
        return isDirectory ? [...files, name, ...getAllEndDirs(name)] : [...files];//[...files, name];
    }, []);

const dirs = getAllEndDirs(process.argv[2]);
// console.log('Dirs found: ' + JSON.stringify(dirs));

for (var dir = 0; dir < dirs.length; ++dir) {

  var javaCode = `package contest.example;

import java.util.Arrays;

import contest.datasource.ChartDataSource;
import contest.datasource.ChartType;
import contest.datasource.ColumnDataSource;
import contest.datasource.ColumnType;
import contest.datasource.DateColumnDataSource;
import contest.datasource.IntegerColumnDataSource;
import contest.datasource.SimpleChartDataSource;

/**
 * Generated automatically
 */
public class Data_` + dirs[dir].split(path.sep)[1] + ` {
`;

  let jsonFileNames = fs.readdirSync(dirs[dir]).filter(file => !fs.statSync(path.join(dirs[dir], file)).isDirectory());
//  console.log(jsonFileNames);

  let jsons = jsonFileNames.map(jsonFileName => JSON.parse(fs.readFileSync(path.join(dirs[dir], jsonFileName))));

//  console.log(jsons);

  let chart = jsons[0];
  let complexChart = jsons.length > 1;

  for (var j = 1; j < jsons.length; ++j) {
    for (var c = 0; c < chart.columns.length; ++c) {
      jsons[j].columns[c].splice(0, 1);
      chart.columns[c] = chart.columns[c].concat(jsons[j].columns[c]);
    }
  }

  var chartType = 'ChartType.LINE';
  var doubleAxis = 'false';
  if (chart.y_scaled) {
    doubleAxis = 'true';
  }
  if (chart.types.y0 === 'bar') {
    chartType = 'ChartType.BAR_STACK';
  }
  if (chart.percentage) {
    chartType = 'ChartType.PERCENTAGE';
  }

  javaCode += '    public static final SimpleChartDataSource chartDataSource = new SimpleChartDataSource(';
  javaCode += chartType + ', ';
  javaCode += doubleAxis + ', ';
  javaCode += 'Arrays.<ColumnDataSource>asList(\n';
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
    javaCode += '            new ' + (type == 'x' ? 'Date': 'Integer') + 'ColumnDataSource(';
    if (type == 'x') {
      javaCode += 'ColumnType.X';
    } else {
      javaCode += 'ChartDataSource.YAxis' + ((c === 2 && doubleAxis === 'true') ? '.RIGHT' : '.LEFT');
      javaCode += ', ColumnType.LINE';
    }
    javaCode += ', "' + name + '", ' + color + ', ';
    if (complexChart) {
      javaCode += 'null';
      var s = '';
      var lastv = 0;
      var wstream = fs.createWriteStream('Data_' + dirs[dir].split(path.sep)[1] + '_' + c + '.bin');
      var fbuf = new Buffer(4);
      fbuf.writeInt32BE(column.length - 1, 0);
      wstream.write(fbuf);
      for (var v = 1; v < column.length; ++v) {
        var buf = new Buffer(4);
        var val = column[v];
        if (c == 0) {
          if (val % 1000 !== 0) {
            console.log('ERROR');
            process.exit(1);
          }
          val /= 1000;
          if (val > 2000000000 || val < -2000000000) {
            console.log('ERROR');
            process.exit(1);
          }
        }
        buf.writeInt32BE(val - lastv, 0);
        lastv = val;
        wstream.write(buf);
      }
      wstream.end();
    } else {
      javaCode += 'new long[]{\n';
      var s = column[1] + 'L';
      for (var v = 1; v < column.length; ++v) {
        if (v % 1000 === 0) {
          s += '\n                ';
        }
        s += ',' + column[v] + 'L';
      }
      javaCode += '                ' + s + '}';
    }
    javaCode += ')';
    if (c < chart.columns.length - 1) {
      javaCode += ',';
    }
    javaCode += '\n';
  }
  javaCode += `    ));
}
`;
//  console.log(javaCode);
  fs.writeFileSync('Data_' + dirs[dir].split(path.sep)[1] + '.java', javaCode);
}