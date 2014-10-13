package com.kevin.netty.example;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * http://blog.csdn.net/chenxuegui1234/article/category/1805407
 * com.kevin.netty.example
 * Author: frhui
 * Date: 14-1-7 下午5:17
 */
public class DiscardServerHandler extends SimpleChannelInboundHandler {


    private static final Logger logger = Logger.getLogger(
            DiscardServerHandler.class.getName());

    //读channel中的msg，该例子是一个discard，所以直接摒弃就是了
    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf b = (ByteBuf) msg;
        b.release();
    }

    //当netty发生错误，执行该方法
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) throws Exception {
        // Close the connection when an exception is raised.
        logger.log(
                Level.WARNING,
                "Unexpected exception from downstream.",
                cause);
        ctx.close();
    }
}
