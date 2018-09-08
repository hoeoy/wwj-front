package com.iandtop.front.smartpark.pos.action;

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
 * 计次   只控制段限次
 */
public class MealCountAction extends BaseAction implements IPosAction {

    private static MealCountAction instance = null;

    private MealCountAction() {

    }

    public static MealCountAction getInstance() {
        if (instance == null) {
            instance = new MealCountAction();
        }
        return instance;
    }

    @Override
    public void handle(PosMessage msg, Vertx vertx, Handler<ServerMessage> posServerMessageHandler) {
        ServerMessage posServerMessage = getPOSServerMessage(msg, new byte[]{0x38, 0x00});

        HandleMealMoneyVO handleMealMoneyVO = new HandleMealMoneyVO();

        handleMealMoneyVO.setMeal_type(HandleMealMoneyVO.Meal_Type_Count);
        handleMealMoneyVO.setCardCode(getCardCode(msg) + "");
        handleMealMoneyVO.setMealMoney(0);
        handleMealMoneyVO.setPosServerMessage(posServerMessage);
        String deviceCode = getDeviceCode(msg) + "";

        PosDao posDao = new PosDao();

        posDao.findCard(vertx, handleMealMoneyVO.getCardCode(), resultSet -> {
            List<JsonObject> cards = resultSet.getRows();
            if(cards != null && cards.size() > 0) {
                String psnname = cards.get(0).getString("psnname");
                posServerMessage.setName(setNameBytes(psnname));

                handleMealMoneyVO.setPsnname(psnname);
                handleMealMoneyVO.setPsncode(cards.get(0).getString("psncode"));
                handleMealMoneyVO.setPk_meal_rule(cards.get(0).getLong("pk_meal_rule").toString());
                handleMealMoneyVO.setPk_card(cards.get(0).getLong("pk_card").toString());
                handleMealMoneyVO.setPk_psnbasdoc(cards.get(0).getLong("pk_staff").toString());
                handleMealMoneyVO.setCard_ineffectived_date(cards.get(0).getString("card_ineffectived_ts"));//失效时间
                handleMealMoneyVO.setLast_money_cash((cards.get(0).getInteger("money_cash")));//现金钱包
                handleMealMoneyVO.setLast_money_corp_grant((cards.get(0).getInteger("money_allowance")));//补贴钱包
                handleMealMoneyVO.setCard_state(cards.get(0).getString("card_state"));

                //判断是否合法卡
                if ("10".equals(handleMealMoneyVO.getCard_state()) && PosUtil.sourceBiggerThanCurrent(handleMealMoneyVO.getCard_ineffectived_date())) {
                    posDao.getDeviceInfo(vertx, deviceCode, deviceRule -> {
                        List<JsonObject> devices = deviceRule.getRows();

                        if (devices != null && devices.size() > 0) {
                            handleMealMoneyVO.setPk_device(devices.get(0).getLong("pk_device").toString());
                            handleMealMoneyVO.setDevice_code(devices.get(0).getString("device_code"));
                            handleMealMoneyVO.setDevice_meal_type(Integer.parseInt(devices.get(0).getString("device_meal_type")));//设备消费类型
                            handleMealMoneyVO.setBe_control_time(devices.get(0).getString("be_control_time"));

                            //设备必须启用段控制
                            if("Y".equals(handleMealMoneyVO.getBe_control_time())){

                                if(handleMealMoneyVO.getPk_meal_rule() != null){

                                            final Integer[] frequency_time = new Integer[1];

                                            posDao.mealDeviceRule(vertx,deviceCode,resultSet1 -> {
                                                List<JsonObject> reuslt = resultSet1.getRows();

                                                if(reuslt != null && reuslt.size() > 0) {

                                                    String time_begin = null;
                                                    String time_end = null;
                                                    String currentTime = DateUtils.currentTime();
                                                    boolean beInDeviceSegment = false;

                                                    //判断当前时间是否在设备规则的时间段内
                                                    for (int i = 0; i < reuslt.size(); i++) {
                                                        time_begin = reuslt.get(i).getString("start_time");
                                                        time_end = reuslt.get(i).getString("end_time");
                                                        if (DateUtils.betweenTime(time_begin, time_end, currentTime)) {
                                                            beInDeviceSegment = true;
                                                            frequency_time[0] = reuslt.get(i).getInteger("frequency_time");
                                                            break;
                                                        }
                                                    }

                                                    if(beInDeviceSegment){
                                                        posDao.mealTimeFreqAndMoney(vertx,handleMealMoneyVO.getPk_card(),time_begin,time_end,"1",timeFreqAndMoney->{
                                                            List<JsonObject> psnSegRecord = timeFreqAndMoney.getRows();
                                                            Integer segFrequency = psnSegRecord.get(0).getInteger("frequency") == null ? 0 : psnSegRecord.get(0).getInteger("frequency");
                                                            if(segFrequency < frequency_time[0]){
                                                                handleMealMoneyVO.setReal_mealMoney(handleMealMoneyVO.getMealMoney());
                                                                mealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao);
                                                            }else{
                                                                handleMealMoneyVO.setState(new byte[]{0x07});
                                                                handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "超过段限次");
                                                            }

                                                        });
                                                    }else{
                                                        handleMealMoneyVO.setState(new byte[]{0x12});
                                                        handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "不在刷卡时间段内");
                                                    }

                                                }else{
                                                    handleMealMoneyVO.setState(new byte[]{0x05});
                                                    handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "没有设置设备规则");
                                                }
                                            });


                                }else{
                                    handleMealMoneyVO.setState(new byte[]{0x05});
                                    handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "没有个人消费规则信息");
                                }


                            }else{
                                handleMealMoneyVO.setState(new byte[]{0x05});
                                handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "设备未启用段控制");
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
                    handleMealMoneyVO.setState(new byte[]{0x01});
                    handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "非法卡");
                }
            }else{
                handleMealMoneyVO.setState(new byte[]{0x01});
                handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "非法卡");
            }
        });


    }
}
