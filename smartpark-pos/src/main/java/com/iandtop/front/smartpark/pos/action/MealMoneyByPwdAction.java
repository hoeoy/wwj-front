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
 * Created by Administrator on 2016-12-06.
 */
public class MealMoneyByPwdAction extends BaseAction implements IPosAction {

    private static MealMoneyByPwdAction instance = null;

    private MealMoneyByPwdAction() {

    }

    public static MealMoneyByPwdAction getInstance() {
        if (instance == null) {
            instance = new MealMoneyByPwdAction();
        }
        return instance;
    }

    @Override
    public void handle(PosMessage msg, Vertx vertx, Handler<ServerMessage> posServerMessageHandler) {
        ServerMessage posServerMessage = getPOSServerMessage(msg, new byte[]{0x38, 0x00});
        HandleMealMoneyVO handleMealMoneyVO = new HandleMealMoneyVO();

        handleMealMoneyVO.setCardCode(getCardCode(msg) + "");
        int mealMoney = BinaryUtil.byteToIntLowInF(msg.getMealMoney());
        String password = getPassword(msg);

        handleMealMoneyVO.setMealMoney(mealMoney);//消费金额
        handleMealMoneyVO.setPosServerMessage(posServerMessage);
        handleMealMoneyVO.setMeal_type(HandleMealMoneyVO.Meal_Type_Normal);// 1是计次消费，0扣款消费
        String deviceCode = getDeviceCode(msg) + "";

        PosDao posDao = new PosDao();
        posDao.findCard(vertx, handleMealMoneyVO.getCardCode(), resultSet -> {
            List<JsonObject> cards = resultSet.getRows();
            if (cards != null && cards.size() > 0) {
                String name = cards.get(0).getString("psnname");
                posServerMessage.setName(setNameBytes(name));

                handleMealMoneyVO.setPsncode(cards.get(0).getString("psncode"));
                handleMealMoneyVO.setPsnname(name);
                handleMealMoneyVO.setPk_meal_rule(cards.get(0).getLong("pk_meal_rule").toString());
                handleMealMoneyVO.setPk_card(cards.get(0).getLong("pk_card").toString());
                handleMealMoneyVO.setPk_psnbasdoc(cards.get(0).getLong("pk_staff").toString());
                handleMealMoneyVO.setCard_ineffectived_date(cards.get(0).getString("card_ineffectived_ts"));//失效时间
                handleMealMoneyVO.setLast_money_cash(cards.get(0).getInteger("money_cash"));//将单位变为分,现金钱包
                handleMealMoneyVO.setLast_money_corp_grant(cards.get(0).getInteger("money_allowance"));//将单位变为分,补贴钱包
                handleMealMoneyVO.setPwd_for_beyond_quota(cards.get(0).getString("password"));
                handleMealMoneyVO.setMoney_cash(-1);
                handleMealMoneyVO.setMoney_corp_grant(-1);


                if(password != null && password.equals(handleMealMoneyVO.getPwd_for_beyond_quota())){

                    if (PosUtil.sourceBiggerThanCurrent(handleMealMoneyVO.getCard_ineffectived_date())) {

                        posDao.getDeviceInfo(vertx, deviceCode, deviceRule -> {
                            List<JsonObject> devices = deviceRule.getRows();
                            if (devices != null && devices.size() > 0) {

                                handleMealMoneyVO.setPk_device(devices.get(0).getLong("pk_device").toString());
                                handleMealMoneyVO.setDevice_code(devices.get(0).getString("device_code"));
                                handleMealMoneyVO.setDevice_meal_type(Integer.parseInt(devices.get(0).getString("device_meal_type")));//设备消费类型
                                handleMealMoneyVO.setBe_control_time(devices.get(0).getString("be_control_time"));

                                //判断设备是否启用段控制
                                if("Y".equals(handleMealMoneyVO.getBe_control_time())){

                                    posDao.mealDeviceRule(vertx,deviceCode,resultSet1 -> {
                                        List<JsonObject> reuslt = resultSet1.getRows();
                                        if(reuslt != null && reuslt.size() > 0){

                                            String time_begin = null;
                                            String time_end = null;
                                            String currentTime = DateUtils.currentTime();
                                            boolean beInDeviceSegment = false;

                                            //判断当前时间是否在设备规则的时间段内
                                            for (int i = 0; i < reuslt.size(); i++){
                                                time_begin = reuslt.get(i).getString("start_time");
                                                time_end = reuslt.get(i).getString("end_time");
                                                if(DateUtils.betweenTime(time_begin,time_end,currentTime)){
                                                    beInDeviceSegment = true;
                                                    break;
                                                }
                                            }

                                            if(beInDeviceSegment){
                                                //直接消费

                                                handleMealMoneyVO.setReal_mealMoney(handleMealMoneyVO.getMealMoney());
                                                mealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao);
                                            }else{
                                                handleMealMoneyVO.setState(new byte[]{0x0c});
                                                handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "不在刷卡时间段内");
                                            }
                                        }else{
                                            handleMealMoneyVO.setState(new byte[]{0x01});
                                            handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "未获取到设备规则相关信息");
                                        }
                                    });
                                }else{
                                    //直接消费
                                    handleMealMoneyVO.setReal_mealMoney(handleMealMoneyVO.getMealMoney());
                                    mealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao);
                                }
                            } else {//设备没有查找到，设备没有注册登记
                                handleMealMoneyVO.setState(new byte[]{0x05});
                                handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "设备没有登记");
                            }
                        });
                    } else {//如果卡已经过期
                        handleMealMoneyVO.setState(new byte[]{0x0e});
                        handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "卡片已经过期");
                    }
                }else{
                    handleMealMoneyVO.setState(new byte[]{0x0b});
                    handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "密码错误");
                }
            } else {//系统中查不到有效卡
                handleMealMoneyVO.setState(new byte[]{0xd});
                handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "未找到卡");
            }
        });
    }
}
