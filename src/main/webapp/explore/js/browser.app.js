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
    $('#sql-result').empty().append(content);
	$('#zebra-table').dataTable({
        aoColumns: columnWidths()
    });
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
	  
	  /*******TODO**************/
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
	if($("#cbinput").prop('checked'))
		this.executeQuery("SELECT * FROM `" + table +"` LIMIT 1000");
	else
		this.executeQuery("SELECT * FROM `" + table +"`");
  };
  
  this.fetchTableField = function (table, field, limit) {
	if($("#cbinput").prop('checked'))
		this.executeQuery("SELECT DISTINCT COUNT(`" + field + "`) AS occurence, `" + field +"` FROM `" + table +"` GROUP BY `"+ field +"` ORDER BY occurence DESC LIMIT 1000");
	else
		this.executeQuery("SELECT DISTINCT COUNT(`" + field + "`) AS occurence, `" + field +"` FROM `" + table +"` GROUP BY `"+ field +"` ORDER BY occurence DESC");
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
  
  this.executeQuery = function (query) {
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

/*function onSelectLimit(){ // set limit according to select box 'limitselect'
	var query = $('#sql-command').val();
	//edit query to set limit
	//+6 due to the key word limit and the following space
	query = query.substring(0,query.toUpperCase().lastIndexOf("LIMIT")+6) + $("#limitselect").val();
    Controller.executeQuery(query);
};*/

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
	/*$('#sql-download-form').submit(function(event){ //listen for submit event
		$('#sql-download-command').val($('#sql-command').val());
		$('#sql-download-form').attr('action', asio.endpoint());
		return true;
    });*/
	return Controller.downloadTable();
}; 

function printMetadata()
{
	asio.fetchMetadata(callback_Metadata);
	function callback_Metadata(error, data)
	{
		if (error != 0)
		{
			var daten = document.getElementById("daten");
			var table = document.createElement("table");
			table.style.width = '100%';
			var tr = new Array();
			var j = 0;
			$.each(data.dataset, function(key, value){
				tr[j] = document.createElement("tr");
				tr[j].style.align="justify";
				var td = document.createElement("td");
				td.style.width = '50%';
				td.style.display="left";
				td.style.fontWeight="500";
				if (j % 2 == 0)
					td.style.background = "#E3E7FF";
				var td2 = document.createElement("td");
				td2.style.width = '100%';
				td2.style.display="inline-block";
				if (j % 2 == 0)
					td2.style.background = "#E3E7FF";
				var text = document.createTextNode(key);
				var text2 = document.createTextNode(value);
				td.appendChild(text);
				td2.appendChild(text2);
				tr[j].appendChild(td);
				tr[j].appendChild(td2);
				table.appendChild(tr[j]); 
				daten.appendChild(table);
				j++;
			
			});
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

//==============================================================>
// BOOTSTRAPPING
function main() {
  Controller.sync();
  overrideForm();
};

$(document).ready(main);
