import com.alibaba.fastjson.JSONObject;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.zip.GZIPInputStream;

/**
 *  火币网 WebSocket 的 K 线数据获取
 *  文档地址 https://huobiapi.github.io/docs/spot/v1/cn/#k-2
 * */
public class WebsocketClient {

    public static WebSocketClient client;

    /* 主方法 */
    public static void main(String[] args) throws InterruptedException {
        try {
            client = new WebSocketClient(new URI("wss://api.huobi.pro/ws"),new Draft_6455()) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    System.out.println("<<<握手-成功>>>");
                }
                @Override
                public void onMessage(String msg) {
                    System.out.println("<<<收到-消息>>>"+msg);
                }
                @Override
                public void onMessage(ByteBuffer bytes) {
                    try {
                        String message = new String(ZipUtil.解压GZIP数据(bytes.array()), "UTF-8");/* 解开GZIP压缩 */
                        JSONObject jsonObject = JSONObject.parseObject(message);/* 转换成Json */
                        if(null != jsonObject.getString("ping")){/* 判断是不是Ping */
                            System.out.println("收到的消息：" + message);/* 收到Ping消息 */
                            JSONObject json = new JSONObject();
                            json.put("pong",获取秒数());
                            System.out.println("发送消息：" + json.toString());
                            send(json.toString());/* 发送Pong回应 */
                        }else{
                            System.err.println("收到的消息：" + message);
                            JSONObject tick = jsonObject.getJSONObject("tick");
                            Double p = tick.getDouble("close");
                            System.out.println("===》 "+p+" 《===");
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                @Override
                public void onClose(int i, String s, boolean b) {
                    System.out.println("<<<连接-关闭>>>");
                }
                @Override
                public void onError(Exception e){
                    e.printStackTrace();
                    System.out.println("<<<错误-关闭>>>");
                }
                long 获取秒数() {
                    return Instant.now().getEpochSecond();
                }
            };
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }



        client.connect();
        System.out.println("正在连接");
        while(!client.getReadyState().equals(WebSocket.READYSTATE.OPEN)){
            System.out.print(".");
            Thread.sleep(500);
        }
        System.out.println();



        /*连接成功,发送信息*/
        JSONObject json = new JSONObject();
        json.put("sub","market.btcusdt.kline.1min");
        json.put("id","id1");
        System.out.println("发送消息：" + json.toString());
        client.send(json.toString());
    }
}

/* Zip工具类 */
class ZipUtil {
    public static byte[] 解压GZIP数据(byte[] depressData) throws Exception {
        ByteArrayInputStream is = new ByteArrayInputStream(depressData);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        GZIPInputStream gis = new GZIPInputStream(is);
        int count;
        byte data[] = new byte[1024];
        while ((count = gis.read(data, 0, 1024)) != -1) {
            os.write(data, 0, count);
        }
        gis.close();
        depressData = os.toByteArray();
        os.flush();
        os.close();
        is.close();
        return depressData;
    }
}