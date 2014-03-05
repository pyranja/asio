/*
 * sql browser views
 * 
 * pyranja, 2014
 */

//==============================================================>
// VIEW DEFINITIONS
var CommanderView = new function () {
  this.update = function (model) {
    $('#sql-command').val(model.command);
  };
};

var ExplorerView = new function () {
  this.update = function (model) {
    var content = ich.template_explorer(model.schema);
    $('#sql-explorer').empty().append(content);
  };
};

var ResultView = new function () {
  this.update = function (model) {
    var content = ich.template_result(model.result);
    $('#sql-result').empty().append(content);
  };
};

//==============================================================>
// CONTROLLER
var Controller = new function () {
  this.views = {
      explorer : ExplorerView,
      commander : CommanderView,
      result : ResultView,
    };
  this.state = {
        command : "",
        schema : null,
        result : null,
      };
	  
	  /*********************/
  this.fetchRowCount = function (table) {
    var query = "SELECT COUNT(*) FROM `" + table +"`";
	asio.executeQuery(query, function (error, xml) {
      if (xml) {
        var result = asio.parseWebrowset(xml);
		return result.rows[0].row;
	  } else {
        alert(error);
      }
    });
  };  
  
  this.fetchTable = function (table, limit) {
    this.executeQuery("SELECT * FROM `" + table +"` LIMIT "+limit);
  };
  
  this.fetchTableField = function (table, field, limit) {
	//-1 due to space
	field = field.substring(0, field.lastIndexOf("(")-1); 
	this.executeQuery("SELECT DISTINCT COUNT(`" + field + "`) AS occurence, `" + field +"` FROM `" + table +"` GROUP BY `"+ field +"` ORDER BY occurence DESC LIMIT "+limit);
  };
  
  this.executeQuery = function (query) {
    var $self = this;
    $self.state.command = query;
    asio.executeQuery(query, function (error, xml) {
      if (xml) {
        var result = asio.parseWebrowset(xml);
        $self.state.result = result;
        $self._publish();
      } else {
        alert(error);
      }
    }); 
  };
  
  this.sync = function () {
    var $self = this;
    asio.fetchSchema(function (error, xml) {
      if (xml) {
        var schema = asio.parseDatabaseSchema(xml);
        $self.state.schema = schema;
        $self._publish();
      } else {
        alert(error);
      }
    });
  };
  
  // publish model changes to all views
  this._publish = function () {
    var $self = this;
    $.each($self.views, function (idx, view) { view.update($self.state); });
  }
};

//==============================================================>
// ACTION HOOKS
function onSelectTable(event, tableName, limit) {
  event.preventDefault();
  event.stopPropagation();
  Controller.fetchTable(tableName, limit);
};

function onSelectLimit(){ // set limit according to select box
	var query = $('#sql-command').val();
	//edit query to set limit
	//+6 due to the key word limit and the following space
	query = query.substring(0,query.toUpperCase().lastIndexOf("LIMIT")+6) + $("#limitselect").val();
    Controller.executeQuery(query);
};

function onSelectField(event, fieldName, limit) {
  event.preventDefault();
  event.stopPropagation();
  Controller.fetchTableField($(event.target).parent().parent().attr("xasiotablename"), fieldName, limit);
};

function overrideForm() {
  // hook into form submission
  var form = $('#sql-command-form');
  form.submit(function (event) {
    event.preventDefault();
    var query = $('#sql-command').val();
    Controller.executeQuery(query);
  });
}; 

function downloadTable() {
	$('#sql-download-form').submit(function(event){ //listen for submit event
		alert($('#sql-command').val());
	
		$('#sql-download-command').val($('#sql-command').val())
		console.log($('#sql-download-command').val());
		return true;
		
    });
}; 

//makes sql-explorer list collapsable
function prepareDBList() {
	$('#dblist').find('li:has(ul)').unbind('click').click(function(event) {
		if(this == event.target) {
			$(this).toggleClass('expanded');
			$(this).children('ul').toggle('medium');
		}
		return false;
	}).addClass('collapsed').removeClass('expanded').children('ul').hide();
 
	//Hack to add links inside the cv
	$('#dblist a').unbind('click').click(function() {
		window.open($(this).attr('href'));
		return false;
	});
	//Create the button functionality
	/*$('#expandList').unbind('click').click(function() {
		$('.collapsed').addClass('expanded');
		$('.collapsed').children().show('medium');
	})
	$('#collapseList').unbind('click').click(function() {
		$('.collapsed').removeClass('expanded');
		$('.collapsed').children().hide('medium');
	})*/
}

//==============================================================>
// BOOTSTRAPPING
function main() {
  Controller.sync();
  overrideForm();
  prepareDBList();
};

$(document).ready(main);