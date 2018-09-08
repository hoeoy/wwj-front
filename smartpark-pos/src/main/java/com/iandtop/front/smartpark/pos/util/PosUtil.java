package com.iandtop.front.smartpark.pos.util;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author andyzhao
 */
public class PosUtil {

    public static String getCurrentTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");//设置日期格式
        return df.format(new Date());
    }

    /**
     * 判断source是否大于当前时间
     * @param source
     * @return
     */
    public static Boolean sourceBiggerThanCurrent(String source) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();
        try {
            c1.setTime(df.parse(source));
        } catch (java.text.ParseException e) {
            Logger logger = LoggerFactory.getLogger(PosUtil.class);
            logger.error("日期格式不正确",e);
        }
        int result = c1.compareTo(c2);
        if (result == 0) {
            return false;
        } else if (result < 0) {
            return false;
        }else{
            return true;
        }
    }

    /**
     *判断某个时间是否在时间段内  格式HH:mm:ss
     * @param start_time 开始时间
     * @param end_time  结束时间
     * @param src   目标时间
     * @return
     */
    public static boolean between(String start_time,String end_time,String src){
        long startime = strToTime(start_time);
        long endtime = strToTime(end_time);
        long srctime = strToTime(src);

        return srctime <= endtime && srctime>= startime;

    }

    /**
     * 将字符串的时间转换成秒long类型
     * HH:mm:ss ---> long
     */
    public static long strToTime(String time){

        long hour = Long.parseLong(time.substring(0,2)) * 60 * 60;
        long minute = Long.parseLong(time.substring(3,5)) * 60;
        long second = Long.parseLong(time.substring(6,8));

        return hour+minute+second;
    }


    public static String getCurrentTimeWithoutDate(){
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");//设置日期格式
        return df.format(new Date());
    }
    public static String getCurrentDate(){
        String temp_str="";
        Date dt = new Date();
        //最后的aa表示“上午”或“下午”    HH表示24小时制    如果换成hh表示12小时制
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        temp_str=sdf.format(dt);
        return temp_str;
    }
}
