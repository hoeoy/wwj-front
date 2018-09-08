package com.iandtop.front.smartpark.pos.action;

import com.iandtop.common.utils.BinaryUtil;
import com.iandtop.common.utils.DateUtils;
import com.iandtop.front.smartpark.pos.dao.PosDao;
import com.iandtop.front.smartpark.pos.util.PosUtil;
import com.iandtop.front.smartpark.pos.vo.HandleMealMoneyVO;
import com.iandtop.front.smartpark.pos.vo.PosMessage;
import com.iandtop.front.smartpark.pos.vo.ServerMessage;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.List;

/**
 * Created by Administrator on 2016-11-30.
 */
public class MealMoneyAction extends BaseAction implements IPosAction {

    private static MealMoneyAction instance = null;

    private MealMoneyAction() {

    }

    public static MealMoneyAction getInstance() {
        if (instance == null) {
            instance = new MealMoneyAction();
        }
        return instance;
    }

    @Override
    public void handle(PosMessage msg, Vertx vertx, Handler<ServerMessage> posServerMessageHandler) {
        ServerMessage posServerMessage = getPOSServerMessage(msg, new byte[]{0x38, 0x00});

        HandleMealMoneyVO handleMealMoneyVO = new HandleMealMoneyVO();

        handleMealMoneyVO.setMeal_type(HandleMealMoneyVO.Meal_Type_Normal);
        handleMealMoneyVO.setCardCode(getCardCode(msg) + "");
        handleMealMoneyVO.setMealMoney(BinaryUtil.byteToIntLowInF(msg.getMealMoney()));//消费金额
        handleMealMoneyVO.setPosServerMessage(posServerMessage);
        String deviceCode = getDeviceCode(msg) + "";

        PosDao posDao = new PosDao();

        posDao.findCard(vertx, handleMealMoneyVO.getCardCode(), resultSet -> {
            List<JsonObject> cards = resultSet.getRows();
            if(cards != null && cards.size() > 0){
                String psnname = cards.get(0).getString("psnname");
                posServerMessage.setName(setNameBytes(psnname));
                
                String strPkCard = cards.get(0).getLong("pk_card").toString();
                handleMealMoneyVO.setPsncode(cards.get(0).getString("psncode"));
                handleMealMoneyVO.setPsnname(psnname);
                handleMealMoneyVO.setPk_meal_rule(cards.get(0).getLong("pk_meal_rule").toString());
                handleMealMoneyVO.setPk_card(strPkCard);
                handleMealMoneyVO.setPk_psnbasdoc(cards.get(0).getLong("pk_staff").toString());
                handleMealMoneyVO.setCard_ineffectived_date(cards.get(0).getString("card_ineffectived_ts"));//失效时间
                handleMealMoneyVO.setLast_money_cash((cards.get(0).getInteger("money_cash")));//现金钱包
                handleMealMoneyVO.setLast_money_corp_grant((cards.get(0).getInteger("money_allowance")));//补贴钱包
                handleMealMoneyVO.setCard_state(cards.get(0).getString("card_state"));
                handleMealMoneyVO.setPwd_for_beyond_quota("");
                handleMealMoneyVO.setSerial(cards.get(0).getInteger("serial"));

               /**
                * 根据时间段限制及设备类型限制卡消费
                * 仅限制餐厅消费，`device_type` bigint(20) DEFAULT NULL COMMENT '1.餐厅终端2.咖啡厅终端3.网上超市终端'
                * 判断当前消费，属于那个餐别： `dining_code` varchar(64) DEFAULT NULL COMMENT '餐次餐别编码',
                * 根据卡表中def1的限制规则，做限制处理 `def1` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
                */             
                posDao.findLimitCardbyCardIdAndDeviceCode(vertx, strPkCard, deviceCode, limitCardsSet->{
                	List<JsonObject> cardList = limitCardsSet.getRows();
					if (cardList != null && cardList.size() > 0) {// 无消费权限
						handleMealMoneyVO.setState(new byte[] { 0x01 });
						handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "没有消费权限");
					} else {// 有消费权限
                		  //判断是否合法卡
                         if("10".equals(handleMealMoneyVO.getCard_state()) && PosUtil.sourceBiggerThanCurrent(handleMealMoneyVO.getCard_ineffectived_date())){
                             posDao.getDeviceInfo(vertx, deviceCode, deviceRule -> {
                                 List<JsonObject> devices = deviceRule.getRows();
                                 if (devices != null && devices.size() > 0) {
                                     handleMealMoneyVO.setPk_device(devices.get(0).getLong("pk_device").toString());
                                     handleMealMoneyVO.setDevice_code(devices.get(0).getString("device_code"));
                                     handleMealMoneyVO.setDevice_meal_type(Integer.parseInt(devices.get(0).getString("device_meal_type")));//设备消费类型
                                     handleMealMoneyVO.setBe_control_time(devices.get(0).getString("be_control_time"));

                                         if(handleMealMoneyVO.getPk_meal_rule() != null){
                                             //判断个人消费规则
                                             posDao.mealPsnRule(vertx,handleMealMoneyVO.getPk_meal_rule(),resultSetpsnRule -> {
                                                 List<JsonObject> psnRule = resultSetpsnRule.getRows();
                                                 if(psnRule != null && psnRule.size() > 0){
                                                     Integer money_day = psnRule.get(0).getInteger("money_day");

                                                     //根据卡pk获取当天的消费总额
                                                     posDao.mealDayFreqAndMoney(vertx,handleMealMoneyVO.getPk_card(),resultSetRecord -> {
                                                         List<JsonObject> psnMealRecord = resultSetRecord.getRows();
                                                         Integer money = psnMealRecord.get(0).getInteger("money") == null ? 0 :psnMealRecord.get(0).getInteger("money");

                                                         //今日消费金额小于日限额
                                                         if(money < money_day){

                                                                 //判断设备是否启用段控制
                                                                 if("Y".equals(handleMealMoneyVO.getBe_control_time())){

                                                                     posDao.mealDeviceRule(vertx,deviceCode,resultSet1 -> {
                                                                         List<JsonObject> reuslt = resultSet1.getRows();
                                                                         if(reuslt != null && reuslt.size() > 0){

                                                                             String time_begin = null;
                                                                             String time_end = null;
                                                                             String currentTime = DateUtils.currentTime();
                                                                             boolean beInDeviceSegment = false;

                                                                             final Integer[] frequency_time = new Integer[1];

                                                                             //判断当前时间是否在设备规则的时间段内
                                                                             for (int i = 0; i < reuslt.size(); i++){
                                                                                 time_begin = reuslt.get(i).getString("start_time");
                                                                                 time_end = reuslt.get(i).getString("end_time");
                                                                                 if(DateUtils.betweenTime(time_begin,time_end,currentTime)){
                                                                                     beInDeviceSegment = true;
                                                                                     frequency_time[0] = reuslt.get(i).getInteger("frequency_time");
                                                                                     break;
                                                                                 }
                                                                             }
                                                                             //判断是否超过段限次
                                                                             if(beInDeviceSegment){
                                                                                 posDao.mealTimeFreqAndMoney(vertx,handleMealMoneyVO.getPk_card(),time_begin,time_end,"0",timeFreqAndMoney->{
                                                                                     List<JsonObject> psnSegRecord = timeFreqAndMoney.getRows();
                                                                                     Integer segFrequency = psnSegRecord.get(0).getInteger("frequency") == null ? 0 : psnSegRecord.get(0).getInteger("frequency");

                                                                                     if(frequency_time[0] != null && frequency_time[0] > segFrequency){
                                                                                         handleMealMoneyVO.setMeal_kind("0");
                                                                                         handleMealMoneyVO.setReal_mealMoney(handleMealMoneyVO.getMealMoney());
                                                                                         mealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao);
                                                                                     }else{
                                                                                         handleMealMoneyVO.setState(new byte[]{0x07});
                                                                                         handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "超过段限次");
                                                                                     }
                                                                                 });
                                                                             }else{
                                                                                 handleMealMoneyVO.setState(new byte[]{0x0c});
                                                                                 handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "不在时间段内");
                                                                             }
                                                                         }else{
                                                                             handleMealMoneyVO.setState(new byte[]{0x05});
                                                                             handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "未获取到设备规则相关信息");
                                                                         }
                                                                     });
                                                                 }else{
                                                                     //直接消费
                                                                     handleMealMoneyVO.setReal_mealMoney(handleMealMoneyVO.getMealMoney());
                                                                     mealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao);
                                                                 }

                                                         }else{
                                                             handleMealMoneyVO.setState(new byte[]{0x02});
                                                             handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "超过日限额,需要密码");
                                                         }
                                                     });
                                                 }else{
                                                     handleMealMoneyVO.setState(new byte[]{0x05});
                                                     handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "没有个人消费规则信息");
                                                 }
                                             });
                                         }else{
                                             handleMealMoneyVO.setState(new byte[]{0x05});
                                             handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "没有个人消费规则信息");
                                         }

                                 }else{
                                     //设备没有查找到，设备没有注册登记
                                     handleMealMoneyVO.setState(new byte[]{0x05});
                                     handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "设备没有登记");
                                 }
                             });
                         }else{
                             if("20".equals(handleMealMoneyVO.getCard_state()) ){
                                 handleMealMoneyVO.setState(new byte[]{0x06});
                                 handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "黑名单卡");
                             }else if(!PosUtil.sourceBiggerThanCurrent(handleMealMoneyVO.getCard_ineffectived_date())){
                                 handleMealMoneyVO.setState(new byte[]{0x0e});
                                 handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "卡片超过有效期");
                             }else {
                                 //非法卡
                                 handleMealMoneyVO.setState(new byte[]{0x01});
                                 handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "非法卡");
                             }
                         } 
                	 } 
                });
            }else {
                //系统中查不到有效卡
                handleMealMoneyVO.setState(new byte[]{0x01});
                handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "没有相关卡信息");
            }
		});
	}
}
