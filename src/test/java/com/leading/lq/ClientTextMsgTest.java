package com.leading.lq;

import java.io.IOException;

import com.apm70.fileq.client.Client;
import com.apm70.fileq.client.publish.PublishState;
import com.apm70.fileq.util.ClientBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientTextMsgTest {

    public static void main(final String[] args) throws IOException {
        final ClientBuilder builder = new ClientBuilder();
        final Client client = builder
                .connKeepalivedSeconds(120 * 1000) // 长连接保持时间，超时无数据自动断开
                .ackTimeout(120 * 1000) // 数据包ACK确认的超时时间
                .storePath("/tmp/clientUpload") // 文件临时存储目录
                .binaryLogging(false) // 是否打印数据的二进制日志
                .reportHost("127.0.0.1")
                .reportPort(6666)
                .clientIp("127.0.0.1")
                .secretKey("apm70-Q")
                .build();

        // 注册服务监听，接收发布事件
        client.registerPublishStateListener((final PublishState state) -> {
            ClientTextMsgTest.log.info("收到发布状态变更事件: {}", state);
        });

        System.out.println("## 请输入文本消息，测试发布消息到服务器，输入 exit 终止程序 ##");
        final byte[] input = new byte[1024];
        int i = 0;
        while (true) {
            final int size = System.in.read(input);
            final String value = new String(input, 0, size).trim();
            if (value.equals("exit")) {
                break;
            }
            final String businessId = "file:businessId" + i++;
            client.publish(value, "txt-publish", businessId, "127.0.0.1", 6666);
        }
    }
}
