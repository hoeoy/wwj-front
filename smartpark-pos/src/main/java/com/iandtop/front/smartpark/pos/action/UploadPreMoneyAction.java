package com.iandtop.front.smartpark.pos.action;

import java.util.List;

import com.iandtop.common.utils.BinaryUtil;
import com.iandtop.front.smartpark.pos.dao.PosDao;
import com.iandtop.front.smartpark.pos.util.PosUtil;
import com.iandtop.front.smartpark.pos.vo.HandleMealMoneyVO;
import com.iandtop.front.smartpark.pos.vo.PosMessage;
import com.iandtop.front.smartpark.pos.vo.ServerMessage;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * 计次   只控制段限次
 */
public class UploadPreMoneyAction extends BaseAction implements IPosAction {

    private static UploadPreMoneyAction instance = null;

    private UploadPreMoneyAction() {
    }

    public static UploadPreMoneyAction getInstance() {
        if (instance == null) {
            instance = new UploadPreMoneyAction();
        }
        return instance;
    }

    @Override
    public void handle(PosMessage msg, Vertx vertx, Handler<ServerMessage> posServerMessageHandler) {
        PosDao posDao = new PosDao();
        ServerMessage posServerMessage = getPOSServerMessage(msg, new byte[]{0x24, 0x00});
        //TODO 具体业务 需要安装lombok插件
        String cardCode = getCardCode(msg) + "";
        int mealMoney = BinaryUtil.byteToIntLowInF(msg.getMealMoney());
        String mealTimestamp = BinaryUtil.bytesToHexString(msg.getMealTimestamp());

        String meal_ts = mealTimestamp.substring(0,4)+"-"+mealTimestamp.substring(4,6)+"-"+mealTimestamp.substring(6,8)+" "+mealTimestamp.substring(8,10)+":"+mealTimestamp.substring(10,12)+":"+mealTimestamp.substring(12,14);


        int subApplicationRemain = BinaryUtil.byteToIntLowInF(msg.getSubApplicationRemain());
        int subApplicationOutMoney = BinaryUtil.byteToIntLowInF(msg.getSubApplicationOutMoney());

        int recordType = BinaryUtil.oneByteToIntHignInF(msg.getRecordType()[0]);
        int deviceCode =  BinaryUtil.byteToIntLowInF(msg.getDeviceCode());

        HandleMealMoneyVO handleMealMoneyVO = new HandleMealMoneyVO();

        handleMealMoneyVO.setDevice_code(String.valueOf(deviceCode));
        handleMealMoneyVO.setMeal_ts(meal_ts);
        handleMealMoneyVO.setMeal_type(HandleMealMoneyVO.Meal_Type_Accounting);
        handleMealMoneyVO.setCardCode(cardCode);
        handleMealMoneyVO.setMealMoney(mealMoney);//消费金额
        handleMealMoneyVO.setPosServerMessage(posServerMessage);
        posDao.findCard(vertx, cardCode, resultSet -> {
            List<JsonObject> cards = resultSet.getRows();
            if(cards != null && cards.size() > 0){

                String psnname = cards.get(0).getString("psnname");

                handleMealMoneyVO.setPsncode(cards.get(0).getString("psncode"));
                handleMealMoneyVO.setPsnname(psnname);
                handleMealMoneyVO.setPk_meal_rule(cards.get(0).getLong("pk_meal_rule").toString());
                handleMealMoneyVO.setPk_card(cards.get(0).getLong("pk_card").toString());
                handleMealMoneyVO.setPk_psnbasdoc(cards.get(0).getLong("pk_staff").toString());
                handleMealMoneyVO.setCard_ineffectived_date(cards.get(0).getString("card_ineffectived_ts"));//失效时间
                handleMealMoneyVO.setLast_money_cash((cards.get(0).getInteger("money_cash")));//现金钱包
                handleMealMoneyVO.setLast_money_corp_grant((cards.get(0).getInteger("money_allowance")));//补贴钱包
                handleMealMoneyVO.setCard_state(cards.get(0).getString("card_state"));
                handleMealMoneyVO.setPwd_for_beyond_quota("");
                handleMealMoneyVO.setSerial(cards.get(0).getInteger("serial"));

                //判断是否合法卡
                if(("10".equals(handleMealMoneyVO.getCard_state()) || "20".equals(handleMealMoneyVO.getCard_state())) && PosUtil.sourceBiggerThanCurrent(handleMealMoneyVO.getCard_ineffectived_date())){

                    posDao.getDeviceInfo(vertx, String.valueOf(deviceCode), deviceRule -> {
                        List<JsonObject> devices = deviceRule.getRows();
                        if (devices != null && devices.size() > 0) {

                            handleMealMoneyVO.setPk_device(devices.get(0).getLong("pk_device").toString());
                            handleMealMoneyVO.setDevice_code(devices.get(0).getString("device_code"));
                            handleMealMoneyVO.setDevice_meal_type(Integer.parseInt(devices.get(0).getString("device_meal_type")));//设备消费类型

                            uploadPreMoney(vertx, handleMealMoneyVO, posServerMessageHandler, posDao);

                        }else{
                            //未找到设备信息
                            handleMealMoneyVO.setMemo("未找到设备信息");
                            uploadPreMoneyError(vertx,handleMealMoneyVO,posServerMessageHandler,posDao);
                            posServerMessage.setRealMealMoney(new byte[]{0x00, 0x00, 0x00, 0x00});
                            posServerMessage.setUpLoadState(new byte[]{0x00});//success:00,fail:01
                            posServerMessageHandler.handle(posServerMessage);
                        }
                    });

                }else{
                    //非法卡
                    handleMealMoneyVO.setMemo("非法卡");
                    uploadPreMoneyError(vertx,handleMealMoneyVO,posServerMessageHandler,posDao);
                    posServerMessage.setRealMealMoney(new byte[]{0x00, 0x00, 0x00, 0x00});
                    posServerMessage.setUpLoadState(new byte[]{0x00});//success:00,fail:01
                    posServerMessageHandler.handle(posServerMessage);
                }

            }else{
                //系统中查不到有效卡
                handleMealMoneyVO.setMemo("系统中查不到有效卡");
                uploadPreMoneyError(vertx,handleMealMoneyVO,posServerMessageHandler,posDao);
                posServerMessage.setRealMealMoney(new byte[]{0x00, 0x00, 0x00, 0x00});
                posServerMessage.setUpLoadState(new byte[]{0x00});//success:00,fail:01
                posServerMessageHandler.handle(posServerMessage);
            }
                }
        );

    }
}
