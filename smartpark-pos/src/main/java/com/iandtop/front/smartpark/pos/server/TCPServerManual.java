package com.iandtop.front.smartpark.pos.server;

import com.iandtop.common.utils.BinaryUtil;
import com.iandtop.front.smartpark.pos.filter.PosFilter;
import com.iandtop.front.smartpark.pos.util.PosConstants;
import com.iandtop.front.smartpark.pos.vo.PosMessage;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;

public class TCPServerManual {
    Logger logger = LoggerFactory.getLogger(TCPServerManual.class);

    public void startTCPServer(Vertx vertx) {
        NetServer server = vertx.createNetServer();
        //pos机采用的是短连接，一个连接一个报文交易，交易后链接立即关闭。
        //新的交易来到后会新开启一个链接，所以不用考虑多条消息报文交叉接收的情况，不用考虑粘包,拆包
        server.connectHandler(socket -> {
            //未接收  ，1已经获取到消息长度，2已经获取到消息长度，正在读取后序信息，3读取完成，4进行crc16校验成功
            PosMessage message = new PosMessage();
            message.setMessageReceiveState(PosMessage.MESSAGE_RECEIVE_STATE_PRE);

            socket.handler(buffer -> {
                System.out.println(BinaryUtil.bytesToHexString(buffer.getBytes()));
                switch (message.getMessageReceiveState()) {
                    case PosMessage.MESSAGE_RECEIVE_STATE_PRE:
                        message.setMessageContentStr(buffer.getBytes());
                        //大于等于消息头长度,说明消息头传送完成,可以读取消息头信息来获得消息体长度
                        if (message.getMessageContentStr().length >= PosMessage.MSG_HEAD_LENGTH) {
                            byte[] bs = buffer.getBytes(0, 2);
                            message.setMessageHead(bs);
                            byte[] reversedBs = BinaryUtil.littleEndianToBigEndian(bs);
                            message.setMessgeBodyLength(new String(reversedBs).hashCode());
                            //目前接收到的消息的长度大于等于消息总长度
                            if (message.getMessageContentStr().length >= PosMessage.MSG_HEAD_LENGTH + message.getMessgeBodyLength()) {
                                //进入消息发送完成状态
                               /* message.setMessageReceiveState(POSMessage.MESSAGE_RECEIVE_STATE_CRC16);
                                //TODO 进行crc16数据验证
                                PosFilter.getInstance().buildPOSMessage(message);
                                PosFilter.getInstance().buildPOSServerMessage(message, vertx, posServerMessage -> {
                                    if (posServerMessage != null) {
                                        byte[] bytes = posServerMessage.getBytes();
                                        Buffer buffer2 = Buffer.buffer().appendBytes(bytes);
                                        socket.write(buffer2);
                                    }
                                });*/
                                handleMessage(vertx, socket, message);
                            } else {
                                //进入继续接收消息状态
                                message.setMessageReceiveState(PosMessage.MESSAGE_RECEIVE_STATE_LENGTH);
                            }
                        }
                        break;
                    case PosMessage.MESSAGE_RECEIVE_STATE_LENGTH:
                    case PosMessage.MESSAGE_RECEIVE_STATE_RECEIVING:
                        message.setMessageContentStr(buffer.getBytes());
                        //目前接收到的消息的长度大于等于消息总长度
                        if (message.getMessageContentStr().length >= PosMessage.MSG_HEAD_LENGTH + message.getMessgeBodyLength()) {
                            handleMessage(vertx, socket, message);
                        }
                        break;
                }
            });

            socket.closeHandler(v -> {
                logger.info("本次链接关闭");
            });

        }).listen(PosConstants.server_port, res -> {
            if (res.succeeded()) {
                logger.info("监听成功,监听端口号：" + server.actualPort());
            } else {
                logger.error("监听失败");
            }
        });
    }

    /**
     * 处理一条完整接收到的消息，并且服务器返回相应的消息回答
     *
     * @param message
     */
    private void handleMessage(Vertx vertx, NetSocket socket, PosMessage message) {
        message.setMessageReceiveState(PosMessage.MESSAGE_RECEIVE_STATE_CRC16);
        //TODO 进行crc16数据验证
        PosFilter.getInstance().buildPOSMessage(message);
        PosFilter.getInstance().buildPOSServerMessage(message, vertx, posServerMessage -> {
            if (posServerMessage != null) {
                byte[] bytes = posServerMessage.getBytes();
                Buffer buffer2 = Buffer.buffer().appendBytes(bytes);
                socket.write(buffer2);
                logger.info("服务器返回:" + BinaryUtil.bytesToHexString(bytes));
            }
        });
    }
}

