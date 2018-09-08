package com.iandtop.front.smartpark.pos.action;

import com.iandtop.common.utils.BinaryUtil;
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
 * 不进行消费规则，直接扣款消费
 *
 * @author andyzhao
 */
public class MealNoRuleAction extends BaseAction implements IPosAction {

    public void handle(PosMessage msg, Vertx vertx, Handler<ServerMessage> posServerMessageHandler) {
        ServerMessage posServerMessage = getPOSServerMessage(msg, new byte[]{0x38, 0x00});
        HandleMealMoneyVO handleMealMoneyVO = new HandleMealMoneyVO();
        handleMealMoneyVO.setCardCode(getCardCode(msg) + "");
        int mealMoney = BinaryUtil.byteToIntLowInF(msg.getMealMoney());

        handleMealMoneyVO.setMealMoney(mealMoney);//消费金额
        handleMealMoneyVO.setPosServerMessage(posServerMessage);
        handleMealMoneyVO.setMeal_type(0);// 1是计次消费，0扣款消费
        String deviceCode = getDeviceCode(msg) + "";

        PosDao posDao = new PosDao();
        posDao.findCard(vertx, handleMealMoneyVO.getCardCode(), resultSet -> {
            List<JsonObject> cards = resultSet.getRows();
            if (cards != null && cards.size() > 0) {
                String name = cards.get(0).getString("PSNNAME");
                posServerMessage.setName(setNameBytes(name));

                handleMealMoneyVO.setPsnname(name);
                handleMealMoneyVO.setPk_card(cards.get(0).getString("PK_CARD"));
                handleMealMoneyVO.setPk_corp(cards.get(0).getString("PK_CORP"));
                handleMealMoneyVO.setPk_psnbasdoc(cards.get(0).getString("PK_PSNBASDOC"));
                handleMealMoneyVO.setCard_ineffectived_date(cards.get(0).getString("CARD_INEFFECTIVED_DATE"));//失效时间
                handleMealMoneyVO.setLast_money_cash((cards.get(0).getInteger("MONEY_CASH")));
                handleMealMoneyVO.setLast_money_corp_grant((cards.get(0).getInteger("MONEY_CORP_GRANT") ));
                handleMealMoneyVO.setMoney_cash(-1);
                handleMealMoneyVO.setMoney_corp_grant(-1);

                if (PosUtil.sourceBiggerThanCurrent(handleMealMoneyVO.getCard_ineffectived_date())) {
                    posDao.mealDeviceRule(vertx, deviceCode, deviceRule -> {
                        List<JsonObject> devices = deviceRule.getRows();
                        if (devices != null && devices.size() > 0) {
                            handleMealMoneyVO.setPk_device(devices.get(0).getString("PK_DEVICE"));
                            handleMealMoneyVO.setDevice_meal_type(Integer.parseInt(devices.get(0).getString("DEVICE_MEAL_TYPE")));//设备消费类型
                            //直接消费
                            handleMealMoneyVO.setReal_mealMoney(handleMealMoneyVO.getMealMoney());
                            mealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao);
                        } else {//设备没有查找到，设备没有注册登记
                            handleMealMoneyVO.setState(new byte[]{0x05});
                            handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "设备没有登记");
                        }
                    });
                } else {//如果卡已经过期
                    handleMealMoneyVO.setState(new byte[]{0x01});
                    handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "卡片已经过期");
                }
            } else {//系统中查不到有效卡
                posServerMessage.setName(setNameBytes("未找到"));
                handleMealMoneyVO.setState(new byte[]{0x01});
                handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "未找到卡");
            }
        });
    }
}
