package com.iandtop.front.smartpark.pub.utils;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.jdbc.JDBCClient;

import java.sql.*;
import java.util.List;

/**
 * sqlite链接工具类
 * @author andyzhao
 */
public class JDBCSQLiteClientUtil {

  /*  public static JDBCClient getSQLiteJDBCClient(Vertx vertx){
        if(jdbcClient == null){
            synchronized (JDBCOracleClientUtil.class)
            {
                if (jdbcClient == null) {
                    jdbcClient  = JDBCClient.createShared(vertx, new JsonObject()
                            .put("url", "org.sqlite.JDBC")
                            .put("driver_class", "jdbc:sqlite:door.db")
//                            .put("user", "smartpark")
//                            .put("password", "smartpark")
                            .put("max_pool_size", 30));
                }
            }
        }

        return jdbcClient;
    }*/

    public static void isExistTable(Vertx vertx, String dbName ,String tableName, Handler<Boolean> resultHandler) {
        String sql = "SELECT name FROM sqlite_master " +
                "WHERE type='table' and name = '" + tableName + "'";
        try {
            JDBCSQLiteClientUtil.executeCommon(vertx, dbName, sql, data -> {
                if (data.size() > 0) {
                    resultHandler.handle(true);
                } else {
                    resultHandler.handle(false);
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static Connection getSQLiteConnection(String dbName) {
        Connection c = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:" + dbName + ".db");

        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        System.out.println("Opened database successfully");
        return c;
    }

    /**
     * 执行批量更新任务
     *
     * @param vertx
     * @param db_name
     * @param sqls
     * @param resultSetHandler
     * @throws Exception
     */
    public static void executeUpdateCommonBatch(Vertx vertx, String db_name, List<String> sqls, Handler<Integer> resultSetHandler) throws Exception {

        Connection conn = getSQLiteConnection(db_name);
        conn.setAutoCommit(false);
        Statement stmt = conn.createStatement();

        vertx.executeBlocking(future -> {
            try {
                //插入盘点任务
                for (int i = 0; i < sqls.size(); i++) {
                    String sql = sqls.get(i);
                    stmt.executeUpdate(sql);
                }
                conn.commit();

                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
                future.complete(sqls.size());
            } catch (SQLException e) {
                e.printStackTrace();
                future.fail(e.getMessage());
            }
        }, res -> {
            if (res.succeeded()) {
                resultSetHandler.handle((Integer) res.result());
            } else {
                System.out.println("The result is: " + res.result());
            }
        });
    }

    /**
     * 批量执行SQL
     * @param vertx
     * @param db_name
     * @param sqls
     * @param resultSetHandler
     * @throws SQLException
     */
    public static void executeSqls(Vertx vertx, String db_name, List<String> sqls, Handler<Boolean> resultSetHandler) throws SQLException {
        Statement stmt = getSQLiteConnection(db_name).createStatement();
        vertx.executeBlocking(future -> {
            try{
                for (String sql : sqls) {
                    stmt.addBatch(sql);
                }

                int[] rnum = stmt.executeBatch();
                stmt.close();
                future.complete(rnum);
            }catch(SQLException e){
                e.printStackTrace();
                future.fail(e.getMessage());
            }
        },res -> {
            if (res.succeeded()) {
                resultSetHandler.handle(true);
            } else {
                System.out.println("The result is: " + res.result());
            }
        });
    }

    /**
     * 执行update类型sql
     *
     * @param vertx
     * @param db_name
     * @param sql
     * @param resultSetHandler
     * @throws SQLException
     */
    public static void executeUpdateCommon(Vertx vertx, String db_name, String sql, Handler<Integer> resultSetHandler) throws SQLException {
        Statement stmt = getSQLiteConnection(db_name).createStatement();
        vertx.executeBlocking(future -> {
            try {
                Integer rnum = stmt.executeUpdate(sql);
                stmt.close();
                future.complete(rnum);
            } catch (SQLException e) {
                e.printStackTrace();
                future.fail(e.getMessage());
            }
        }, res -> {
            if (res.succeeded()) {
                resultSetHandler.handle((Integer) res.result());
            } else {
                System.out.println("The result is: " + res.result());
            }
        });
    }

    /**
     * /执行查询sql
     *
     * @param vertx
     * @param db_name
     * @param sql
     * @param resultHandler
     * @throws SQLException
     */
    public static void executeCommon(Vertx vertx, String db_name, String sql, Handler<List> resultHandler) throws SQLException {
        Statement stmt = getSQLiteConnection(db_name).createStatement();
        vertx.executeBlocking(future -> {
            try {
                ResultSet resultSet = stmt.executeQuery(sql);
                List data = JDBCResultSetUtil.resultSetToList(resultSet);
                stmt.close();
                future.complete(data);
            } catch (SQLException e) {
                e.printStackTrace();
                future.fail(e.getMessage());
            }
        }, res -> {
            if (res.succeeded()) {
                resultHandler.handle((List) res.result());
            } else {
                System.out.println("The result is: " + res.result());
            }
        });
    }
}
