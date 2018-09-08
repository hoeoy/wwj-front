package com.iandtop.front.smartpark.pub.vo;

import java.util.LinkedHashMap;
import java.util.Map;

public class RequestResultVO {
    private Boolean success ;
    private String msg;
    private String detailMessage;
    private String statusCode;
    private Object resultData;
    private String resultDataType;
    private Boolean debug;

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getDetailMessage() {
        return detailMessage;
    }

    public void setDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public Object getResultData() {
        return resultData;
    }

    public void setResultData(Object resultData) {
        this.resultData = resultData;
    }

    public String getResultDataType() {
        return resultDataType;
    }

    public void setResultDataType(String resultDataType) {
        this.resultDataType = resultDataType;
    }

    public Boolean getDebug() {
        return debug;
    }

    public void setDebug(Boolean debug) {
        this.debug = debug;
    }

    public Map<String, Object> toMapValue()
    {
        Map<String, Object> map = new LinkedHashMap<String, Object>();

        map.put("success", success);
        map.put("msg", msg);
        map.put("detailMessage", detailMessage);
        map.put("statusCode", statusCode);
        map.put("resultData", resultData);
        map.put("resultDataType", resultDataType);

        return map;
    }


}