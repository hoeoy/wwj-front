package com.iandtop.front.smartpark.pos.vo;

import lombok.Data;

@Data
public class PosMessage {

    //消息命令字（消息类型)
    public static final byte MESSAGE_TYPE_CARD_INFO = (byte) (0xa1 & 0xff);//卡信息查询
    public static final byte MESSAGE_TYPE_MEAL = (byte) (0xa2 & 0xff);//消费请求
    public static final byte MESSAGE_TYPE_MEAL_PASSWORD = (byte) (0xa3 & 0xff);//带密码的消费请求
    public static final byte MESSAGE_TYPE_STATISTIC_INFO = (byte) (0xa4 & 0xff);//统计查询
    public static final byte MESSAGE_TYPE_CANCEL_MEAL = (byte) (0xa5 & 0xff);//消费撤销
    public static final byte MESSAGE_TYPE_UPLOAD_PRE_MONEY = (byte) (0xa6 & 0xff);//预扣费数据上传
    public static final byte MESSAGE_TYPE_HEARTBEET = (byte) (0xa7 & 0xff);//心跳接口
    public static final byte MESSAGE_TYPE_CHARGE = (byte) (0xa9 & 0xff);//联机充值接口
    public static final byte MESSAGE_TYPE_LOGIN = (byte) (0xa0 & 0xff);//操作员联机登录接口

    //消息接收状态
    public static final int MESSAGE_RECEIVE_STATE_PRE = 0;//未接收
    public static final int MESSAGE_RECEIVE_STATE_LENGTH = 4;//已经获取到消息长度
    public static final int MESSAGE_RECEIVE_STATE_RECEIVING = 8;//正在读取后序信息
    public static final int MESSAGE_RECEIVE_STATE_RECEIVED = 16;//读取完成
    public static final int MESSAGE_RECEIVE_STATE_CRC16 = 32;//进行crc16校验成功
    public static final int MSG_HEAD_LENGTH = 2; //消息头长度

    private int messageReceiveState;//接收消息当前状态

    private byte[] messageContentStr;//消息字符串，包括head和body

    private byte[] messageHead = new byte[2];//消息头

    private int messgeBodyLength;//消息体长度

    private byte[] messageType;//消息命令字

    private byte[] deviceCode = new byte[4];//机号

    private byte[] operator = new byte[4];//POS机操作员

    private byte[] cardCode = new byte[8];//物理卡号

    private byte[] mealMark = new byte[8];//交易标识

    private byte[] mealType = new byte[1];//交易类型 0 : 消费 1 : 计次交易

    private byte[] mealMoney = new byte[4];//消费金额（左低右高）

    private byte[] mealTimestamp = new byte[7];//消费时间，交易时间（20131207111314）

    private byte[] subApplicationNum = new byte[1];//子应用号

    private byte[] subApplicationRemain = new byte[4];// 子应用交易前余额（左低右高）

    //private byte[] subApplicationInMoney = new byte[4];//子应用交易前进款流水（左低右高）

    private byte[] subApplicationOutMoney = new byte[4];//子应用交易前出款流水（左低右高）

    private byte[] recordType = new byte[1];//标志位（0：正常记录 1：消费灰记录 2：正常消费撤销记录 3：消费撤销灰记录）

    private byte[] deviceNum = new byte[4];//终端流水（左低右高）

    private byte[] cardCodeLogic = new byte[4];//逻辑卡号

    private byte[] remain = new byte[4];//当前卡余额（左低右高）

    private byte[] password = new byte[4];//超额消费密码（右补F）

    private byte[] mealDayMoney = new byte[4];//当前卡日累计额（左低右高）

    private byte[] batchNum = new byte[4];//当前卡流水

    private byte[] corpCode = new byte[4];//企业号（数字，左低右高）

    private byte[] CRC16 = new byte[2];//CRC16

    private byte[] blackListVersion = new byte[4];//黑名单版本号

    private byte[] unUploadDataSize = new byte[4];//未上传数据数量（左低右高）

}
