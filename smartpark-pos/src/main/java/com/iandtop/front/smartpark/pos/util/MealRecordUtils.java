package com.iandtop.front.smartpark.pos.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MealRecordUtils {

	/**
	 * @author 吕召
	 * 获取指定时间对应的表明
	 */
	public static String getMealRecordName(Long timeMillis){

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Calendar calendar = Calendar.getInstance();

		String date = sdf.format(new Date(timeMillis));
		String year = date.substring(0,4);
		String month = date.substring(5,7);
		String day = date.substring(8);

		day = Integer.parseInt(day) < 15 ? "01" : "15";

		//String tableName = "MEAL_RECORD_" + year + "_" + month + "_" + day;
		String tableName = "meal_record_" + year + "_" + month + "_" + day;
		return tableName;

	}

	/**
	 * 获取指定时间到当前时间段内所有的表名
	 */
	public static List<String> getMealRecordNames(String time){

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DATE, calendar.getActualMaximum(Calendar.DATE));
		String lastMonthDay = sdf.format(calendar.getTime());
		lastMonthDay = lastMonthDay.substring(0,10) + " 23:59:59";

		long monthLastTime = 0;
		long paraTime = 0;

		try {
			monthLastTime = sdf.parse(lastMonthDay).getTime();
		} catch (ParseException e1) {
			e1.printStackTrace();
		}

		List<String> tableNames = new ArrayList<String>();

		String time1 = time.substring(0,4);
		String time2 = time.substring(5,7);
		String time3 = time.substring(8,10);

		int year = Integer.parseInt(time1);
		int month = Integer.parseInt(time2);
		int day = Integer.parseInt(time3);

		String tableDay = "";
		Boolean leftMonth = (day<15);

		while(true){
			tableDay = leftMonth?"01":"15" ;
			String tableName = "MEAL_RECORD_"+year+"_"+((month<10)?("0"+month):month)+"_"+tableDay;
			tableNames.add(tableName.toUpperCase());
			leftMonth = !leftMonth;
			if(tableDay.equals("15")){
				month++;
			}
			if(month>=13){
				month = 1;
				year++;
			}


			try {
				paraTime = sdf.parse(year + "-" + month + "-" + tableDay + " 00:00:00").getTime();
			} catch (ParseException e) {
				e.printStackTrace();
			}
			if(paraTime > monthLastTime){
				break;
			}

		}

		return tableNames;
	}

	public static List<String> getTableNamesBetween(Date start_date,Date end_date) throws ParseException {
		List<String> tableNames = new ArrayList<String>();

		String startDate = DateUtils.formatDate(start_date);
		String endDate = DateUtils.formatDate(end_date);

		Calendar cal = Calendar.getInstance();

		String tableName = "";
		int daysIndex = DateUtils.daysBetween(startDate, endDate);
		if(daysIndex == 0){
			tableName = getMealRecordName(DateUtils.parseDate(endDate).getTime());
			tableNames.add(tableName);
		}else{
			for (int i = 0; i < daysIndex; i++) {
				cal.setTime(start_date);
				tableName = getMealRecordName(cal.getTimeInMillis());
				if(!tableNames.contains(tableName)){
					tableNames.add(tableName);
				}
				start_date = DateUtils.getNDaysLaterDate(start_date, 1);
			}
		}

		return tableNames;
	}

	public static List<String> getCurrentMonthTableNames(){
		//yyyy-MM-dd HH:mm:ss
		String TS = DateUtils.currentDatetime();
		String year = TS.substring(0,4);
		String month = TS.substring(5,7);

		List<String> tableNames = new ArrayList<String>();

		tableNames.add("MEAL_RECORD_"+year+"_"+month+"_01");
		tableNames.add("MEAL_RECORD_"+year+"_"+month+"_15");

		return tableNames;
	}
	
}
