package com.kevin.netty;

import com.kevin.netty.ThreadUtils.PriorityThreadFactory;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;


@ChannelHandler.Sharable
public class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {

    private static final LoggingHandler LOGGING_HANDLER = new LoggingHandler();
    private static final EventExecutorGroup EVENT_EXECUTORS = new
            DefaultEventExecutorGroup(3, new PriorityThreadFactory("executionLogicHandlerThread+#", Thread.NORM_PRIORITY));

    /**
     * This method will be called once the {@link io.netty.channel.Channel} was registered. After the method returns this instance
     * will be removed from the {@link io.netty.channel.ChannelPipeline} of the {@link io.netty.channel.Channel}.
     *
     * @param ch the {@link io.netty.channel.Channel} which was registered.
     * @throws Exception is thrown if an error occurs. In that case the {@link io.netty.channel.Channel} will be closed.
     */
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("LOGGING_HANDLER", LOGGING_HANDLER);
        pipeline.addLast("decoder", new clientDecoder(20000, 0, 2, 0, 2));
        pipeline.addLast("handler", new clientHandler());
        pipeline.addLast("encoder", new clientEncoder());

    }
}