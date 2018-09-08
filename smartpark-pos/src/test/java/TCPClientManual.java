import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;

/**
 * Created by Administrator on 2016/5/19.
 */
public class TCPClientManual {
    public static void main(String arg[]) {
        Vertx vertx = Vertx.vertx();
        NetClientOptions options = new NetClientOptions().setConnectTimeout(10000)
                .setReconnectAttempts(10).//重连次数
                setReconnectInterval(500);//重连间隔
        NetClient client = vertx.createNetClient(options);
        client.connect(5000, "192.168.1.10", res -> {
            NetSocket socket = res.result();
            socket.handler(buffer -> {
                buffer.getFloat(0);
                buffer.getInt(4);
                //System.out.println("Net client receiving: " + buffer.toString("UTF-8"));
            });

            // Now send some data
            for (int i = 0; i < 10; i++) {
                String str = "hello " + i + "\n";
                //  System.out.println("Net client sending: " + str);
                socket.write(str);
            }
        });
    }

    /**
     * 利用多个核心
     */
    public static void main2() {

    }
}

