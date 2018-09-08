package com.iandtop.front.smartpark.pub.utils;

import com.google.gson.Gson;
import com.iandtop.front.smartpark.pub.vo.RequestResultVO;

/**
 * 返回RequestResultVO字符串
 * @author andyzhao
 */
public class ResponseUtil {
    public static String getResponse(Boolean isSuccess,String msg, Object resultData) {
        Gson gson = new Gson();
        RequestResultVO resultVO = new RequestResultVO();
        resultVO.setSuccess(isSuccess);
        resultVO.setMsg(msg);
        resultVO.setResultData(resultData);
        String rs = gson.toJson(resultVO);
        return rs;
    }
}
