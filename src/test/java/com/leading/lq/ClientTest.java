package com.leading.lq;

import java.io.File;
import java.io.IOException;

import com.apm70.fileq.client.Client;
import com.apm70.fileq.client.publish.PublishState;
import com.apm70.fileq.util.ClientBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientTest {

    public static void main(final String[] args) throws IOException {
        final ClientBuilder builder = new ClientBuilder();
        final Client client = builder
                .connKeepalivedSeconds(120 * 1000) // 长连接保持时间，超时无数据自动断开
                .ackTimeout(120 * 1000) // 数据包ACK确认的超时时间
                .storePath("/tmp/clientUpload") // 文件临时存储目录
                .binaryLogging(false) // 是否打印数据的二进制日志
                // ============== 监控上报服务配置，如果启动了中心端服务的话 =============
                .monitorReport(false) 
                //.reportHost("127.0.0.1")// 上报服务地址
                //.reportPort(6666) // 上报服务端口
                //.clientIp("127.0.0.1")// 客户端IP
                //.secretKey("apm70-Q")// 连接上报服务的安全密钥
                // ===================== 监控上报服务配置 ===========================
                .build();

        // 注册服务监听，接收发布事件
        client.registerPublishStateListener((final PublishState state) -> {
            ClientTest.log.info("收到发布状态变更事件: {}", state);
        });

        System.out.println("## 请输入完整的文件路径，测试发布文件到服务器 ##");
        // 控制台输入文件路径，测试文件发布
        final byte[] input = new byte[512];
        int i = 0;
        while (true) {
            final int size = System.in.read(input);
            final String value = new String(input, 0, size).trim();
            if (value.equals("exit")) {
                break;
            }
            final File file = new File(value);
            if (file.exists() && !file.isDirectory()) {
                final String businessId = "file:businessId" + i++;
                // 发布文件到[127.0.0.1:6666] 端点
                client.publishFile(file, "file-publish", businessId, "127.0.0.1", 6666);
            } else {
                System.out.println("无效的文件地址：" + value);
            }
        }
    }
}
