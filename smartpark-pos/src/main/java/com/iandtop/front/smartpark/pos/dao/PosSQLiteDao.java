package com.iandtop.front.smartpark.pos.dao;

import com.iandtop.front.smartpark.pos.util.PosConstants;
import com.iandtop.front.smartpark.pub.utils.JDBCSQLiteClientUtil;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import java.sql.SQLException;
import java.util.Map;

/**
 * 本地sqlite数据库
 * @author andyzhaozhao
 */
public class PosSQLiteDao {
    private final static String DB_NAME = "sppos";//sqlite数据库名称

    public static void getParamValue(Vertx vertx, String key, Handler<String> resultHandler) {
        String sql = "SELECT * FROM door_param where param_key = '" + key + "'";
        try {
            JDBCSQLiteClientUtil.executeCommon(vertx, DB_NAME, sql, data -> {
                if (data.size() > 0) {
                    Map map = (Map) data.get(0);
                    resultHandler.handle((String) map.get("param_value"));
                } else {
                    resultHandler.handle(PosConstants.NOValue);
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
            resultHandler.handle(PosConstants.NOValue);
        }
    }

    public static void updateParam(Vertx vertx, String key, String value, Handler<Integer> resultHandler) {
        String sql = "update door_param set param_value='" + value + "' where param_key ='" + key + "' ";
        try {
            JDBCSQLiteClientUtil.executeUpdateCommon(vertx, DB_NAME, sql, num -> {
                resultHandler.handle(num);
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void insertParam(Vertx vertx, String key, String value, Handler<Integer> resultHandler) {
        String sql = "insert into door_param (param_key,param_value) " +
                "values ('" + key + "','" + value + "')";
        try {
            JDBCSQLiteClientUtil.executeUpdateCommon(vertx, DB_NAME, sql, num -> {
                resultHandler.handle(num);
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void initTables(Vertx vertx, Handler<Integer> resultHandler) {
        String sysparamSql = "create table IF NOT EXISTS door_param(" +
                "param_key		TEXT primary key  ," +
                "param_value	TEXT not null );";

        try {
            JDBCSQLiteClientUtil.executeUpdateCommon(vertx,DB_NAME, sysparamSql, num -> {
                resultHandler.handle(num);
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void dropTables(Vertx vertx, Handler<Integer> resultHandler) {
        String sysparamSql = "drop TABLE if EXISTS pos_param;";
        try {
            JDBCSQLiteClientUtil.executeUpdateCommon(vertx,DB_NAME, sysparamSql, res -> {
                resultHandler.handle(res);
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
