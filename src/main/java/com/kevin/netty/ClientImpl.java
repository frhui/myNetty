package com.kevin.netty;


import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.log4j.xml.DOMConfigurator;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;


public class ClientImpl implements Runnable {

    private String host = "";


    private short port;

    public static void main(String[] args) {
        try {
            DOMConfigurator.configure("res/log4j.xml");

            Properties properties = getProPertis("res/client.properties");
            int clientNum = Integer.valueOf(properties.getProperty("num"));
            String host = properties.getProperty("host");
            short port = Short.valueOf(properties.getProperty("port"));
            for (int i = 0; i != clientNum; ++i) {
                ClientImpl client = new ClientImpl();
                client.setHost(host);
                client.setPort(port);
                Thread thread = new Thread(client);
                thread.start();
            }

        } catch (Exception e) {
            //do nothing
        }

    }

    public static Properties getProPertis(String filePath) {
        InputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(new File(filePath));
            Properties serverSettings = new Properties();
            serverSettings.load(fileInputStream);
            fileInputStream.close();
            return serverSettings;
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return null;
        }
    }

    public void start() throws Exception {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup).channel(NioSocketChannel.class);      // NioSocketChannel is being used to create a client-side Channel.
            //Note that we do not use childOption() here unlike we did with
            // ServerBootstrap because the client-side SocketChannel does not have a parent.
            bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
            bootstrap.handler(new ClientChannelInitializer());

            // Bind and start to accept incoming connections.
            ChannelFuture channelFuture = bootstrap.connect(this.host, this.port);
            // Wait until the server socket is closed.
            // In this server, this does not happen, but you can do that to gracefully
            // shut down your CLIENT.
            Channel channel = channelFuture.channel();

            while (true) {
                ByteBuf buffer = PooledByteBufAllocator.DEFAULT.heapBuffer(10);
                buffer.writeShort(Short.MIN_VALUE);//包长占2字节
                buffer.writeByte(1);
                buffer.writeByte(0);
                buffer.setShort(0, buffer.writerIndex() - 0x2);
                channel.writeAndFlush(buffer);
                Thread.sleep(200);
            }


        } finally {
            workerGroup.shutdownGracefully();
        }

    }

    public short getPort() {
        return port;
    }

    public void setPort(short port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }


    private ByteBuf getWriteBuffer(int arg1, int arg2, ByteBuf buffer, Object... paras) {
        if (buffer == null) {
            buffer = PooledByteBufAllocator.DEFAULT.heapBuffer(10);
        }
        buffer.writeShort(Short.MIN_VALUE);//包长占2字节
        buffer.writeByte(arg1);
        if (arg2 != 0) buffer.writeByte(arg2);
        for (Object para : paras) {
            if (para instanceof Byte) {
                buffer.writeByte((Byte) para);  // 占1字节
            } else if ((para instanceof String)) {
                buffer.writeBytes(((String) para).getBytes());
            } else if (para instanceof Integer) {
                buffer.writeInt((Integer) para);    //占4字节
            } else if (para instanceof Short) {
                buffer.writeShort((Short) para);  //占2字节
            }
        }
        /**包长占2字节，setShort（）*/
        buffer.setShort(0, buffer.writerIndex() - 0x2);
        return buffer;
    }

    @Override
    public void run() {
        try {
            start();
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}