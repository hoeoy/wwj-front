package com.iandtop.front.smartpark.pos;

import com.iandtop.common.utils.CommonFileUtil;
import com.iandtop.front.smartpark.pos.server.TCPServerManual;
import com.iandtop.front.smartpark.pos.util.PosConstants;
import com.iandtop.front.smartpark.pub.vo.DBParamVO;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.NetServer;

import java.io.File;
import java.util.Properties;

public class Starter {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        String dirPath = System.getProperty("user.home")+ File.separator+"frontlog"+File.separator+"pos" ;
        CommonFileUtil.createDir(dirPath);

        Logger logger = LoggerFactory.getLogger(Starter.class);
        logger.info("消费前置机开启");

        Properties p = CommonFileUtil.getPropertyFile(CommonFileUtil.baseDir + "\\smartpark-pos.properties");
        String port = p.getProperty("tcpserverport");//tcp server端口

        DBParamVO vo = new DBParamVO();
        vo.setDburl(p.getProperty("dburl"));
        vo.setDbport(p.getProperty("dbport"));
        vo.setDbsid(p.getProperty("dbsid"));
        vo.setDbuser(p.getProperty("dbuser"));
        vo.setDbpassword(p.getProperty("dbpassword"));

        PosConstants.vo = vo;
        PosConstants.server_port = Integer.parseInt(port);

        new TCPServerManual().startTCPServer(vertx);
    }

    /**
     * 利用多个核心
     */
    public static void main2() {
        //TODO vertx的集群配置，消费系统的稳定性
        Vertx vertx = Vertx.vertx();
        for (int i = 0; i < 10; i++) {
            NetServer server = vertx.createNetServer();
            server.connectHandler(socket -> {
                socket.handler(buffer -> {
                    // Just echo back the data
                    socket.write(buffer);
                });
            });
            server.listen(1234, "localhost");
        }
    }
}

