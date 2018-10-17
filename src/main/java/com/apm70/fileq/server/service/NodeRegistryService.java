package com.apm70.fileq.server.service;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.apm70.fileq.protocol.message.ProtocolMessage;
import com.apm70.fileq.protocol.message.Registry;
import com.apm70.fileq.util.ClientUtils;
import com.apm70.fileq.util.MD5Utils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;

/**
 * 结点注册服务
 * @author liuyg
 *
 */
public class NodeRegistryService {

	private static final long REGISTRY_TIMEOUT = 300000L;
	@Setter
	private String secretKey;
	/** 结点编号生成器 */
	@Setter
	private Function<Registry, String> nodeNoGetter;
	
    private final ConcurrentHashMap<String, NodeInfo> nodes = new ConcurrentHashMap<>();

    public Collection<NodeInfo> getAllNodes() {
        return this.nodes.values();
    }

    public List<NodeInfo> getActiveNodes() {
        return this.nodes.values().stream().filter(c -> c.isActive()).collect(Collectors.toList());
    }

    public List<NodeInfo> getInactiveNodes() {
        return this.nodes.values().stream().filter(c -> !c.isActive()).collect(Collectors.toList());
    }

    public void registry(final ChannelHandlerContext ctx, final ProtocolMessage msg) {
        final Registry registry = (Registry) msg.getBody();
        // TODO: 校验secret
        if (Math.abs(System.currentTimeMillis() - registry.getTimestamp()) > REGISTRY_TIMEOUT) {
        	    ClientUtils.sendFailureAck(ctx, msg.getMsgId(), "注册操作已经超时，请检查服务器时钟是否正确");
        	    return;
        }
        MD5Utils.verifySignature(registry.getSignature(), registry.getNetwork(), secretKey, registry.getTimestamp());
        // TODO: 分配服务器唯一编号
        NodeInfo info = this.nodes.get(registry.getNetwork());
        if (info == null) {
            final String no = nodeNoGetter.apply(registry);
            info = new NodeInfo(registry.getClientName(), registry.getNetwork(), no, ctx.channel());
            this.nodes.put(info.address, info);
        } else {
        	    // 校验是否同一个channel
        	    if (!info.getChannel().equals(ctx.channel()) && info.getChannel().isActive()) {
        	    	    // 踢掉旧的连接
        	      	info.getChannel().close();
        	      	info.setChannel(ctx.channel());
        	    }
            info.updateLatestActiveTime();
        }
        ClientUtils.setClientInfo(ctx.channel(), info);
        ClientUtils.sendSuccessAck(ctx, msg.getMsgId());
    }

    public static class NodeInfo {
        private static final long EXPIRED_TIME = 60000L;
        @Getter
        private final String name;
        @Getter
        private final String address;
        @Getter
        private final String nodeNo;
        @Getter @Setter
        private Channel channel;
        private long latestActiveTime;

        public NodeInfo(final String name, final String address, final String nodeNo, Channel channel) {
            this.name = name;
            this.address = address;
            this.nodeNo = nodeNo;
            this.channel = channel;
            this.latestActiveTime = System.currentTimeMillis();
        }

        public boolean isActive() {
            return (System.currentTimeMillis() - this.latestActiveTime) < EXPIRED_TIME;
        }

        public void updateLatestActiveTime() {
            this.latestActiveTime = System.currentTimeMillis();
        }
    }
}
