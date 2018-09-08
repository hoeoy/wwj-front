package com.iandtop.front.smartpark.pos.action;

import java.util.List;

import com.iandtop.front.smartpark.pos.dao.PosDao;
import com.iandtop.front.smartpark.pos.vo.HandleMealMoneyVO;
import com.iandtop.front.smartpark.pos.vo.PosMessage;
import com.iandtop.front.smartpark.pos.vo.ServerMessage;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 *
 */
public class MealCancelAction extends BaseAction implements IPosAction {

    private static MealCancelAction instance = null;

    private MealCancelAction() {

    }

    public static MealCancelAction getInstance() {
        if (instance == null) {
            instance = new MealCancelAction();
        }
        return instance;
    }

    @Override
    public void handle(PosMessage msg, Vertx vertx, Handler<ServerMessage> posServerMessageHandler) {

        /**
         * 消费撤销流程:
         *  1 --> 获取设备发送的信息:机号,卡号
         *  2 --> 数据库中查找对应机号最后一笔消费记录
         *  3 --> 对比卡号是否一样
         *  4 --> 卡号一样:记录表中插入记录,更新卡表数据
         *        卡号不一样:返回撤销失败
         */

        ServerMessage posServerMessage = getPOSServerMessage(msg, new byte[]{0x38, 0x00});
        HandleMealMoneyVO handleMealMoneyVO = new HandleMealMoneyVO();

        handleMealMoneyVO.setMeal_type(HandleMealMoneyVO.Meal_Type_Normal);
        handleMealMoneyVO.setCardCode(getCardCode(msg) + "");
        handleMealMoneyVO.setPosServerMessage(posServerMessage);
        String deviceCode = getDeviceCode(msg) + "";
        PosDao posDao = new PosDao();

        posDao.findCard(vertx, handleMealMoneyVO.getCardCode(), resultSet -> {
            List<JsonObject> cards = resultSet.getRows();
            if(cards != null && cards.size() > 0){

                String psnname = cards.get(0).getString("psnname");
                posServerMessage.setName(setNameBytes(psnname));
                handleMealMoneyVO.setPsnname(psnname);
                handleMealMoneyVO.setPsncode(cards.get(0).getString("psncode"));
                handleMealMoneyVO.setPk_card(cards.get(0).getLong("pk_card").toString());
                handleMealMoneyVO.setPk_psnbasdoc(cards.get(0).getLong("pk_staff").toString());
                handleMealMoneyVO.setLast_money_cash((cards.get(0).getInteger("money_cash")));//现金钱包
                handleMealMoneyVO.setLast_money_corp_grant((cards.get(0).getInteger("money_allowance")));//补贴钱包
                handleMealMoneyVO.setCard_state(cards.get(0).getString("card_state"));
                handleMealMoneyVO.setPwd_for_beyond_quota("");

                posDao.queryPsnLastMealRecord(vertx,deviceCode,lastRecord -> {
                    List<JsonObject> records = lastRecord.getRows();

                    if(records != null && records.size() > 0){
                        Integer mealMoney = 0-records.get(0).getInteger("meal_money");
                        String pk_device = records.get(0).getLong("pk_device").toString();
//                        String device_meal_type = records.get(0).getString("device_meal_type");

//                        handleMealMoneyVO.setDevice_meal_type(Integer.parseInt(device_meal_type));
                        handleMealMoneyVO.setDevice_code(deviceCode);
                        handleMealMoneyVO.setPk_device(pk_device);
                        handleMealMoneyVO.setMealMoney(mealMoney);
                        handleMealMoneyVO.setReal_mealMoney(mealMoney);
                        mealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao);

                    }else{
                        posServerMessage.setCancelState(new byte[]{0x00});
                        posServerMessage.setName(setNameBytes("未找到消费记录"));
                        handleMealMoneyVO.setState(new byte[]{0x01});
                        handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "未找到消费记录");
                    }
                });
            }else{
                posServerMessage.setCancelState(new byte[]{0x00});
                posServerMessage.setName(setNameBytes("未找到卡信息"));
                handleMealMoneyVO.setState(new byte[]{0x01});
                handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "没有相关卡信息");
            }
        });

    }
}
