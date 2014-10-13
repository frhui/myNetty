package com.kevin.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.TooLongFrameException;

import java.nio.ByteOrder;
import java.util.List;


public class clientDecoder extends ByteToMessageDecoder {
    private final ByteOrder byteOrder;
    private final int maxFrameLength;
    private final int lengthFieldOffset;
    private final int lengthFieldLength;
    private final int lengthFieldEndOffset;
    private final int lengthAdjustment;
    private final int initialBytesToStrip;
    private final boolean failFast;
    private boolean discardingTooLongFrame;
    private long tooLongFrameLength;
    private long bytesToDiscard;

    /**
     * @param maxFrameLength
     * @param lengthFieldOffset
     * @param lengthFieldLength
     * @param lengthAdjustment
     * @param initialBytesToStrip
     */
    public clientDecoder(
            int maxFrameLength,
            int lengthFieldOffset, int lengthFieldLength,
            int lengthAdjustment, int initialBytesToStrip) {
        this(
                maxFrameLength,
                lengthFieldOffset, lengthFieldLength, lengthAdjustment,
                initialBytesToStrip, true);
    }

    /**
     * @param maxFrameLength
     * @param lengthFieldOffset
     * @param lengthFieldLength
     * @param lengthAdjustment
     * @param initialBytesToStrip
     * @param failFast
     */
    public clientDecoder(
            int maxFrameLength, int lengthFieldOffset, int lengthFieldLength,
            int lengthAdjustment, int initialBytesToStrip, boolean failFast) {
        this(
                ByteOrder.BIG_ENDIAN, maxFrameLength, lengthFieldOffset, lengthFieldLength,
                lengthAdjustment, initialBytesToStrip, failFast);
    }

    /**
     * @param byteOrder
     * @param maxFrameLength
     * @param lengthFieldOffset
     * @param lengthFieldLength
     * @param lengthAdjustment
     * @param initialBytesToStrip
     * @param failFast
     */
    public clientDecoder(
            ByteOrder byteOrder, int maxFrameLength, int lengthFieldOffset, int lengthFieldLength,
            int lengthAdjustment, int initialBytesToStrip, boolean failFast) {
        if (byteOrder == null) {
            throw new NullPointerException("byteOrder");
        }

        if (maxFrameLength <= 0) {
            throw new IllegalArgumentException(
                    "maxFrameLength must be a positive integer: " +
                            maxFrameLength);
        }

        if (lengthFieldOffset < 0) {
            throw new IllegalArgumentException(
                    "lengthFieldOffset must be a non-negative integer: " +
                            lengthFieldOffset);
        }

        if (initialBytesToStrip < 0) {
            throw new IllegalArgumentException(
                    "initialBytesToStrip must be a non-negative integer: " +
                            initialBytesToStrip);
        }

        if (lengthFieldLength != 1 && lengthFieldLength != 2 &&
                lengthFieldLength != 3 && lengthFieldLength != 4 &&
                lengthFieldLength != 8) {
            throw new IllegalArgumentException(
                    "lengthFieldLength must be either 1, 2, 3, 4, or 8: " +
                            lengthFieldLength);
        }

        if (lengthFieldOffset > maxFrameLength - lengthFieldLength) {
            throw new IllegalArgumentException(
                    "maxFrameLength (" + maxFrameLength + ") " +
                            "must be equal to or greater than " +
                            "lengthFieldOffset (" + lengthFieldOffset + ") + " +
                            "lengthFieldLength (" + lengthFieldLength + ").");
        }

        this.byteOrder = byteOrder;
        this.maxFrameLength = maxFrameLength;
        this.lengthFieldOffset = lengthFieldOffset;
        this.lengthFieldLength = lengthFieldLength;
        this.lengthAdjustment = lengthAdjustment;
        lengthFieldEndOffset = lengthFieldOffset + lengthFieldLength;
        this.initialBytesToStrip = initialBytesToStrip;
        this.failFast = failFast;
    }

    /**
     * decode message will be added the MessageList<Object> out  queue!
     *
     * @param ctx
     * @param in
     * @param out
     * @throws Exception 针对每一个channel 都会有一个相应的OutputMessageBuf消息缓存队列
     */
    @Override
    protected final void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        ByteBuf byteBuf = decode(ctx, in);
        if (byteBuf != null) {
            out.add(byteBuf);
        }
    }

    /**
     * return the  date of ByteBuf
     *
     * @param ctx
     * @param in
     * @return
     * @throws Exception
     */
    private ByteBuf decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        if (discardingTooLongFrame) {
            long bytesToDiscard = this.bytesToDiscard;
            int localBytesToDiscard = (int) Math.min(bytesToDiscard, in.readableBytes());
            in.skipBytes(localBytesToDiscard);
            bytesToDiscard -= localBytesToDiscard;
            this.bytesToDiscard = bytesToDiscard;
            failIfNecessary(ctx, false);
            return null;
        }

        if (in.readableBytes() < lengthFieldEndOffset) {
            return null;
        }

        int actualLengthFieldOffset = in.readerIndex() + lengthFieldOffset;

        /**获取数据包长*/
//        long frameLength = (in.order(byteOrder)).getUnsignedByte(actualLengthFieldOffset);
        long frameLength = (in.order(byteOrder)).getUnsignedShort(actualLengthFieldOffset);

        if (frameLength < 0) {
            in.skipBytes(lengthFieldEndOffset);
            throw new CorruptedFrameException(
                    "negative pre-adjustment length field: " + frameLength);
        }

        frameLength += lengthAdjustment + lengthFieldEndOffset;

        if (frameLength < lengthFieldEndOffset) {
            in.skipBytes(lengthFieldEndOffset);
            throw new CorruptedFrameException(
                    "Adjusted frame length (" + frameLength + ") is less " +
                            "than lengthFieldEndOffset: " + lengthFieldEndOffset);
        }

        if (frameLength > maxFrameLength) {
            // Enter the discard mode and discard everything received so far.
            discardingTooLongFrame = true;
            tooLongFrameLength = frameLength;
            bytesToDiscard = frameLength - in.readableBytes();
            in.skipBytes(in.readableBytes());
            failIfNecessary(ctx, true);
            return null;
        }

        // never overflows because it's less than maxFrameLength
        int frameLengthInt = (int) frameLength;
        if (in.readableBytes() < frameLengthInt) {
            return null;
        }

        if (initialBytesToStrip > frameLengthInt) {
            in.skipBytes(frameLengthInt);
            throw new CorruptedFrameException(
                    "Adjusted frame length (" + frameLength + ") is less " +
                            "than initialBytesToStrip: " + initialBytesToStrip);
        }
        in.skipBytes(initialBytesToStrip);

        // extract frame
        int readerIndex = in.readerIndex();
        int actualFrameLength = frameLengthInt - initialBytesToStrip;
        ByteBuf frame = extractFrame(in, readerIndex, actualFrameLength, ctx);
        in.readerIndex(readerIndex + actualFrameLength);
        return frame;
    }

    /**
     * 返回字节长度 byte  short
     * return  the length of pack
     *
     * @param in
     * @param actualLengthFieldOffset
     * @return
     */
    @Deprecated
    private long getFrameLength(ByteBuf in, int actualLengthFieldOffset) {
        in = in.order(byteOrder);
        return in.getUnsignedShort(actualLengthFieldOffset);//return  the length

    }

    /**
     * @param ctx
     * @param firstDetectionOfTooLongFrame
     */
    private void failIfNecessary(ChannelHandlerContext ctx, boolean firstDetectionOfTooLongFrame) {
        if (bytesToDiscard == 0) {
            // Reset to the initial state and tell the handlers that
            // the frame was too large.
            long tooLongFrameLength = this.tooLongFrameLength;
            this.tooLongFrameLength = 0;
            discardingTooLongFrame = false;
            if (!failFast ||
                    failFast && firstDetectionOfTooLongFrame) {
                fail(tooLongFrameLength);
            }
        } else {
            // Keep discarding and notify handlers if necessary.
            if (failFast && firstDetectionOfTooLongFrame) {
                fail(tooLongFrameLength);
            }
        }
    }

    /**
     * Extract the sub-region of the specified buffer.
     * <p/>
     * If you are sure that the frame and its content are not accessed after
     * the current {@link #decode(io.netty.channel.ChannelHandlerContext, io.netty.buffer.ByteBuf)}
     * call returns, you can even avoid memory copy by returning the sliced
     * sub-region (i.e. <tt>return buffer.slice(index, length)</tt>).
     * It's often useful when you convert the extracted frame into an object.
     * Refer to the source code of {@link io.netty.handler.codec.serialization.ObjectDecoder} to see how this method
     * is overridden to avoid memory copy.
     */
    protected ByteBuf extractFrame(ByteBuf buffer, int index, int length) {
        ByteBuf frame = Unpooled.buffer(length);
        frame.writeBytes(buffer, index, length);
        return frame;
    }

    /**
     * is overridden to avoid memory copy.
     * 使用Unpooled ByteBufs会造成沉重的分配与再分配问题,
     * 使用ChannelHandlerContext.alloc()或Channel.alloc()
     * 来获取ByteBufAllocator分配ByteBuf,从而减轻GC执行；
     *
     * @param buffer
     * @param index
     * @param length
     * @param ctx    ChannelHandlerContext
     * @return
     */
    protected ByteBuf extractFrame(ByteBuf buffer, int index, int length, ChannelHandlerContext ctx) {
        ByteBuf frame = ctx.alloc().buffer(length);
        frame.writeBytes(buffer, index, length);
        return frame;
    }

    /**
     * 解析数据，返回pack[]数组数据包
     *
     * @param buffer
     * @param index
     * @param length
     * @return byte[]
     */
    private byte[] extractFrameByteArray(ByteBuf buffer, int index, int length) {
        byte[] pack = new byte[length];
        buffer.readBytes(pack, index, length);
        return pack;
    }

    //logger fail
    private void fail(long frameLength) {
        if (frameLength > 0) {
            throw new TooLongFrameException(
                    "Adjusted frame length exceeds " + maxFrameLength +
                            ": " + frameLength + " - discarded");
        } else {
            throw new TooLongFrameException(
                    "Adjusted frame length exceeds " + maxFrameLength +
                            " - discarding");
        }
    }
}
