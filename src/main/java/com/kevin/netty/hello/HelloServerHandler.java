package com.kevin.netty.hello;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.InetAddress;

/**
 * com.kevin.netty.hello
 * Author: frhui
 * Date: 2014/5/28 9:20
 */
public class HelloServerHandler extends SimpleChannelInboundHandler {


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 收到消息直接打印输出
        System.out.println(ctx.channel().remoteAddress() + " Say " + msg);
        // 返回客户端消息 - 我已经接收到了你的消息
        ctx.writeAndFlush("Received your message !\n");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
       System.out.print("RamoteAddress:"+ctx.channel().remoteAddress() +" active");
        ctx.writeAndFlush("Welcome to "+ InetAddress.getLocalHost().getHostName()+" service\n");
        super.channelActive(ctx);
    }
}
