package com.iandtop.front.smartpark.pos.filter;

import com.iandtop.common.utils.BinaryUtil;
import com.iandtop.front.smartpark.pos.action.*;
import com.iandtop.front.smartpark.pos.vo.PosMessage;
import com.iandtop.front.smartpark.pos.vo.ServerMessage;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import java.util.Arrays;

public class PosFilter {

    private static PosFilter instance = null;

    private PosFilter() {

    }

    public static PosFilter getInstance() {
        if (instance == null) {
            instance = new PosFilter();
        }
        return instance;
    }

    public PosMessage buildPOSMessage(PosMessage posMessage) {
        byte[] msgContent = posMessage.getMessageContentStr();
        byte[] msgType = Arrays.copyOfRange(msgContent, 2, 3);//根据规范，第三个字符代表命令字
        posMessage.setMessageType(msgType);

        switch (posMessage.getMessageType()[0]) {
            case PosMessage.MESSAGE_TYPE_CARD_INFO:
                posMessage.setDeviceCode(Arrays.copyOfRange(msgContent, 3, 7));
                posMessage.setOperator(Arrays.copyOfRange(msgContent, 7, 11));
                posMessage.setCardCode(Arrays.copyOfRange(msgContent, 11, 19));
                posMessage.setRemain(Arrays.copyOfRange(msgContent, 19, 23));
                posMessage.setBatchNum(Arrays.copyOfRange(msgContent, 23, 27));
                posMessage.setCorpCode(Arrays.copyOfRange(msgContent, 27, 31));
                posMessage.setCRC16(Arrays.copyOfRange(msgContent, 31, 33));
                break;
            case PosMessage.MESSAGE_TYPE_MEAL:
                posMessage.setDeviceCode(Arrays.copyOfRange(msgContent, 3, 7));
                posMessage.setOperator(Arrays.copyOfRange(msgContent, 7, 11));
                posMessage.setCardCode(Arrays.copyOfRange(msgContent, 11, 19));
                posMessage.setMealMark(Arrays.copyOfRange(msgContent, 19, 27));
                posMessage.setMealType(Arrays.copyOfRange(msgContent, 27, 28));
                posMessage.setMealMoney(Arrays.copyOfRange(msgContent, 28, 32));
                posMessage.setRemain(Arrays.copyOfRange(msgContent, 32, 36));
                posMessage.setMealDayMoney(Arrays.copyOfRange(msgContent, 36, 40));
                posMessage.setBatchNum(Arrays.copyOfRange(msgContent, 40, 44));
                posMessage.setCorpCode(Arrays.copyOfRange(msgContent, 44, 48));
                posMessage.setCRC16(Arrays.copyOfRange(msgContent, 48, 50));
                break;
            case PosMessage.MESSAGE_TYPE_MEAL_PASSWORD:
                posMessage.setDeviceCode(Arrays.copyOfRange(msgContent, 3, 7));
                posMessage.setOperator(Arrays.copyOfRange(msgContent, 7, 11));
                posMessage.setCardCode(Arrays.copyOfRange(msgContent, 11, 19));
                posMessage.setMealMark(Arrays.copyOfRange(msgContent, 19, 27));
                posMessage.setMealType(Arrays.copyOfRange(msgContent, 27, 28));
                posMessage.setMealMoney(Arrays.copyOfRange(msgContent, 28, 32));
                posMessage.setRemain(Arrays.copyOfRange(msgContent, 32, 36));
                posMessage.setPassword(Arrays.copyOfRange(msgContent, 36, 40));
                posMessage.setMealDayMoney(Arrays.copyOfRange(msgContent, 40, 44));
                posMessage.setBatchNum(Arrays.copyOfRange(msgContent, 44, 48));
                posMessage.setCorpCode(Arrays.copyOfRange(msgContent, 48, 52));
                posMessage.setCRC16(Arrays.copyOfRange(msgContent, 52, 54));
                break;
            case PosMessage.MESSAGE_TYPE_STATISTIC_INFO:
                break;
            case PosMessage.MESSAGE_TYPE_CANCEL_MEAL:
                posMessage.setDeviceCode(Arrays.copyOfRange(msgContent, 3, 7));
                posMessage.setOperator(Arrays.copyOfRange(msgContent, 7, 11));
                posMessage.setCardCode(Arrays.copyOfRange(msgContent, 11, 19));
                posMessage.setRemain(Arrays.copyOfRange(msgContent, 19, 23));
                posMessage.setMealMark(Arrays.copyOfRange(msgContent, 23, 31));
                posMessage.setBatchNum(Arrays.copyOfRange(msgContent, 31, 35));
                posMessage.setCorpCode(Arrays.copyOfRange(msgContent, 35, 39));
                posMessage.setCRC16(Arrays.copyOfRange(msgContent, 39, 41));
                break;
            case PosMessage.MESSAGE_TYPE_UPLOAD_PRE_MONEY://预扣费接口
                posMessage.setDeviceCode(Arrays.copyOfRange(msgContent, 3, 7));//4
                posMessage.setOperator(Arrays.copyOfRange(msgContent, 7, 11));//4
                posMessage.setCardCode(Arrays.copyOfRange(msgContent, 11, 19));//物理卡号
                posMessage.setMealMark(Arrays.copyOfRange(msgContent, 19, 27));//交易标志8
                posMessage.setMealType(Arrays.copyOfRange(msgContent, 27, 28));//交易类型
                posMessage.setMealMoney(Arrays.copyOfRange(msgContent, 28, 32));//交易金额
                posMessage.setMealTimestamp(Arrays.copyOfRange(msgContent, 32, 39));//交易时间
                posMessage.setSubApplicationNum(Arrays.copyOfRange(msgContent, 39, 40));//子应用号
                posMessage.setSubApplicationRemain(Arrays.copyOfRange(msgContent, 40, 44));
                posMessage.setSubApplicationOutMoney(Arrays.copyOfRange(msgContent, 44, 48));//子应用交易前出款流水
                posMessage.setRecordType(Arrays.copyOfRange(msgContent, 48, 49));//标志位
                posMessage.setDeviceNum(Arrays.copyOfRange(msgContent, 49,53));//终端流水
                posMessage.setCorpCode(Arrays.copyOfRange(msgContent, 53, 57));
                posMessage.setCRC16(Arrays.copyOfRange(msgContent, 57, 59));
                break;
            case PosMessage.MESSAGE_TYPE_HEARTBEET:
                posMessage.setDeviceCode(Arrays.copyOfRange(msgContent, 3, 7));
                posMessage.setOperator(Arrays.copyOfRange(msgContent, 7, 11));
                posMessage.setBlackListVersion(Arrays.copyOfRange(msgContent, 11, 15));
                posMessage.setUnUploadDataSize(Arrays.copyOfRange(msgContent, 15, 19));
                posMessage.setCorpCode(Arrays.copyOfRange(msgContent, 19, 23));
                posMessage.setCRC16(Arrays.copyOfRange(msgContent, 23, 25));
                break;
            case PosMessage.MESSAGE_TYPE_CHARGE:
                break;
            case PosMessage.MESSAGE_TYPE_LOGIN:
                break;
        }

        return posMessage;
    }

    public void buildPOSServerMessage(PosMessage message, Vertx vertx, Handler<ServerMessage> posServerMessageHandler) {
        switch (message.getMessageType()[0]) {
            case PosMessage.MESSAGE_TYPE_CARD_INFO:
                new FindCardInfoAction().handle(message, vertx, posServerMessageHandler);
                break;
            case PosMessage.MESSAGE_TYPE_MEAL:

                byte mealtbyte = message.getMealType()[0];
                int mealType = BinaryUtil.byteToIntHignInF(new byte[]{0x00, 0x00, 0x00, mealtbyte});
                switch (mealType) {
                    case 0:
                        MealMoneyAction.getInstance().handle(message, vertx, posServerMessageHandler);
                        break;
                    case 1:
                        MealCountAction.getInstance().handle(message, vertx, posServerMessageHandler);
                        break;
                }
                break;
            case PosMessage.MESSAGE_TYPE_MEAL_PASSWORD:
                byte mealtbytePassword = message.getMealType()[0];
                int mealtTytePassword = BinaryUtil.byteToIntHignInF(new byte[]{0x00, 0x00, 0x00, mealtbytePassword});
                switch (mealtTytePassword) {
                    case 0:
                        MealMoneyByPwdAction.getInstance().handle(message, vertx, posServerMessageHandler);
                        break;
                    case 1:
                        MealCountAction.getInstance().handle(message, vertx, posServerMessageHandler);
                        break;
                }
                break;
            case PosMessage.MESSAGE_TYPE_STATISTIC_INFO:
                break;
            case PosMessage.MESSAGE_TYPE_CANCEL_MEAL:
                MealCancelAction.getInstance().handle(message, vertx, posServerMessageHandler);
                break;
            case PosMessage.MESSAGE_TYPE_UPLOAD_PRE_MONEY:
                UploadPreMoneyAction.getInstance().handle(message, vertx, posServerMessageHandler);
                break;
            case PosMessage.MESSAGE_TYPE_HEARTBEET:
                new HeartBeatAction().handle(message, vertx, posServerMessageHandler);
                break;
            case PosMessage.MESSAGE_TYPE_CHARGE:
                break;
            case PosMessage.MESSAGE_TYPE_LOGIN:
                break;
        }
    }

}

