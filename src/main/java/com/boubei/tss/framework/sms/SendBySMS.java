package com.boubei.tss.framework.sms;

import com.boubei.tss.um.helper.dto.OperatorDTO;
import com.boubei.tss.util.EasyUtils;


public class SendBySMS extends SendChannel {

    protected void send() {

        String phone = (String) params.get("phone");
        String tlCode = (String) params.get("tlCode");
        String tlParam = (String) params.get("tlParam");
        String outId = (String) params.get("outId");

        String domain = (String) params.get("domain");
        String smsSign = (String) params.get("sms_sign");
        String smsKey = (String) params.get("sms_key");
        String smsSecret = (String) params.get("sms_secret");

        if (phone != null) {
            AbstractSMS.create(domain, smsKey, smsSecret, smsSign).send(phone, tlCode, tlParam, outId);
        } else {
            for (String receiverId : receiverIds) {
                Long _receiverId = EasyUtils.obj2Long(receiverId);
                OperatorDTO dto = loginService.getOperatorDTOByID(_receiverId);
                phone = (String) EasyUtils.checkNull(dto.getAttribute("telephone"), dto.getLoginName());

                AbstractSMS.create().send(phone, tlCode, tlParam, outId);
            }
        }

    }

}
