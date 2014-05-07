/* 
 * asio - js library
 * 
 * requires jquery
 * 
 * pyranja, 2014
 */

//==============================================================>
// NAMESPACE
var asio = (function() {
  var exports = {};

  // ==============================================================>
  // CONSTANTS

  // relative path from site location to asio sql endpoint
  // change this to adapt to deployment location
  var ENDPOINT_PATH = "/read/sql";
  // service suffixes
  var SERVICE_SCHEMA = "/schema";
  var SERVICE_QUERY = "/";
  var SERVICE_META = "/read/meta";

  // ==============================================================>
  // UTILS

  // guess dataset root URL from current location
  // -> strip /explore path from URL
  // -> strip file name and/or trailing slash from URL
  function guessRoot() {
    var base = $(location).attr('href').replace(/\/explore/, '').replace(
        /([^\/]*).html$/, '').replace(/\/$/, '');
    return base;
  }

  // determine the sql query endpoint of this dataset
  function endpoint() {
    return guessRoot() + ENDPOINT_PATH;
  }
  ;

  // forward a success ajax response to a nodejs style callback
  function forwardTo(callback) {
    return function(result) {
      callback(null, result);
    };
  }
  ;

  // transform a HTTP status and text message into an Error and
  // forward it to a nodejs style callback
  // TODO parse exception payload for more detailled error messages
  function escalateTo(callback) {
    return function(ignored, status, text) {
      var cause = new Error(status + ":" + text);
      callback(cause, null);
    };
  }
  ;

  // ==============================================================>
  // TYPES

  // hold result of a sql query
  // [ column_definitions ] + [ row : [ values ] ]
  function SqlResult() {
    this.columns = [];
    this.rows = [];
  }
  ;
  SqlResult.prototype.addRow = function(values) {
    this.rows.push({
      row : values
    });
  };
  exports.SqlResult = SqlResult;

  // ==============================================================>
  // FUNCTIONS
  // FIXME hacked
  exports.endpoint = function() {
    return endpoint();
  }

  // parse a xml webrowset into a SqlResult object
  exports.parseWebrowset = function(xml) {
    var table = new SqlResult();
    // parse column definitions
    table.columns = $(xml).find('column-definition').map(function() {
      return $(this).children('column-label').text();
    }).get();
    // parse each row
    $(xml).find('currentRow').each(function() {
      var values = $(this).children('columnValue').map(function() {
        return $(this).text();
      }).get();
      table.addRow(values);
    });
    return table;
  };

  // parse an OGSADAI xml database schema into a list of table names
  exports.parseDatabaseSchema = function(xml) {
    var datasource = $(xml).find('table').map(function() {
      var dsName = $(this).attr('schema');
      if (dsName == 'null') {
        dsName = $(this).attr('catalog');
      }
      return {
        datasourceName : dsName
      }
    }).get();

    var tables = $(xml).find('table').map(function() {
      var tableName = $(this).attr('name');
	  var datatype;
      var columns = $(this).find('column').map(function() {
        datatype = $(this).find('sqlTypeName').map(function() {
          return $(this).text();
        }).get();
		return $(this).attr('name');
      }).get();

      return {
        name : tableName,
		datatype : datatype,
        columns : columns
      };
    }).get();

    return {
      datasourceName : $(datasource[0]).attr('datasourceName'),
      tables : tables
    };
  };

  // fetch the sql schema from the asio endpoint
  // expect a callback function with two arguments (error, xml)
  // where error is null on success and xml holds the xml response document
  // on failure error will hold an Error object and xml is null
  exports.fetchSchema = function(callback) {
    var target = endpoint() + SERVICE_SCHEMA;
    var req = $.ajax({
      url : target,
      headers : {
        'Accept' : 'application/xml',
        'Accept-Language' : 'en',
        'Accept-Charset' : 'UTF-8',
      },
      dataType : 'xml',
    });
    req.done(forwardTo(callback)).fail(escalateTo(callback));
  };

  // fetch the dataset metadata as JS object
  // expect a callback function (error, json)
  // json will hold the meta data document on success
  exports.fetchMetadata = function(callback) {
    var target = guessRoot() + SERVICE_META;
    var req = $.getJSON(target);
    req.done(forwardTo(callback)).fail(escalateTo(callback));
  }

  // execute the given sql query on the asio endpoint
  // expect a callback function with two arguments (error, xml)
  // where error is null on success and xml holds the xml response document
  // on failure error will hold an Error object and xml is null
  exports.executeQuery = function(query, callback) {
    var pleaseWaitDiv = $('<div class="modal hide fade" id="pleaseWaitDialog" data-backdrop="static" data-keyboard="false"><div class="modal-header"><h1>Loading data. Please wait...</h1></div><div class="modal-body"><div class="progress progress-striped active"><div class="bar" style="width: 100%;"></div></div></div></div>');
    var target = endpoint() + SERVICE_QUERY;
    var req = $.ajax({
      type : 'POST',
      url : target,
      data : query,
	  beforeSend: function () {
		pleaseWaitDiv.modal();
	  },
	  complete:function(){
		pleaseWaitDiv.modal('hide');
		$('#cbcontainer').show();
		$("#downloadbutton").removeAttr("disabled");
	  },
      processData : false,
      contentType : 'application/sql-query',
      headers : {
        'Accept' : 'application/xml',
        'Accept-Language' : 'en',
        'Accept-Charset' : 'UTF-8',
      },
      dataType : 'xml',
    });
	
    req.done(forwardTo(callback)).fail(escalateTo(callback));
  };

  // ==============================================================>
  // EXPORTS
  return exports;
}());