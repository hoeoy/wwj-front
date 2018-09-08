package com.iandtop.front.smartpark.pub.utils;

import com.iandtop.front.smartpark.pub.vo.DBParamVO;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;

/**
 * oracle链接工具类
 *
 * @author andyzhao
 */
public class JDBCOracleClientUtil {
    private static JDBCClient jdbcClient = null;

    public static JDBCClient getOracleJDBCClient(Vertx vertx, DBParamVO vo) {
        if (jdbcClient == null) {
            synchronized (JDBCOracleClientUtil.class) {
                if (jdbcClient == null) {
                    jdbcClient = JDBCClient.createShared(vertx, new JsonObject()
//                            .put("url", "jdbc:oracle:thin:@PC201603142051:1521:orcl")
//                            .put("driver_class", "oracle.jdbc.driver.OracleDriver")
//                            .put("user", "smartpark")
//                            .put("password", "smartpark")
                            .put("url", "jdbc:oracle:thin:@" + vo.getDburl() + ":" + vo.getDbport() + ":" + vo.getDbsid())
                            .put("driver_class", "oracle.jdbc.driver.OracleDriver")
                            .put("user", vo.getDbuser())
                            .put("password", vo.getDbpassword())
                            .put("max_pool_size", 30));
                }
            }
        }

        return jdbcClient;
    }

    public static void executeUpdateCommon(Vertx vertx, DBParamVO vo, String sql, Handler<UpdateResult> resultSetHandler) {
        getOracleJDBCClient(vertx, vo).getConnection(conn -> {
            if (conn.failed()) {
                System.err.println(conn.cause().getMessage());
                return;
            }
            final SQLConnection connection = conn.result();

            execute(connection, sql, r->{
                connection.close(d -> {
                    if (d.failed()) {
                        throw new RuntimeException(d.cause());
                    }
                    resultSetHandler.handle(r);
                });
            });
        });
    }

    public static void execute(SQLConnection connection, String sql, Handler<UpdateResult> resultSetHandler) {
        // query some data with arguments
        connection.execute(sql, rs -> {
            if (rs.succeeded()) {
                resultSetHandler.handle(null);
            } else {
                System.err.println("Cannot retrieve the data from the database");
                rs.cause().printStackTrace();
                return;
            }
        });
    }

    public static void executeCommon(Vertx vertx, DBParamVO vo, String sql, JsonArray params, Handler<ResultSet> resultSetHandler) {
        JDBCOracleClientUtil.getOracleJDBCClient(vertx, vo).getConnection(conn -> {
            if (conn.failed()) {
                System.err.println(conn.cause().getMessage());
                return;
            }
            final SQLConnection connection = conn.result();

            // query some data with arguments
            connection.queryWithParams(sql, params, rs -> {
                if (rs.failed()) {
                    System.err.println("Cannot retrieve the data from the database");
                    rs.cause().printStackTrace();
                    return;
                }

                resultSetHandler.handle(rs.result());

                // and close the connection
                connection.close(done -> {
                    if (done.failed()) {
                        throw new RuntimeException(done.cause());
                    }
                });
            });
        });
    }

    public static void executeUpdateTX(Vertx vertx, DBParamVO vo, Handler<TXHandlerVO> taskHandler, Handler<Boolean> resultHandler) {
        getOracleJDBCClient(vertx, vo).getConnection(conn -> {
            if (conn.failed()) {
                System.err.println(conn.cause().getMessage());
                return;
            }
            final SQLConnection connection = conn.result();
            startTx(connection, sr -> {
                TXHandlerVO posHandlerVO = new TXHandlerVO();
                posHandlerVO.setConnection(connection);
                posHandlerVO.setResultHandler(r->{
                    if (r) {//如果成功结束
                        endTx(connection, enTxR -> {
                            resultHandler.handle(true);
                        });
                    } else {
                        rollbackTx(connection, roolbackTR -> {
                            resultHandler.handle(false);
                        });
                    }
                });
                taskHandler.handle(posHandlerVO);
            });
        });
    }

    private static void startTx(SQLConnection conn, Handler<ResultSet> done) {
        conn.setAutoCommit(false, res -> {
            if (res.failed()) {
                throw new RuntimeException(res.cause());
            }

            done.handle(null);
        });
    }

    private static void endTx(SQLConnection conn, Handler<Boolean> done) {
        conn.commit(res -> {
            if (res.failed()) {
                throw new RuntimeException(res.cause());
            }

            conn.close(d -> {
                if (d.failed()) {
                    throw new RuntimeException(d.cause());
                }
            });
            done.handle(true);
        });
    }

    private static void rollbackTx(SQLConnection conn, Handler<Boolean> done) {
        conn.rollback(res -> {
            if (res.failed()) {
                throw new RuntimeException(res.cause());
            }
            conn.close(d -> {
                if (d.failed()) {
                    throw new RuntimeException(d.cause());
                }
            });
            done.handle(true);
        });
    }

    /**
     * 执行事务，储存数据的vo
     */
    public static class TXHandlerVO {
        private SQLConnection connection;
        private Handler<Boolean> resultHandler;

        public Handler<Boolean> getResultHandler() {
            return resultHandler;
        }

        public void setResultHandler(Handler<Boolean> resultHandler) {
            this.resultHandler = resultHandler;
        }

        public SQLConnection getConnection() {
            return connection;
        }

        public void setConnection(SQLConnection connection) {
            this.connection = connection;
        }
    }
}

