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
  
//==============================================================>
//CONSTANTS

  //relative path from site location to asio sql endpoint
  // change this to adapt to deployment location
  var ENDPOINT_PATH = "/read/sql";
  //service suffixes
  var SERVICE_SCHEMA = "/schema";
  var SERVICE_QUERY = "/";
  
//==============================================================>
//UTILS
  
  // guess sql endpoint url from current location
  // -> strip /static path from URL
  // -> strip file name and/or trailing slash from URL
  // -> append endpoint suffix
  function endpoint() {
   var base = $(location).attr('href').replace(/\/explore/, '').replace(/([^\/]*).html$/, '')
     .replace(/\/$/, '');
   return base + ENDPOINT_PATH;
  };
  
  // forward a success ajax response to a nodejs style callback
  function forwardTo(callback) {
    return function(result) { callback(null, result); }; 
  };
  
  // transform a HTTP status and text message into an Error and 
  // forward it to a nodejs style callback
  function escalateTo(callback) {
    return function(ignored, status, text) { 
      var cause = new Error(status + ":" + text);
      callback(cause, null);
    };
  };
  
//==============================================================>
//TYPES

  //hold result of a sql query
  // [ column_definitions ] + [ row : [ values ] ]
  function SqlResult() {
   this.columns = [];
   this.rows = [];
  };
  SqlResult.prototype.addRow = function (values) {
   this.rows.push({ row : values });
  };
  exports.SqlResult = SqlResult;
  
//==============================================================>
//FUNCTIONS
  // FIXME hacked
  exports.endpoint = function() {
    return endpoint();
  }
  
  //parse a xml webrowset into a SqlResult object
  exports.parseWebrowset = function(xml) {
   var table = new SqlResult();
   // parse column definitions
   table.columns = $(xml).find('column-definition').map(function () {
     return $(this).children('column-label').text();
   }).get();
   // parse each row
   $(xml).find('currentRow').each(function () {
     var values = $(this).children('columnValue').map(function () {
       return $(this).text();
     }).get();
     table.addRow(values);
   });
   return table;
  };
  
  //parse an OGSADAI xml database schema into a list of table names
  exports.parseDatabaseSchema = function (xml) {
   var datasource = $(xml).find('table').map(function () {
	  var dsName = $(this).attr('schema');
	  if(dsName == 'null'){ dsName = $(this).attr('catalog'); }
	  return { datasourceName : dsName }
   }).get();
   
   var tables = $(xml).find('table').map(function () {
     var tableName = $(this).attr('name');
	 //var rowCount = " ( rows: " + Controller.fetchRowCount(tableName) +" )"
	 //alert(fetchRowCount(tableName));
	 
	 
	 var columns = $(this).find('column').map(function() {
		var datatype = $(this).find('sqlTypeName').map(function() {
		  return $(this).text();
		}).get();
	   return $(this).attr('name') + " (" + datatype + ")";
	 }).get(); 
	 
     return { name : tableName, columns : columns};
   }).get();
   
   return { datasourceName : $(datasource[0]).attr('datasourceName'), tables : tables };
  };
  
  // fetch the sql schema from the asio endpoint 
  // expect a callback function with two arguments (error, xml)
  //  where error is null on success and xml holds the xml response document
  //  on failure error will hold an Error object and xml is null
  exports.fetchSchema = function (callback) {
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
  
  // execute the given sql query on the asio endpoint
  // expect a callback function with two arguments (error, xml)
  //  where error is null on success and xml holds the xml response document
  //  on failure error will hold an Error object and xml is null
  exports.executeQuery = function (query, callback) {
   var target = endpoint() + SERVICE_QUERY;
   var req = $.ajax({
     type : 'POST',
     url : target,
     data : query,
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
  
//==============================================================>
// EXPORTS
  return exports;
}());