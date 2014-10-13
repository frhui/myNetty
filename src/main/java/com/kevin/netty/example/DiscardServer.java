package com.kevin.netty.example;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * com.kevin.netty.example
 * Author: frhui
 * Date: 14-1-7 下午5:13
 */
public class DiscardServer {

    private final int port;

    public DiscardServer(int port) {
        this.port = port;
    }

    public void run() throws Exception {
        // 创建两个EventLoopGroup,一个充当register处理器bossGroup，bossGroup把处理过的通道交给workerGroup进行io操作
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            // 一个服务器助手类
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    //用它来建立新accept的连接，用于构造serversocketchannel的工厂类
                    .channel(NioServerSocketChannel.class)
                            //在serverBootstrap内部用该handler去处理实例化的channel
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        //当有新accept的时候，这个方法会调用
                        @Override
                        public void initChannel(SocketChannel ch)
                                throws Exception {
                            ch.pipeline().addLast(new DiscardServerHandler());
                        }
                    });

            // 绑定并等待accept到来的连接
            ChannelFuture f = b.bind(port).sync();


            //关闭服务器
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        int port;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = 8080;
        }
        new DiscardServer(port).run();
    }
}


