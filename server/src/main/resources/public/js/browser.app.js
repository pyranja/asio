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
	$(".collapsibleList").collapsibleList();
  };
};

var ResultView = new function () {
  this.update = function (model) {
    var content = ich.template_result(model.result);
	var fieldArray = new Object();
	var andor = ($("#zebra-table input[type=checkbox]").prop('checked'));	
	$("#zebra-table input[type=text]").each(function(){
		fieldArray[$(this).attr('id')] = $(this).val();
	});
	
	$('#sql-result').empty().append(content);	
	$('#zebra-table').dataTable({
        aoColumns: columnWidths()
    });
	$('#zebra-table > tbody  > tr').each(function() {
		$(this).linkify();
	});
	
	addColumnFilterUI();
	for (var key in fieldArray) {
		$("#"+key).val(fieldArray[key]);
	}
	$("#zebra-table input[type=checkbox]").prop('checked', andor);
	setAndOrOperator();
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
  
  this.fetchTable = function (table) {
    if($("#cbinput").prop('checked'))
		this.executeQuery("SELECT * FROM `" + table +"` LIMIT 1000");
	else
		this.executeQuery("SELECT * FROM `" + table +"`");
	
	$("#selectedTable").val(table);
  };
  
  this.fetchTableField = function (table, field) {
	if($("#cbinput").prop('checked'))
		this.executeQuery("SELECT DISTINCT COUNT(`" + field + "`) AS occurence, `" + field +"` FROM `" + table +"` GROUP BY `"+ field +"` ORDER BY occurence DESC LIMIT 1000");
	else
		this.executeQuery("SELECT DISTINCT COUNT(`" + field + "`) AS occurence, `" + field +"` FROM `" + table +"` GROUP BY `"+ field +"` ORDER BY occurence DESC");
	
	$("#selectedTable").val(table);
  };
  
  this.onCheckLimitBox = function () {
	var isChecked = $("#cbinput").prop('checked');
	if(!isChecked){
	    $.confirm({
          text: "<span style='font-size: 20px;'>Are you sure you want to display all data? <br/> The processing of large datasets may take a while!</span>",
          confirm: function() {
            var query = $('#sql-command').val();
			if(query.toUpperCase().lastIndexOf("LIMIT") != -1)
				query = query.substring(0,query.toUpperCase().lastIndexOf("LIMIT"));
			
			Controller.executeQuery(query);
          },
          cancel: function() {
            
          }
        });
	} else {
		var query = $('#sql-command').val();
		if(query.toUpperCase().lastIndexOf("LIMIT") == -1)
			query = query + " LIMIT 1000";
		
		Controller.executeQuery(query);
	}
  }
  
  this.executeQuery = function (query, callback) {
    var $self = this;
    $self.state.command = query;
    asio.executeQuery(query, function (error, xml) {
      if (xml) {
        var result = asio.parseWebrowset(xml);
        $self.state.result = result;
		$self.views.commander.update($self.state);
		$self.views.result.update($self.state);
        //$self._publish();
      } else {
        alert(error);
      }
    }); 
  };
  
  this.downloadTable = function() {
	var req = $('#sql-download-form').submit(function(event){
		$('#sql-download-command').val($('#sql-command').val());
		$('#sql-download-form').attr('action', asio.endpoint());
		return true;
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

function onCheckLimitBox() {
  Controller.onCheckLimitBox();
};

function onFilterByColumns(table) {
	var fieldArray = new Object();
	$($("#zebra-table thead tr th").get().reverse()).each(function(){
		if($("#f"+$(this).text()).val() != "")
			fieldArray[$(this).text()] = $("#f"+$(this).text()).val();
	});
	
	var query = "SELECT * FROM "+$("#selectedTable").val()+" WHERE ";
	var index = 0;
	for (var key in fieldArray) {
		if(index < 1)
			query = query + key +" LIKE '%"+fieldArray[key]+"%'";
		else
			query = query + " "+$("#filterOperator").val()+" " +  key +" LIKE '%"+fieldArray[key]+"%'";
		
		index++;
	};
	query = query + ";"
	
	if(index > 0){
		Controller.executeQuery(query);
	}
};

function onSelectField(event, fieldName, limit) {
  event.preventDefault();
  event.stopPropagation();
  Controller.fetchTableField($(event.target).parent().parent().parent().attr("xasiotablename"), fieldName, limit);
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
		$('#sql-download-command').val($('#sql-command').val());
		$('#sql-download-form').attr('action', asio.endpoint());
		return true;
    });
	return Controller.downloadTable();
}; 

function printMetadata()
{
  var INCLUDED_PROPERTIES = ['identifier', 'label', 'description', 'author', 'license', 'category', 'created', 'updated'];
  asio.fetchMetadata(callback_Metadata);
  function callback_Metadata(error, data)
  {
    if (error != 0)
		{
      var table = document.getElementById("meta-table");
      $.each(data, function(key, value){
        // omit all properties not explicitly included (negative index means not found)
        // omit properties with null or empty value
        if ($.inArray(key, INCLUDED_PROPERTIES) < 0 || !value) {
          return;
        }

        var row = document.createElement("tr");
				row.style.align="justify";
        table.appendChild(row);

        var propertyNameCell = document.createElement("td");
        propertyNameCell.style.width = '50%';
        propertyNameCell.style.display="left";
        propertyNameCell.style.fontWeight="500";
        propertyNameCell.appendChild(document.createTextNode(key));
        row.appendChild(propertyNameCell);

        var propertyValueCell = document.createElement("td");
        propertyValueCell.style.width = '100%';
        propertyValueCell.style.display="inline-block";
        propertyValueCell.appendChild(document.createTextNode(value));
				row.appendChild(propertyValueCell);
      });
      container.appendChild(table);
    }
		else
		{
			alert("Error: printMetadata");
		}
	}
}

//create automatic widths for datatable columns
function columnWidths() {
    var ao = [];
    $("#zebra-table th").each(function(i) {
        switch (i) {
            case 0 : 
                ao.push({"sWidth": "15%"});
                break;
            default :
                ao.push({"sWidth": "auto"});
                break;
        }
    });
    return ao;
}

function addColumnFilterUI(){
	var buttonCell = document.getElementById("zebra-table").createTFoot().insertRow(0).insertCell(0);
	buttonCell.colSpan = 999;
	buttonCell.innerHTML = '<table><tr><td id="andoropcell"><input id="andor" type="checkbox" name="andor" value="limit" onchange="setAndOrOperator()" checked> OR operator</td><td><button id="columnfilter" title="Column filter" type="submit" class="btn btn-default btn-lg" onclick="onFilterByColumns();" value="Column filter"><span class="glyphicon glyphicon-download"></span> Column filter</button></td><td><button title="Reset table" type="submit" class="btn btn-default btn-lg" onclick="resetTable();" value="Reset table"><span class="glyphicon glyphicon-download"></span> Reset table</button></td></tr></table>';
	
	var searchRow = document.getElementById("zebra-table").tFoot.insertRow(0);	
	$($("#zebra-table thead tr th").get().reverse()).each(function(){	
		var cell = searchRow.insertCell(0);
		cell.innerHTML = '<input style="width:90%" type="text" class="form-control" id="f'+$(this).text()+'" placeholder="'+$(this).text()+'" value="">';
	});
}

function setAndOrOperator(){
	var isChecked = $("#andor").prop('checked');
	if(isChecked) {
		$("#andoropcell").html('<input id="andor" type="checkbox" name="andor" onchange="setAndOrOperator()" checked> OR operator');
		$("#filterOperator").val("OR");
	} else {
		$("#andoropcell").html('<input id="andor" type="checkbox" name="andor" onchange="setAndOrOperator()"> AND operator');
		$("#filterOperator").val("AND");
	}
}

function resetTable() {
	document.getElementById("zebra-table").deleteTFoot();
	Controller.fetchTable($("#selectedTable").val());
}

//==============================================================>
// BOOTSTRAPPING
function main() {
  Controller.sync();
  overrideForm();
};

$(document).ready(main);
