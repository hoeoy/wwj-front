package com.iandtop.front.smartpark.pos.action;

import java.util.List;

import com.iandtop.common.utils.BinaryUtil;
import com.iandtop.front.smartpark.pos.dao.PosDao;
import com.iandtop.front.smartpark.pos.vo.PosMessage;
import com.iandtop.front.smartpark.pos.vo.ServerMessage;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * 查找用户处理
 *
 * @author andyzhao
 */
public class FindCardInfoAction extends BaseAction implements IPosAction {

    @Override
    public void handle(PosMessage msg, Vertx vertx, Handler<ServerMessage> posServerMessageHandler) {
        final long cardCode = getCardCode(msg);
        String deviceCode = getDeviceCode(msg) + "";
        PosDao posDao = new PosDao();
        posDao.findCard(vertx, cardCode + "", resultSet -> {
            List<JsonObject> cards = resultSet.getRows();
           ServerMessage posServerMessage = getPOSServerMessage(msg, new byte[]{0x30, 0x00});
            if (cards != null && cards.size() > 0) {
                int bash = (cards.get(0).getInteger("money_cash"));//将单位变为分,现金钱包
                int MONEY_CORP_GRANT = (cards.get(0).getInteger("money_allowance"));//将单位变为分,现金钱包
                String name = cards.get(0).getString("psnname");

                posDao.getDeviceInfo(vertx, deviceCode, deviceRule -> {

                    byte[] nameBytes = setNameBytes(name);
                    byte[] bashBytes = null;

                    List<JsonObject> devices = deviceRule.getRows();
                    if (devices != null && devices.size() > 0) {
                        Integer MEAL_TYPE =  Integer.parseInt(devices.get(0).getString("device_meal_type"));
                        switch (MEAL_TYPE){
                            case 0:
                                bashBytes = BinaryUtil.intToByteLowInF(bash);
                                break;
                            case 10:
                                bashBytes = BinaryUtil.intToByteLowInF(MONEY_CORP_GRANT);
                                break;
                            default:
                                bashBytes = BinaryUtil.intToByteLowInF(bash + MONEY_CORP_GRANT);
                                break;
                        };
                    };

                    posServerMessage.setCardState(new byte[]{0x00});
                    posServerMessage.setName(nameBytes);
                    posServerMessage.setRemain(bashBytes);

                    setCRC16(posServerMessage); //设置crc16
                    posServerMessageHandler.handle(posServerMessage);
                });

            } else {
                posServerMessage.setCardState(new byte[]{0x01});
                posServerMessage.setName(setNameBytes("未找到"));
                posServerMessage.setRemain(BinaryUtil.intToByteLowInF(0));

                setCRC16(posServerMessage); //设置crc16
                posServerMessageHandler.handle(posServerMessage);
            }


        });
    }


}
