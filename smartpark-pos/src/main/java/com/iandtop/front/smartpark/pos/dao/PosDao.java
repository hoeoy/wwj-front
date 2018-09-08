package com.iandtop.front.smartpark.pos.dao;

import com.iandtop.common.utils.DateUtils;
import com.iandtop.front.smartpark.pos.util.PosConstants;
import com.iandtop.front.smartpark.pos.util.PosUtil;
import com.iandtop.front.smartpark.pos.vo.HandleMealMoneyVO;
import com.iandtop.front.smartpark.pub.utils.JDBCMysqlClientUtil;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;

import java.util.Calendar;

public class PosDao {

    public void queryPsnLastMealRecord(Vertx vertx, String device_code, Handler<ResultSet> resultSetHandler) {
        String mealRecordTableName = getMealRecordTableName();

        String sql = "select * from " + mealRecordTableName + " where device_code=? order by meal_ts desc ";
        JsonArray params = new JsonArray().add(device_code);
        JDBCMysqlClientUtil.executeCommon(vertx, PosConstants.vo, sql, params, resultSetHandler);
    }

    /**
     * 查询个人消费规则
     */
    public void mealPsnRule(Vertx vertx, String pk_meal_rule, Handler<ResultSet> resultSetHandler) {
        String sql = "select * from meal_rule where pk_meal_rule=?";
        JsonArray params = new JsonArray().add(pk_meal_rule);
        JDBCMysqlClientUtil.executeCommon(vertx, PosConstants.vo, sql, params, resultSetHandler);
    }

//    public void findCard(Vertx vertx, String cardCode, Handler<ResultSet> resultSetHandler) {
//        String sql = "select bd_psnbasdoc.pk_psnbasdoc, bd_psnbasdoc.psnname, bd_psnbasdoc.sex, bd_psnbasdoc.id, bd_psndoc.psncode, " +
//                " card_card.* ,card_ineffectived_date"
//                + " from bd_psnbasdoc inner join bd_psndoc on bd_psnbasdoc.pk_psnbasdoc=bd_psndoc.pk_psnbasdoc" +
//                " left join card_card on bd_psnbasdoc.pk_psnbasdoc=card_card.pk_psnbasdoc"
//                + " where card_card.card_code=? and card_card.card_state in('10','20')";
//        JsonArray params = new JsonArray().add(cardCode);
//        JDBCOracleClientUtil.executeCommon(vertx, PosConstants.vo, sql, params, resultSetHandler);
//    }

    public void findCard(Vertx vertx, String cardCode, Handler<ResultSet> resultSetHandler) {
        String sql = "select " +
                "a.*, " +
                "b.staff_code psncode, " +
                "b.staff_name psnname, " +
                "b.pk_staff  " +
                "from card_card a " +
                "left join db_staff b on a.pk_staff = b.pk_staff " +
                "where a.card_code=? and a.card_state in('10','20')";
        JsonArray params = new JsonArray().add(cardCode);
        JDBCMysqlClientUtil.executeCommon(vertx, PosConstants.vo, sql, params, resultSetHandler);
    }

    /**
     * 冻结需要限制的用户
     * 根据时间段限制及设备类型限制卡消费 仅限制餐厅消费，`device_type` bigint(20) DEFAULT NULL COMMENT
     * '1.餐厅终端2.咖啡厅终端3.网上超市终端' 判断当前消费，属于那个餐别： `dining_code` varchar(64) DEFAULT
     * NULL COMMENT '餐次餐别编码', 根据卡表中def1的限制规则，做限制处理 `def1` varchar(255) CHARACTER
     * SET utf8 DEFAULT NULL,
     * <p>
     * SELECT cc.* FROM card_card cc INNER JOIN meal_dining md ON
     * find_in_set(md.dining_code, cc.def1) WHERE cc.pk_card = "6" AND now()
     * BETWEEN md.begin_time AND md.end_time AND EXISTS ( SELECT mdv.pk_device
     * FROM meal_device mdv WHERE mdv.device_code = "21" AND mdv.device_type =
     * '1' );
     */
    public void findLimitCardbyCardIdAndDeviceCode(Vertx vertx, String strPKCard, String deviceCode, Handler<ResultSet> resultSetHandler) {

        String sql = "SELECT cc.* FROM card_card cc "
                + "INNER JOIN meal_dining md ON find_in_set(md.dining_code, cc.def1) "
                + "WHERE cc.pk_card = ? "
//                + "AND now() BETWEEN md.begin_time AND md.end_time "
                + "AND SUBSTR(NOW(),12,8) BETWEEN md.begin_time"
                + "AND EXISTS ("
                + "SELECT mdv.pk_device FROM meal_device mdv "
                + "WHERE mdv.device_code = ? "
                + "AND mdv.device_type = '1')";

        JsonArray params = new JsonArray();
        params.add(strPKCard);
        params.add(deviceCode);
        JDBCMysqlClientUtil.executeCommon(vertx, PosConstants.vo, sql, params, resultSetHandler);
    }

    public void getDeviceInfo(Vertx vertx, String deviceCode, Handler<ResultSet> resultSetHandler) {
        String sql = "select * from meal_device where device_code=?";
        JsonArray params = new JsonArray().add(deviceCode);
        JDBCMysqlClientUtil.executeCommon(vertx, PosConstants.vo, sql, params, resultSetHandler);
    }

    public void mealDeviceRule(Vertx vertx, String deviceCode, Handler<ResultSet> resultSetHandler) {
        String sql = "select " +
                "a.* " +
                "from device_time_sub a " +
                "left join meal_device b on a.pk_device_time = b.pk_device_time " +
                "where b.device_code=?";
        JsonArray params = new JsonArray().add(deviceCode);
        JDBCMysqlClientUtil.executeCommon(vertx, PosConstants.vo, sql, params, resultSetHandler);
    }

    //日实际消费次数和金额
    public void mealDayFreqAndMoney(Vertx vertx, String pk_card, Handler<ResultSet> resultSetHandler) {
        String mealRecordTableName = getMealRecordTableName();
        String sql = "select sum(meal_money) money from " + mealRecordTableName + " a " +
                "where meal_type='0' and meal_way='0' and a.meal_ts like'%" + PosUtil.getCurrentDate() + "%' and pk_card = ?";
        JsonArray params = new JsonArray().add(pk_card);
        JDBCMysqlClientUtil.executeCommon(vertx, PosConstants.vo, sql, params, resultSetHandler);
    }

    //时间段实际消费次数和金额
    public void mealTimeFreqAndMoney(Vertx vertx, String pk_card, String begin_time, String end_time, String meal_type, Handler<ResultSet> resultSetHandler) {
        String mealRecordTableName = getMealRecordTableName();
        String beginTime = PosUtil.getCurrentDate() + " " + begin_time;
        String endTime = PosUtil.getCurrentDate() + " " + end_time;

        /*
        String sql = "select sum(meal_money) money,count(*) frequency from " + mealRecordTableName + " a " +
                " where meal_type = '"+meal_type+"' and pk_card = ? and meal_way='0' " +
                "and ts >= '"+ beginTime +"' " +
                "and ts <= '"+ endTime +"' ";
                // 修改：20171020 只计算 启用段限制次同类设备 的 消费总和
        */
        String sql = "select sum(meal_money) money,count(*) frequency from " + mealRecordTableName + " a " +
                " where meal_type = '" + meal_type + "' and pk_card = ? and meal_way='0' " +
                "and ts >= '" + beginTime + "' " +
                "and ts <= '" + endTime + "' " +
                "and a.pk_device IN (SELECT mdv.pk_device FROM meal_device mdv WHERE mdv.be_control_time = 'Y')";

//                " STR_TO_DATE(ts,'yyyy-mm-dd hh24:mi:ss') >= STR_TO_DATE('" + beginTime + "','yyyy-mm-dd hh24:mi:ss') and" +
//                " STR_TO_DATE(ts,'yyyy-mm-dd hh24:mi:ss') <= STR_TO_DATE('" + endTime + "','yyyy-mm-dd hh24:mi:ss')";

        JsonArray params = new JsonArray().add(pk_card);
        JDBCMysqlClientUtil.executeCommon(vertx, PosConstants.vo, sql, params, resultSetHandler);
    }

    public void getMealDiningType(Vertx vertx, Handler<ResultSet> resultSetHandler) {
        String sql = "select * from meal_dining where 1=?";
        JsonArray params = new JsonArray().add(1);
        JDBCMysqlClientUtil.executeCommon(vertx, PosConstants.vo, sql, params, resultSetHandler);
    }

    public String getMealSql(HandleMealMoneyVO handleMealMoneyVO) {
        String sql = "update card_card set " +
                "money_cash=" + handleMealMoneyVO.getMoney_cash() +
                ",money_allowance=" + handleMealMoneyVO.getMoney_corp_grant() +
                ",serial=" + (handleMealMoneyVO.getSerial() + 1) +
                "  where pk_card='" + handleMealMoneyVO.getPk_card() + "' ";
        return sql;
    }

    public void mealTx(SQLConnection connection, HandleMealMoneyVO handleMealMoneyVO, Handler<UpdateResult> resultSetHandler) {
        String sql = getMealSql(handleMealMoneyVO);
        JDBCMysqlClientUtil.execute(connection, sql, resultSetHandler);
    }

    public String getRecordSql(HandleMealMoneyVO handleMealMoneyVO) {
        String mealRecordTableName = getMealRecordTableName();
        int retain = handleMealMoneyVO.getMoney_corp_grant() + handleMealMoneyVO.getMoney_cash();
        int real_meal_cash_money = handleMealMoneyVO.getLast_money_cash() - handleMealMoneyVO.getMoney_cash();
        int real_meal_grant_Money = handleMealMoneyVO.getLast_money_corp_grant() - handleMealMoneyVO.getMoney_corp_grant();

        String sql = "insert into " + mealRecordTableName + "(" +
                "pk_staff," +
                "pk_card," +
                "pk_device," +
                "staff_code," +
                "card_code," +
                "meal_money," +
                "cash_retain," +
                "allowance_retain," +
                "money_retain," +
                "meal_type," +
                "meal_cash_money," +
                "meal_allowance," +
                "meal_way," +
                "device_meal_type," +
                "device_code," +
                "dining_code," +
                "dining_name," +
                "meal_ts)" +
                " values ('" +
                handleMealMoneyVO.getPk_psnbasdoc() + "','" +
                handleMealMoneyVO.getPk_card() + "','" +
                handleMealMoneyVO.getPk_device() + "','" +
                handleMealMoneyVO.getPsncode() + "','" +
                handleMealMoneyVO.getCardCode() + "'," +
                handleMealMoneyVO.getMealMoney() + "," +
                handleMealMoneyVO.getMoney_cash() + "," +
                handleMealMoneyVO.getMoney_corp_grant() + "," +
                retain + ",'" +
                handleMealMoneyVO.getMeal_type() + "'," +
                real_meal_cash_money + "," +
                real_meal_grant_Money + "," +
                "'0','" +
                handleMealMoneyVO.getDevice_meal_type() + "','" +
                handleMealMoneyVO.getDevice_code() + "','" +
                handleMealMoneyVO.getDining_code() + "','" +
                handleMealMoneyVO.getType_name() + "','" +
                DateUtils.currentDatetime() + "'" +
                ")";

        return sql;
    }

    public void mealRecordTx(SQLConnection connection, HandleMealMoneyVO handleMealMoneyVO, Handler<UpdateResult> resultSetHandler) {
        String sql = getRecordSql(handleMealMoneyVO);
        JDBCMysqlClientUtil.execute(connection, sql, resultSetHandler);
    }

    public void uploadPreMoneyRecordTx(SQLConnection connection, HandleMealMoneyVO handleMealMoneyVO, Handler<UpdateResult> resultSetHandler) {
        String mealRecordTableName = handleMealMoneyVO.getTablename();
        int retain = handleMealMoneyVO.getMoney_corp_grant() + handleMealMoneyVO.getMoney_cash();
        int real_meal_cash_money = handleMealMoneyVO.getLast_money_cash() - handleMealMoneyVO.getMoney_cash();
        int real_meal_grant_Money = handleMealMoneyVO.getLast_money_corp_grant() - handleMealMoneyVO.getMoney_corp_grant();

        String sql = "insert into " + mealRecordTableName + "(" +
                "pk_staff," +
                "pk_card," +
                "pk_device," +
                "staff_code," +
                "card_code," +
                "meal_money," +
                "cash_retain," +
                "allowance_retain," +
                "money_retain," +
                "meal_type," +
                "meal_cash_money," +
                "meal_allowance," +
                "meal_way," +
                "device_meal_type," +
                "device_code," +
                "dining_code," +
                "dining_name," +
                "meal_ts)" +
                " values ('" +
                handleMealMoneyVO.getPk_psnbasdoc() + "','" +
                handleMealMoneyVO.getPk_card() + "','" +
                handleMealMoneyVO.getPk_device() + "','" +
                handleMealMoneyVO.getPsncode() + "','" +
                handleMealMoneyVO.getCardCode() + "'," +
                handleMealMoneyVO.getMealMoney() + "," +
                handleMealMoneyVO.getMoney_cash() + "," +
                handleMealMoneyVO.getMoney_corp_grant() + "," +
                retain + ",'" +
                handleMealMoneyVO.getMeal_type() + "'," +
                real_meal_cash_money + "," +
                real_meal_grant_Money + "," +
                "'0','" +
                handleMealMoneyVO.getDevice_meal_type() + "','" +
                handleMealMoneyVO.getDevice_code() + "','" +
                handleMealMoneyVO.getDining_code() + "','" +
                handleMealMoneyVO.getType_name() + "','" +
                /*
                 * DateUtils.currentDatetime() + "'" + ")";
				 */
                handleMealMoneyVO.getMeal_ts() + "'" + ")";
        JDBCMysqlClientUtil.execute(connection, sql, resultSetHandler);
    }

    public void mealRecordErroeTx(SQLConnection connection, HandleMealMoneyVO handleMealMoneyVO, Handler<UpdateResult> resultSetHandler) {
        String sql = "insert into meal_record_error(" +
                "cardcode," +
                "mealmoney," +
                "meal_ts," +
                "devicecode," +
                "record_type," +
                "memo) " +
                "values('" + handleMealMoneyVO.getCardCode() + "'," + handleMealMoneyVO.getMealMoney() + ",'" + handleMealMoneyVO.getMeal_ts() + "','" + handleMealMoneyVO.getDevice_code() + "','" + handleMealMoneyVO.getMeal_type() + "','" + handleMealMoneyVO.getMemo() + "')";
        JDBCMysqlClientUtil.execute(connection, sql, resultSetHandler);
    }

    private String getMealRecordTableName() {
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH) + 1;
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        String mealRecordTableName = "meal_record_" + currentYear + "_" + ((currentMonth < 10) ? ("0" + currentMonth) : currentMonth) + "_" + ((currentDay < 15) ? "01" : "15");
        return mealRecordTableName;
    }

}
