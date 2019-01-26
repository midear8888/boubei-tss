const appid = 2018051160132356;

function getFormData(formId){
    formId = formId ||'fm';
    var d = {};
    var t = $("#"+formId).serializeArray();
    $.each(t, function() {
        if(d[this.name]){
            d[this.name] += ',' + this.value;
        }
        else{
            d[this.name] = this.value;
        }     
    });
    return d;
}


function restfulParams(params){
    var queryString = "?";
    $.each(params, function(key, value) {
        if( queryString.length > 1 ) {
            queryString += "&";
        }
        queryString += (key + "=" + value);
    });
    return queryString
}

function formatterNull2Empty(value){
    return value || ''
}


function domToString (node) {  
    let tmpNode = document.createElement('div')
    tmpNode.appendChild(node) 
    let str = tmpNode.innerHTML
    tmpNode = node = null; // 解除引用，以便于垃圾回收  
    return str;  
}

function createElement(obj){
    var dom = document.createElement(obj.dom);
    var text = obj.innerText;
    var html = obj.innerHTML;
    delete obj.dom;
    delete obj.innerText;
    delete obj.innerHTML;
    for(k in obj){
        dom.setAttribute(k, obj[k])
    }
    if(text){
        dom.innerText = text;
    }
    if(html){
        dom.innerHTML = html;
    }
    return dom;
}


/*
* 格式化金额数字
* number：要格式化的数字
* decimals：保留几位小数
* */
function number_format(number, decimals) {
    number = (number + '').replace(/[^0-9+-Ee.]/g, '');
    var n = !isFinite(+number) ? 0 : +number,
        prec = !isFinite(+decimals) ? 0 : Math.abs(decimals),
        sep = ',',
        s = '',
        toFixedFix = function (n, prec) {
            var k = Math.pow(10, prec);
            return '' + Math.ceil(n * k) / k;
        };
 
    s = (prec ? toFixedFix(n, prec) : '' + Math.round(n)).split('.');
    var re = /(-?\d+)(\d{3})/;
    while (re.test(s[0])) {
        s[0] = s[0].replace(re, "$1" + sep + "$2");
    }
 
    if ((s[1] || '').length < prec) {
        s[1] = s[1] || '';
        s[1] += new Array(prec - s[1].length + 1).join('0');
    }
    return s.join('.');
};



Array.prototype.each = function(callback){
    for (var i = 0; i < this.length; i++) {
        callback(i,this[i])
    }
}
Array.prototype.contains = function(value){
    for (var i = 0; i < this.length; i++) {
        if(this[i] == value){
            return true;
        }
    }
    return false;
}

function searchParams(name, decode) {
    let items = {}; // 先清空
    queryString = window.location.search.substring(1);

    var params = queryString.split("&");
    for (var i = 0; i < params.length; i++) {
        var param = params[i].split("=");
        if(param.length == 2) {
            var key = param[0].replace(/%20/g, "");
            items[key] = param[1].trim();
        }
    }
    var str = items[name];
    return decode ? unescape(str) : str; // decode=true，对参数值（可能为中文等）进行编码
}

const moduleMap = {};

function queryModuleDef(callback){
    $.get('/tss/xdata/json/cloud_module_def',{},function(data){
        data.each(function(i,item){
            moduleMap[item.id] = item.module;
        })
        callback && callback()
    })
}

function formatterProduct(item){
    const productMap = {
        RechargeOrderHandler : '充值',
        RenewalfeeOrderHandler : '续费',
        //模块产品购买
        ModuleOrderHandler : '购买',//普通产品
        ModuleOrderE8Handler : 'E8',//e8产品
        ModuleOrderEFFHandler : 'EFF',//eff产品
    }
    const h = productMap[item.type] || item.type;
    const t = item.module_id ? moduleMap[item.module_id] : "";
    //普通产品需带上module_id的中文名
    if( item.type == "ModuleOrderHandler" ){
        return h + t
    }
    return h
}

function createPanel(content){
    $('.panel').remove();
    let $div = $(`
        <div class="panel">
            <div class="panel-title">
                <div class="panel-title-button">
                    <span> <button class="panel-title-button-close">关闭</button> </span>
                </div>
            </div>
        </div>`);
    $div.append(content).appendTo('body');
    $('.panel-title-button-close').click((e)=>{
        $div.remove();
    })
    return $div;
}

