package com.kevin.netty.example;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * com.kevin.netty.example
 * Author: frhui
 * Date: 14-1-7 下午5:27
 */
public class DiscardClientHandler extends SimpleChannelInboundHandler<Object> {
    private static final Logger logger = Logger
            .getLogger(DiscardClientHandler.class.getName());

    private final int messageSize;
    private ByteBuf content;
    private ChannelHandlerContext ctx;

    public DiscardClientHandler(int messageSize) {
        if (messageSize <= 0) {
            throw new IllegalArgumentException("messageSize: " + messageSize);
        }
        this.messageSize = messageSize;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;

        //初始化信息
        content = ctx.alloc().directBuffer(messageSize).writeZero(messageSize);

        // 发送已经初始化的信息
        generateTraffic();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        content.release();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg)
            throws Exception {

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        // 关闭连接当抛出一个异常
        logger.log(Level.WARNING, "Unexpected exception from downstream.",
                cause);
        ctx.close();
    }

    long counter;

    private void generateTraffic() {
        //冲洗出站套接字的缓冲区。刷新后,再次生成相同数量的传送。
        ctx.writeAndFlush(content.duplicate().retain()).addListener(
                trafficGenerator);
    }

    private final ChannelFutureListener trafficGenerator = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                generateTraffic();
            }
        }
    };

}
