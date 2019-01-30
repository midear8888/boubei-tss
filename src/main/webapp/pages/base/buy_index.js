const appid = 2018051160132356,
    MODULE_DEF = "/tss/cloud/modules",
    MODULE_MONEY = "/tss/cloud/order/price/query",
    MODULE_ORDER = '/tss/cloud/order',
    MODULE_ORDER_LIST = '/tss/cloud/order/list',
    ACCOUNT = '/tss/auth/account',
    ORDER_FLOW = '/tss/auth/account/flow',
    SUBAUTHORIZE = '/tss/auth/account/subauthorize',
    SUBAUTHORIZE_ROLE = "/tss/auth/account/subauthorize/role",
    API_USER = "/tss/wx/api/users/id2name",
    OFFLINE_PAYED = "/tss/cloud/order/payed/";

function offline_payed(order_no){
    $.post(OFFLINE_PAYED + order_no, {}, (result)=>{
        console.log(result)
    })
}

function getFormData(formId){
    formId = formId || 'fm';
    if( !formId.indexOf('.') == 0 && !formId.indexOf('#') == 0 ){
        formId = '#' + formId;
    }
    var d = {};
    var t = $(formId).serializeArray();
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
    $.get(MODULE_DEF, {}, function(data){
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
        ModuleOrderHandler : 'E8',//普通产品
        ModuleOrderEFFHandler : 'EFF',//eff产品
    }
    let clazzs = (item.type||'').split('.');
    let clazz = clazzs[clazzs.length-1];
    const h = productMap[clazz] || clazz;
    const t = item.module_id ? moduleMap[item.module_id] : "";
    if( clazz == "RenewalfeeOrderHandler" ){
        return h + t
    }
    return h
}

function createPanel(title, content, funcSubmit){
    $('.panel').remove();
    let $div = $(`
        <div class="panel">
            <div style="position:relative; width:100%; height: 100%;">
                <div class="panel-title">
                    <span class="panel-title-text">` + title + `</span>
                    <div class="panel-title-button">
                        <span> <button class="panel-title-button-close">关闭</button> </span>
                    </div>
                </div>
                <div class="panel-content"></div>
                <div class="panel-footer">
                    <div class="panel-footer-button">
                        <span> <button class="panel-footer-button-submit">提交</button> </span>
                    </div>
                </div>
            </div>
        </div>`);
    $div.find('.panel-content').append(content);
    $div.appendTo('body');
    $('.panel-title-button-close').click((e)=>{
        $div.remove();
    })
    if( funcSubmit ){
        $('.panel-footer-button-submit').click((e)=>{
            funcSubmit($div);
        })
    }else{
        $('.panel-footer-button-submit').remove();
    }
}

//英文值检测
function isEnglish(name) {
    if(name.length == 0)
        return false;
    for(i = 0; i < name.length; i++) {
        if(name.charCodeAt(i) > 128)
            return false;
    }
    return true;
}

// E-mail值检测
function isMail(name){
    if(! isEnglish(name))
        return false;
    i = name.indexOf("@");
    j = name.lastIndexOf("@");
    if(i == -1)
        return false;
    if(i != j)
        return false;
    if(i == name.length)
        return false;

    return true;
}

