package com.iandtop.front.smartpark.pos.action;

import com.iandtop.common.utils.BinaryUtil;
import com.iandtop.front.smartpark.pos.util.PosUtil;
import com.iandtop.front.smartpark.pos.vo.PosMessage;
import com.iandtop.front.smartpark.pos.vo.ServerMessage;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

/**
 * 心跳处理
 * @author andyzhao
 */
public class HeartBeatAction extends BaseAction implements IPosAction {

    @Override
    public void handle(PosMessage msg, Vertx vertx, Handler<ServerMessage> posServerMessageHandler) {
        //TODO dao
        int blackListAreaSize = 9;//四个字节 ,黑名单区域占用9个字节(每条黑名单数据占9个字节)
        int messageHead = BinaryUtil.byteToIntLowInF(new byte[]{0x21, 0x00, 0x00, 0x00}) + blackListAreaSize;
        byte[] messageHeads = BinaryUtil.intToByteLowInF(messageHead);
        ServerMessage posServerMessage = new ServerMessage();
        posServerMessage.getMessageHead()[0] = messageHeads[0];
        posServerMessage.getMessageHead()[1] = messageHeads[1];
        posServerMessage.setMessageType(msg.getMessageType());
        posServerMessage.setDeviceCode(msg.getDeviceCode());
        posServerMessage.setOperator(msg.getOperator());

        String cTime = PosUtil.getCurrentTime();
        byte[] bcd = BinaryUtil.ascii2bcd(cTime.getBytes(), cTime.length());
        posServerMessage.setTime(bcd);
        byte[] blbytes = BinaryUtil.intToByteLowInF(blackListAreaSize);
        posServerMessage.setBlackListAreaSize(new byte[]{blbytes[0], blbytes[1]});
        String blackList = "00000000112233330A";
        byte[] blackListBytes = BinaryUtil.ascii2bcd(blackList.getBytes(), blackList.length());
        posServerMessage.setBlackListArea(blackListBytes);
        posServerMessage.setBlackListVersion(msg.getBlackListVersion());
        posServerMessage.setAllowanceVersion(new byte[]{0x74, 0x00, 0x00, 0x00});
        posServerMessage.setIsSuccess(new byte[]{0x00});

        posServerMessage.setCorpCode(msg.getCorpCode());

        //设置crc16
        setCRC16(posServerMessage);
        posServerMessageHandler.handle(posServerMessage);
    }
}
