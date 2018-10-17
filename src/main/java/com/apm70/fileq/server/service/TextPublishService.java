package com.apm70.fileq.server.service;

import com.apm70.fileq.config.Constants;
import com.apm70.fileq.protocol.message.ProtocolMessage;
import com.apm70.fileq.protocol.message.Text;
import com.apm70.fileq.server.topic.MessageType;
import com.apm70.fileq.server.topic.TopicMessageBean;
import com.apm70.fileq.util.ClientUtils;

import io.netty.channel.ChannelHandlerContext;
import lombok.Setter;

public class TextPublishService {

    @Setter
    private TopicPublishService topicPublishService;

    public void handleTextPublish(final ChannelHandlerContext ctx, final ProtocolMessage msg) {
        final Text text = (Text) msg.getBody();
        ClientUtils.sendSuccessAck(ctx, msg.getMsgId());

        final TopicMessageBean bean = new TopicMessageBean();
        bean.setBusinessId(text.getBusinessId());
        bean.setTopic(text.getTopic());
        bean.setValue(new String(text.getPayload(), Constants.defaultCharset));
        bean.setMsgType(MessageType.TEXT);
        this.topicPublishService.publish(bean);
    }
}
