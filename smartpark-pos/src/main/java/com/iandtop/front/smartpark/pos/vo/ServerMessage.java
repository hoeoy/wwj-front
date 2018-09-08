package com.iandtop.front.smartpark.pos.vo;

import com.iandtop.common.utils.BinaryUtil;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ServerMessage {

    private byte[] messageHead = new byte[2];//消息头

    private byte[] messageType = new byte[1];//消息命令字

    private byte[] deviceCode = new byte[4];//机号

    private byte[] operator = new byte[4];//POS机操作员

    private byte[] cardCode = new byte[8];//物理卡号

    private byte[] cardState = new byte[1];//卡状态(00：正常 01：非法卡）

    private byte[] name = new byte[8];//提示信息（一般为姓名）

    private byte[] remain = new byte[4];//当前卡余额（左低右高）

    private byte[] limitMoneyDay = new byte[4];//日限额（左低右高）

    private byte[] limitMoneyOne = new byte[4];//单笔限额（左低右高）

    private byte[] cardLogicCode = new byte[4];//逻辑卡号

    private byte[] corpCode = new byte[4];//企业号（数字，左低右高）

    private byte[] CRC16 = new byte[2];//CRC16


    private byte[] time = new byte[7];//日期时间（20131207111314）
    private byte[] blackListAreaSize = new byte[2];//黑名单域长度(左低右高)
    private byte[] blackListArea;//黑名单域，格式为：00000000112233330A，卡号8字节，0A为增加，0D为删除。
    private byte[] blackListVersion = new byte[4];//最后一条黑名单对应的版本，也就是本批次最大版本号
    private byte[] allowanceVersion = new byte[4];//补贴版本
    private byte[] isSuccess = new byte[1];//00：成功  01：失败

    private byte[] mealMark = new byte[8];//交易标识
    private byte[] mealState = new byte[1];//回应位（00:消费成功 01：禁止消费 02：需要密码 04：余额不足 05：配置错误），为00，01，04时，需要pos机回写卡，供预扣费时使用。05时不允许消费，但是不回写卡。
    private byte[] mealMoney = new byte[4];//消费金额（左低右高）

    private byte[] cancelMoney = new byte[4];//撤销金额
    private byte[] cancelState = new byte[1];//撤销状态（00：成功 01：失败），需要pos机回写卡，供预扣费时使用。
    private byte[] cancelRemain = new byte[4];//撤销后余额（左低右高）

    private byte[] realMealMoney = new byte[4];//实际交易金额（左低右高）
    private byte[] upLoadState = new byte[1];//上传状态（00：成功 01：失败）

    public byte[] getBytes() {
        byte[] r = null;
        List<byte[]> bytebytes = new ArrayList<byte[]>();
        switch (messageType[0]) {
            case PosMessage.MESSAGE_TYPE_CARD_INFO:
                bytebytes.add(messageHead);
                bytebytes.add(messageType);
                bytebytes.add(deviceCode);
                bytebytes.add(operator);
                bytebytes.add(cardCode);
                bytebytes.add(cardState);
                bytebytes.add(name);
                bytebytes.add(remain);
                bytebytes.add(limitMoneyDay);
                bytebytes.add(limitMoneyOne);
                bytebytes.add(cardLogicCode);
                bytebytes.add(corpCode);
                bytebytes.add(CRC16);
                break;
            case PosMessage.MESSAGE_TYPE_MEAL:
            case PosMessage.MESSAGE_TYPE_MEAL_PASSWORD:
                bytebytes.add(messageHead);
                bytebytes.add(messageType);
                bytebytes.add(deviceCode);
                bytebytes.add(operator);
                bytebytes.add(cardCode);
                bytebytes.add(mealMark);
                bytebytes.add(mealState);
                bytebytes.add(remain);
                bytebytes.add(mealMoney);
                bytebytes.add(name);
                bytebytes.add(limitMoneyDay);
                bytebytes.add(limitMoneyOne);
                bytebytes.add(corpCode);
                bytebytes.add(CRC16);
                break;
            case PosMessage.MESSAGE_TYPE_STATISTIC_INFO:
                break;
            case PosMessage.MESSAGE_TYPE_CANCEL_MEAL:
                bytebytes.add(messageHead);
                bytebytes.add(messageType);
                bytebytes.add(deviceCode);
                bytebytes.add(operator);
                bytebytes.add(cardCode);
                bytebytes.add(mealMark);
                bytebytes.add(cancelMoney);
                bytebytes.add(cancelState);
                bytebytes.add(cancelRemain);
                bytebytes.add(name);
                bytebytes.add(limitMoneyDay);
                bytebytes.add(limitMoneyOne);
                bytebytes.add(corpCode);
                bytebytes.add(CRC16);
                break;
            case PosMessage.MESSAGE_TYPE_UPLOAD_PRE_MONEY:
                bytebytes.add(messageHead);
                bytebytes.add(messageType);
                bytebytes.add(deviceCode);
                bytebytes.add(operator);
                bytebytes.add(cardCode);
                bytebytes.add(mealMark);
                bytebytes.add(realMealMoney);
                bytebytes.add(upLoadState);
                bytebytes.add(corpCode);
                bytebytes.add(CRC16);
                break;
            case PosMessage.MESSAGE_TYPE_HEARTBEET:
                bytebytes.add(messageHead);
                bytebytes.add(messageType);
                bytebytes.add(deviceCode);
                bytebytes.add(operator);
                bytebytes.add(time);
                bytebytes.add(blackListAreaSize);
                bytebytes.add(blackListArea);
                bytebytes.add(blackListVersion);
                bytebytes.add(allowanceVersion);
                bytebytes.add(isSuccess);
                bytebytes.add(corpCode);
                bytebytes.add(CRC16);
                break;
            case PosMessage.MESSAGE_TYPE_CHARGE:
                break;
            case PosMessage.MESSAGE_TYPE_LOGIN:
                break;
        }

        r = BinaryUtil.bytsArrayListTobyteArray(bytebytes);
        return r;
    }

}
