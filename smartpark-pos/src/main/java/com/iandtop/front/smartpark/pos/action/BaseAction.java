package com.iandtop.front.smartpark.pos.action;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import com.iandtop.common.utils.BinaryUtil;
import com.iandtop.front.smartpark.pos.dao.PosDao;
import com.iandtop.front.smartpark.pos.util.DateUtils;
import com.iandtop.front.smartpark.pos.util.MealRecordUtils;
import com.iandtop.front.smartpark.pos.util.PosConstants;
import com.iandtop.front.smartpark.pos.util.PosUtil;
import com.iandtop.front.smartpark.pos.vo.HandleMealMoneyVO;
import com.iandtop.front.smartpark.pos.vo.PosMessage;
import com.iandtop.front.smartpark.pos.vo.ServerMessage;
import com.iandtop.front.smartpark.pub.utils.JDBCMysqlClientUtil;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * action公共类
 *
 * @author andyzhao
 */
public class BaseAction {

    Logger logger = LoggerFactory.getLogger(BaseAction.class);

    //POSServerMessage拼接，公共部分
    protected ServerMessage getPOSServerMessage(PosMessage msg, byte[] messageHead) {
        ServerMessage posServerMessage = new ServerMessage();
        posServerMessage.setMessageHead(messageHead);
        posServerMessage.setMessageType(msg.getMessageType());
        posServerMessage.setDeviceCode(msg.getDeviceCode());
        posServerMessage.setOperator(msg.getOperator());
        posServerMessage.setCardCode(msg.getCardCode());
        posServerMessage.setMealMark(msg.getMealMark());
        posServerMessage.setLimitMoneyDay(new byte[]{0x00, 0x00, 0x00, 0x00});
        posServerMessage.setLimitMoneyOne(new byte[]{0x00, 0x00, 0x00, 0x00});
        posServerMessage.setCardLogicCode(new byte[]{0x00, 0x00, 0x00, 0x00});
        posServerMessage.setCorpCode(msg.getCorpCode());
        posServerMessage.setMealMoney(msg.getMealMoney());
        return posServerMessage;
    }

    /**
     * 设置CRC16的值，检测报文的CRC16位前面的所有位数,生成CRC16值
     *
     * @param posServerMessage
     */
    protected void setCRC16(ServerMessage posServerMessage) {
        byte[] mbytes = posServerMessage.getBytes();
        byte[] needToCrc16Check = Arrays.copyOfRange(mbytes, 0, mbytes.length - 2);
        byte[] crc16byte = BinaryUtil.getCRC16(needToCrc16Check);
        posServerMessage.getCRC16()[0] = crc16byte[0];
        posServerMessage.getCRC16()[1] = crc16byte[1];
    }

    protected String getPassword(PosMessage msg) {
        byte[] passwordBytes = msg.getPassword();
        String password = BinaryUtil.bcd2Str(passwordBytes);
        //目前密码只支持6位
        if (password != null && password.length() >= 8) {
            password = password.substring(2, 8);

            return String.valueOf(Integer.parseInt(password));
        }
        return null;
    }

    protected long getCardCode(PosMessage msg) {
        byte[] cardCodes = msg.getCardCode();
        //目前只用四个字节
        byte[] tcardCodes = new byte[]{cardCodes[4], cardCodes[5], cardCodes[6], cardCodes[7]};
        long cardCode = BinaryUtil.byteToIntLowInF(tcardCodes);
        cardCode = BinaryUtil.longIntToLong(cardCode);
        return cardCode;
    }

    protected long getMealType(PosMessage msg) {
        byte[] bMealType = new byte[]{msg.getMealType()[0]};
        long mealType = BinaryUtil.byteToIntLowInF(bMealType);
        mealType = BinaryUtil.longIntToLong(mealType);
        return mealType;
    }

    protected long getDeviceCode(PosMessage msg) {
        byte[] deviceCodes = msg.getDeviceCode();
        long cardCode = BinaryUtil.byteToIntLowInF(deviceCodes);
        return cardCode;
    }

    /**
     * 左对齐
     */
    protected byte[] setNameBytes(String name) {
        byte[] nameBytes = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        if (name != null && name.trim().length() > 0) {
            if (name.length() > 8) {
                name = name.substring(0, 8);
            }

            try {
                byte[] realNameBytes = name.getBytes("GBK");
                int length = realNameBytes.length;
                if (length > 8) {
                    length = 8;
                }
                for (int i = 0; i < length; i++) {
                    nameBytes[i] = realNameBytes[i];
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return nameBytes;
    }

    /**
     * 右对齐
     */
//    protected byte[] setNameBytes(String name) {
//        byte[] nameBytes = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
//        try {
//            byte[] realNameBytes = name.getBytes("GBK");
//            int startPos = nameBytes.length - realNameBytes.length;//
//            int tmppos = 0;
//            for (int i = startPos; i < nameBytes.length; i++) {
//                nameBytes[i] = realNameBytes[tmppos];
//                tmppos++;
//            }
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//        return nameBytes;
//    }
    protected void mealMoney(Vertx vertx, HandleMealMoneyVO handleMealMoneyVO, Handler<ServerMessage> posServerMessageHandler, PosDao posDao) {
        int retain = -1;//剩余金额
        switch (handleMealMoneyVO.getMeal_type()) {
            case HandleMealMoneyVO.Meal_Type_Count://计次
                handleMealMoneyVO.setState(new byte[]{0x00});
                handleMealMoneyVO.setMoney_cash(handleMealMoneyVO.getLast_money_cash());
                handleMealMoneyVO.setMoney_corp_grant(handleMealMoneyVO.getLast_money_corp_grant());

                handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "消费成功");
                break;
            case HandleMealMoneyVO.Meal_Type_Normal://扣款
                //计算扣款金额
                switch (handleMealMoneyVO.getDevice_meal_type()) {//判断设备规则
                    case 0://仅现金
                        retain = handleMealMoneyVO.getLast_money_cash() - handleMealMoneyVO.getMealMoney();
                        handleMealMoneyVO.setMoney_cash(retain);
                        handleMealMoneyVO.setMoney_corp_grant(handleMealMoneyVO.getLast_money_corp_grant());
                        break;
                    case 10://仅补贴
                        retain = handleMealMoneyVO.getLast_money_corp_grant() - handleMealMoneyVO.getMealMoney();
                        handleMealMoneyVO.setMoney_cash(handleMealMoneyVO.getLast_money_cash());
                        handleMealMoneyVO.setMoney_corp_grant(retain);
                        break;
                    case 20://先现金后补贴
                        retain = handleMealMoneyVO.getLast_money_cash() - handleMealMoneyVO.getMealMoney();
                        if (retain < 0) {//如果现金不够，继续扣补贴
                            retain = handleMealMoneyVO.getLast_money_corp_grant() + retain;
                            handleMealMoneyVO.setMoney_cash(0);
                            handleMealMoneyVO.setMoney_corp_grant(retain);
                        } else {
                            handleMealMoneyVO.setMoney_cash(retain);
                            handleMealMoneyVO.setMoney_corp_grant(handleMealMoneyVO.getLast_money_corp_grant());
                        }
                        break;
                    case 40://先补贴后现金
                        retain = handleMealMoneyVO.getLast_money_corp_grant() - handleMealMoneyVO.getMealMoney();
                        if (retain < 0) {//如果补贴不够，继续扣款
                            retain = handleMealMoneyVO.getLast_money_cash() + retain;
                            handleMealMoneyVO.setMoney_cash(retain);
                            handleMealMoneyVO.setMoney_corp_grant(0);
                        } else {
                            handleMealMoneyVO.setMoney_corp_grant(retain);
                            handleMealMoneyVO.setMoney_cash(handleMealMoneyVO.getLast_money_cash());
                        }
                        break;
                }


                if (retain < 0) {//如果仍然不够，余额不足
                    handleMealMoneyVO.setState(new byte[]{0x04});
                    handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "如果仍然不够，余额不足");
                } else {
                    handleMealMoneyVO.setState(new byte[]{0x00});
                    handleMealMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao, "消费成功");
                }
                break;


        }
    }

    protected void handleMealMoney(Vertx vertx, HandleMealMoneyVO handleMealMoneyVO, Handler<ServerMessage> posServerMessageHandler,
                                   PosDao posDao, String stateMessage) {
        handleMealMoneyVO.getPosServerMessage().setMealState(handleMealMoneyVO.getState());
        //设置crc16
        setCRC16(handleMealMoneyVO.getPosServerMessage());

        //如果是00，说明可以正常消费
        int retain = 0;
        if (handleMealMoneyVO.getState()[0] == 0x00) {

            //可以成功扣款
            //根据设备的消费规则返回不同的余额显示
            if (handleMealMoneyVO.getDevice_meal_type() == 10) {
                //仅补贴
                retain = handleMealMoneyVO.getMoney_corp_grant();
            } else if (handleMealMoneyVO.getDevice_meal_type() == 0) {
                //仅现金
                retain = handleMealMoneyVO.getMoney_cash();
            } else {
                retain = handleMealMoneyVO.getMoney_cash() + handleMealMoneyVO.getMoney_corp_grant();
            }

            handleMealMoneyVO.getPosServerMessage().setRemain(BinaryUtil.intToByteLowInF(retain));//实际卡余额
            handleMealMoneyVO.getPosServerMessage().setCancelRemain(BinaryUtil.intToByteLowInF(retain));
            handleMealMoneyVO.getPosServerMessage().setMealMoney(BinaryUtil.intToByteLowInF(handleMealMoneyVO.getMealMoney()));//实际消费金额

            JDBCMysqlClientUtil.executeUpdateTX(vertx, PosConstants.vo, taskHandler -> {

                //更新卡信息
                posDao.mealTx(taskHandler.getConnection(), handleMealMoneyVO, meal -> {

                    //查询消费餐别
                    posDao.getMealDiningType(vertx, diningTypes -> {

                        List<JsonObject> types = diningTypes.getRows();
                        String start_time = null;
                        String end_time = null;
                        String be_valid = null;
                        String nowTime = PosUtil.getCurrentTimeWithoutDate();
                        for (JsonObject type : types) {
                            start_time = type.getString("begin_time");
                            end_time = type.getString("end_time");
                            be_valid = type.getString("be_valid");
                            if ("Y".equals(be_valid)) {
                                if (PosUtil.between(start_time, end_time, nowTime)) {
                                    handleMealMoneyVO.setType_name(type.getString("dining_name"));
                                    handleMealMoneyVO.setDining_code(type.getString("dining_code"));
//                                    handleMealMoneyVO.setPk_dining_type(type.getString("pk_dining"));
                                }
                            }
                        }

                        //插入消费记录表
                        posDao.mealRecordTx(taskHandler.getConnection(), handleMealMoneyVO, mealRecord -> {
                            taskHandler.getResultHandler().handle(true);
                        });
                    });

                });

            }, result -> {
                if (result) {
                    posServerMessageHandler.handle(handleMealMoneyVO.getPosServerMessage());
                } else {
                    logger.error("插入数据库失败");
                }
            });

        } else {//无法正常消费的返回状态
            handleMealMoneyVO.getPosServerMessage().setRemain(BinaryUtil.intToByteLowInF(0));//实际卡余额
            handleMealMoneyVO.getPosServerMessage().setMealMoney(BinaryUtil.intToByteLowInF(0));//实际消费金额
            posServerMessageHandler.handle(handleMealMoneyVO.getPosServerMessage());
        }
    }

    protected void uploadPreMoney(Vertx vertx, HandleMealMoneyVO handleMealMoneyVO, Handler<ServerMessage> posServerMessageHandler, PosDao posDao) {

        int retain = 0;
        switch(handleMealMoneyVO.getDevice_meal_type()){
            case 0://仅现金
                retain = handleMealMoneyVO.getLast_money_cash() - handleMealMoneyVO.getMealMoney();
                handleMealMoneyVO.setMoney_cash(retain);
                handleMealMoneyVO.setMoney_corp_grant(handleMealMoneyVO.getLast_money_corp_grant());
                break;
            case 10://仅补贴
                retain = handleMealMoneyVO.getLast_money_corp_grant() - handleMealMoneyVO.getMealMoney();
                handleMealMoneyVO.setMoney_cash(handleMealMoneyVO.getLast_money_cash());
                handleMealMoneyVO.setMoney_corp_grant(retain);
                break;
            case 20://先现金后补贴
                retain = handleMealMoneyVO.getLast_money_cash() - handleMealMoneyVO.getMealMoney();
                if (retain < 0) {//如果现金不够，继续扣补贴
                    retain = handleMealMoneyVO.getLast_money_corp_grant() + retain;
                    handleMealMoneyVO.setMoney_cash(0);
                    handleMealMoneyVO.setMoney_corp_grant(retain);
                } else {
                    handleMealMoneyVO.setMoney_cash(retain);
                    handleMealMoneyVO.setMoney_corp_grant(handleMealMoneyVO.getLast_money_corp_grant());
                }
                break;
            case 40://先补贴后现金
                retain = handleMealMoneyVO.getLast_money_corp_grant() - handleMealMoneyVO.getMealMoney();
                if (retain < 0) {//如果补贴不够，继续扣款
                    retain = handleMealMoneyVO.getLast_money_cash() + retain;
                    handleMealMoneyVO.setMoney_cash(retain);
                    handleMealMoneyVO.setMoney_corp_grant(0);
                } else {
                    handleMealMoneyVO.setMoney_corp_grant(retain);
                    handleMealMoneyVO.setMoney_cash(handleMealMoneyVO.getLast_money_cash());
                }
                break;
        }

        JDBCMysqlClientUtil.executeUpdateTX(vertx, PosConstants.vo, taskHandler -> {

            //更新卡信息
            posDao.mealTx(taskHandler.getConnection(), handleMealMoneyVO, meal -> {


                //查询消费餐别
                posDao.getMealDiningType(vertx, diningTypes -> {

                    List<JsonObject> types = diningTypes.getRows();
                    String start_time = null;
                    String end_time = null;
                    String be_valid = null;
                    String nowTime = handleMealMoneyVO.getMeal_ts().substring(11,19);
                    for (JsonObject type : types) {
                        start_time = type.getString("begin_time");
                        end_time = type.getString("end_time");
                        be_valid = type.getString("be_valid");
                        if ("Y".equals(be_valid)) {
                            if (PosUtil.between(start_time, end_time, nowTime)) {
                                handleMealMoneyVO.setType_name(type.getString("dining_name"));
                                handleMealMoneyVO.setDining_code(type.getString("dining_code"));
//                                    handleMealMoneyVO.setPk_dining_type(type.getString("pk_dining"));
                            }
                        }
                    }

                    try {
                        handleMealMoneyVO.setTablename(MealRecordUtils.getMealRecordName(DateUtils.parseDatetime(handleMealMoneyVO.getMeal_ts()).getTime()));
                        //插入消费记录表
                        posDao.uploadPreMoneyRecordTx(taskHandler.getConnection(), handleMealMoneyVO, mealRecord -> {
                            taskHandler.getResultHandler().handle(true);
                        });
                    } catch (ParseException e) {
                        e.printStackTrace();
                        taskHandler.getResultHandler().handle(false);
                    }
                });



            });

        }, result -> {
            if (result) {
                handleMealMoneyVO.getPosServerMessage().setRealMealMoney(BinaryUtil.intToByteLowInF(handleMealMoneyVO.getMealMoney()));
                handleMealMoneyVO.getPosServerMessage().setUpLoadState(new byte[]{0x00});
                posServerMessageHandler.handle(handleMealMoneyVO.getPosServerMessage());
            } else {
                logger.error("插入数据库失败");
            }
        });

    }

    protected void uploadPreMoneyError(Vertx vertx, HandleMealMoneyVO handleMealMoneyVO, Handler<ServerMessage> posServerMessageHandler, PosDao posDao) {
        JDBCMysqlClientUtil.executeUpdateTX(vertx, PosConstants.vo, taskHandler -> {

            //插入消费记录表
            posDao.mealRecordErroeTx(taskHandler.getConnection(), handleMealMoneyVO, mealRecord -> {
                taskHandler.getResultHandler().handle(true);

            });

        }, result -> {
            if (result) {
                posServerMessageHandler.handle(handleMealMoneyVO.getPosServerMessage());
            } else {
                logger.error("插入数据库失败");
            }
        });
    }

}


