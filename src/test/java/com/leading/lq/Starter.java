package com.leading.lq;

import java.io.File;
import java.io.IOException;

import com.apm70.fileq.client.Client;
import com.apm70.fileq.client.publish.PublishState;
import com.apm70.fileq.server.Server;
import com.apm70.fileq.server.topic.TopicSubscriber;
import com.apm70.fileq.server.topic.TopicMessageBean;
import com.apm70.fileq.util.ClientBuilder;
import com.apm70.fileq.util.ServerBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * Jar包的启动入口类，方便通过jar包直接启动服务来简单测试
 * @author liuyg
 *
 */
@Slf4j
public class Starter {
	
	/**
	 * 服务启动入口
	 * @param args 启动参数
	 * @throws IOException
	 */
    public static void main(final String[] args) throws IOException {
        final String runMod = args[0];
        if (runMod.equals("client")) {
            startClient(args);
        } else {
            startServer(args);
        }
    }

    private static void startClient(final String[] args) throws IOException {
        final String tmpFilePath = args[1];
        final String servereAddress = args[2];
        final ClientBuilder builder = new ClientBuilder();
        final Client client = builder
                .connKeepalivedSeconds(120 * 1000) // 长连接保持时间，超时无数据自动断开
                .ackTimeout(120 * 1000) // 数据包ACK确认的超时时间
                .storePath(tmpFilePath) // 文件临时存储目录
                .binaryLogging(false) // 是否打印数据的二进制日志
                .build();

        // 注册服务监听，接收发布事件
        client.registerPublishStateListener((final PublishState state) -> {
            log.info("收到发布状态变更事件: {}", state);
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
                client.publishFile(file, "file-publish", businessId, servereAddress, 6666);
            } else {
                System.out.println("无效的文件地址：" + value);
            }
        }

    }

    private static void startServer(final String[] args) {
        final String storePath = args[1];
        final String limit = args.length >= 3 ? args[2] : "-1";

        final ServerBuilder builder = new ServerBuilder();

        try {
            final Server server = builder.bind("0.0.0.0", 6666)
                    .connKeepalivedSeconds(120 * 1000)
                    .storePath(storePath)
                    .binaryLogging(false)
                    .topicMsgKeepalivedFragments(20)
                    .topicMsgKeepalivedHours(24 * 2)
                    .serverInflowSpeedLimit(Integer.parseInt(limit)) // 限速
                    .build();
            // 启动服务
            server.start();

            // 消息订阅者开始消费数据
            final TopicSubscriber subscriber = server.newTopicSubscriber();
            long topicIndex = 0;
            while (true) {
                final TopicMessageBean msg = subscriber.consume(topicIndex);
                if (msg == null) {
                    Thread.sleep(5000L);
                    continue;
                }
                topicIndex++;
                System.out.println("收到订阅的消息：" + msg.toString());
            }
        } catch (final IOException e1) {
            // IO异常，服务启动失败
            throw new RuntimeException("IO异常，服务启动失败", e1);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
