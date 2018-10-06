<!DOCTYPE html>
<html>
<head>
	<meta charset="UTF-8">
	<title>${recordName}</title>
	
	<link rel="stylesheet" href="../../tools/tssJS/css/boubei.css">
	<link rel="stylesheet" href="../../css/easyui.css">
	
	<link rel="stylesheet" href="../../tools/easyui/themes/default/easyui.css">
	<link rel="stylesheet" href="../../tools/easyui/themes/icon.css">
	
	<script src="../../tools/tssJS/tssJS.all.js"></script>
	
	<script src="../../tools/easyui/jquery.min.js"></script>
	<script src="../../tools/easyui/jquery.easyui.min.js"></script>
	<script src="../../tools/easyui/easyui-lang-zh_CN.js"></script>
	
	<script src="../../tools/easyui.js"></script>
	<script src="../../pages/_js/scm.js"></script>
	<style type="text/css">
		a{
			color:blue;
		}
	</style>

	<script type="text/javascript">

		var fColumn = "${recordFile}";
		var URL = RECORD.${recordId}.URL , table = RECORD.${recordId}.table;
		
		$(function () {
			query();
		});
		
		function create() {
		    openDialog('新增', true);
		    $('#code').textbox('readonly', false);
		    if(fColumn != 'false'){
		        getAttachShow(table,fColumn);
		    }
		}
		
		function update() {
		    var row = getSelectedRow();
		    if (row) {
		        $.getJSON(URL.GET + row.id, {}, function (data) {
		            if(fColumn != 'false'){
		                getAttachShow(table,fColumn,data,'管理证件');
		            }
		            openDialog('修改');
		            $('#fm').form('load', data);
		            $('#code').textbox('readonly', true);
		        }, "GET");
		    }
		}
		
		var FIELDS = ${gridFileds};
		
		function query(params) {
		    params = params || {};
		
		    $('#t1').datagrid({
		        url: URL.QUERY,
		        queryParams: params,
		        fit: true,
		        fitColumns: true,
		        pagination: true,
		        rownumbers: true,
		        pageSize : 30,
		        singleSelect: true,
		        checkOnSelect: true,
		        selectOnCheck: true,
		        toolbar: [ 
		            { text: '新增', iconCls: 'icon-add', handler: create }, 
		            '-', { text: '修改', iconCls: 'icon-edit', handler: update }, 
		            '-', { text: '删除', iconCls : 'icon-remove', handler : _remove }, 
		            '-', { text: '查询', iconCls : 'icon-search', handler : openQueryForm }, 
		            '-', { text: 'Excel模板', handler : getImportTL, id: "btn4" }, 
		            '-', { text: 'Excel导入', iconCls : 'icon-redo', handler : batchImport, id: "btn5" }, 
		            '-', { text: '导出数据', iconCls : 'icon-tss-down', handler : backup, id: "btn6" }
		        ] ,
		        columns: [FIELDS],
		        /* 返回的结果再处理 */
		        loadFilter: function (data) {
		            $.each(data, function (i, item) {                 
		                // 此处可对数据进行预处理，然后再展示到Grid里
		                
		            });
		
		            return data;
		        },
		        onSelect: function(index, row) {
		            $('#btn3').linkbutton("enable");
		        }
		    });
		}
		
		function _remove(){
		    doRemove("t1", table);
		}
		
		var params = {}, 
			paramFields = [${paramFields}];  
			
		function beginQuery() {
			paramFields.each(function(i, item) {
				params[item.replace('query_','')] = $('#' + item).val();
			});
		    
		    query(params);
		    $("#dlg_query").dialog('close');
		}
		
		function openQueryForm() {
			var title = '查询' + rname;
			$('#dlg_query').dialog( {"modal": true} ).dialog('open').dialog('setTitle', title).dialog('center');
		}
		
		function backup() {
		    params.page = 1;
		    params.pagesize = 50000;
		        
		    var queryString = "?";
		    tssJS.each(params, function(key, value) {
		        if( queryString.length > 1 ) {
		            queryString += "&";
		        }
		        queryString += (key + "=" + value);
		    });
		
		    tssJS("#downloadFrame").attr( "src", encodeURI(URL.CSV_EXP + queryString));
		}
		
		function getImportTL() {
		    tssJS("#downloadFrame").attr( "src", encodeURI(URL.CSV_TL) );
		}
		
		function batchImport() {
		    function checkFileWrong(subfix) {
		        return subfix != ".csv";
		    }
		    var importDiv = createImportDiv("请点击图标选择CSV文件导入", checkFileWrong, URL.CSV_IMP);
		    tssJS(importDiv).show();
		}
		
		function saveTable(){
		    if(fColumn != 'false'){
		        containAttachSave(table,fColumn);
		    }
		    else{
		        save(table);
		    }
		}

	</script>

</head>

<body>
	<div id="main" class="easyui-layout" fit="true">
	    <div id="dataContainer" data-options="region:'center'" border="false" title="${recordName}">
	        <table id="t1" border="false"></table>
	    </div>
	</div>
	
	<div id="dlg" class="easyui-dialog" closed="true" buttons="#dlg-buttons">
	    <form id="fm" method="post" novalidate>
	        <input name="id" type="hidden"/>
	        <input name="version" type="hidden"/>
	        ${recordForm}
	    </form>
	</div>
	<div id="dlg-buttons">
	    <a href="#" class="easyui-linkbutton" iconCls="icon-ok" onclick="saveTable()">保 存</a>
	    <a href="#" class="easyui-linkbutton" iconCls="icon-cancel" onclick="closeDialog()">取 消</a>
	</div>
	
	<div id="dlg_query" class="easyui-dialog" closed="true" buttons="#dlg_query-buttons">
	    ${queryForm}
	</div>
	<div id="dlg_query-buttons">
	    <a href="#" class="easyui-linkbutton" iconCls="icon-search" onclick="beginQuery()">开始查询</a>
	</div>
	
	<iframe id="downloadFrame" style="display:none"></iframe>
</body>
</html>