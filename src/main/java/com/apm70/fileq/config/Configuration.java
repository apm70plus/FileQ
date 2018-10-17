package com.apm70.fileq.config;

import java.util.function.Function;

import com.alibaba.fastjson.serializer.DoubleSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.apm70.fileq.protocol.message.Registry;
import com.apm70.fileq.server.NodeLogsCollector;
import com.apm70.fileq.server.MonitorDataCollector;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Configuration {

    static {
        SerializeConfig.getGlobalInstance().put(Double.class, new DoubleSerializer("0.0##"));
    }

    /** 连接保持时长（秒） */
    private int connKeepalivedSeconds;
    /** 是否输出数据的二进制日志 */
    private boolean binaryLogging;

    /** 服务端口 */
    private int serverPort;
    /** 服务监听的地址（默认 0.0.0.0） */
    private String serverHost;
    /** 服务端存储根目录 */
    private String serverStorePath;
    /** 订阅消息存储保留的分段数量（每段65536条消息） */
    private int topicMsgKeepalivedFragments;
    /** 订阅消息存储保留的时间（小时） */
    private int topicMsgKeepalivedHours;
    /** 服务端接收数据流速限制（KB/S） */
    private int serverInflowSpeedLimit;
    /** 客户端监控指标采集 */
    private MonitorDataCollector monitorDataCollector;
    /** 客户端日志采集 */
    private NodeLogsCollector clientLogsCollector;
    /** 根据客户端注册信息，提供一个客户端唯一的标识 */
	private Function<Registry, String> clientNoSupplier;

    /** 客户端存储根目录 */
    private String clientStorePath;
    /** 客户端文件保留时长（小时）， 超过时间自动销毁 */
    private int clientFileKeepalivedHours;
    /** Ack确认消息超时间隔（毫秒） **/
    private int ackTimeout;
    /** 是否上报监控数据 */
    private boolean monitorReport;

    private String reportHost;
    private int reportPort;
    private String clientIp;
    private String secretKey;
}
