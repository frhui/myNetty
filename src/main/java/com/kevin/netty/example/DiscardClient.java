package com.kevin.netty.example;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * com.kevin.netty.example
 * Author: frhui
 * Date: 14-1-7 下午5:26
 */
public class DiscardClient {
    private final String host;
    private final int port;
    private final int firstMessageSize;

    public DiscardClient(String host, int port, int firstMessageSize)
    {
        this.host = host;
        this.port = port;
        this.firstMessageSize = firstMessageSize;
    }

    public void run() throws Exception
    {
        EventLoopGroup group = new NioEventLoopGroup();
        try
        {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class)
                    .handler(new DiscardClientHandler(firstMessageSize));

            // 尝试建立连接
            ChannelFuture f = b.connect(host, port).sync();

            // 等待直到连接断开
            f.channel().closeFuture().sync();
        } finally
        {
            group.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception
    {
        if (args.length < 2 || args.length > 3)
        {
            System.err.println("Usage: " + DiscardClient.class.getSimpleName()
                    + " <host> <port> [<first message size>]");
            return;
        }

        // Parse options.
        final String host = args[0];
        final int port = Integer.parseInt(args[1]);
        final int firstMessageSize;
        if (args.length == 3)
        {
            firstMessageSize = Integer.parseInt(args[2]);
        }
        else
        {
            firstMessageSize = 256;
        }

        new DiscardClient(host, port, firstMessageSize).run();
    }

}
