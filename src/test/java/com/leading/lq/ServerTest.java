package com.leading.lq;

import java.io.IOException;

import com.apm70.fileq.server.Server;
import com.apm70.fileq.server.topic.TopicSubscriber;
import com.apm70.fileq.server.topic.TopicMessageBean;
import com.apm70.fileq.util.ServerBuilder;

/**
 * 服务端测试（文件/消息接收）
 * @author liuyg
 *
 */
public class ServerTest {

    public static void main(final String[] args) {
        final ServerBuilder builder = new ServerBuilder();
        Server server;
        try {
            server = builder.bind("0.0.0.0", 6666)
                    .connKeepalivedSeconds(120 * 1000)
                    .storePath("/tmp/serverUpload")
                    .binaryLogging(false)
                    .topicMsgKeepalivedFragments(20)
                    .topicMsgKeepalivedHours(24 * 2)
                    .secretKey("apm70-Q")
                    //.clientNoSupplier(clientNoSupplier) // 客户端NO分配器，由业务提供
                    //.serverInflowSpeedLimit(1024) // 限速每秒最大接收 1024KB
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
