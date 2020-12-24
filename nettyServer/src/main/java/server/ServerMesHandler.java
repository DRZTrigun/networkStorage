package server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

class ServerMesHandler extends ChannelInboundHandlerAdapter {

    private State currentState = State.IDLE;        // устанавливаем в состояние ожидания
    private int nextLength;
    private long fileLenght;
    private long receiveFileLenght;
    private BufferedOutputStream outputStream;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = ((ByteBuf) msg);
        while (buf.readableBytes() > 0) {
            if (currentState == State.IDLE) {
                byte readed = buf.readByte();
                if (readed == (byte) 15) {
                    currentState = State.NAME_LENGTH;      // меняем состояние на определение длины названия файла
                    receiveFileLenght = 0L;
                    System.out.println();
                } else {
                    System.out.println("ERROR:" + readed);
                }
            }

            if (currentState == State.NAME_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    System.out.println();
                    nextLength = buf.readInt();
                    currentState = State.NAME;         // меняем состояние на имя файла
                }
            }

            if (currentState == State.NAME){
                if (buf.readableBytes() >= nextLength){
                    byte[] fileName = new byte[nextLength];
                    buf.readBytes(fileName);
                    System.out.println("STATE: Filename received - _" + new String(fileName, "UTF-8"));
                    outputStream = new BufferedOutputStream(new FileOutputStream("_" + new String(fileName)));
                    currentState = State.FILE_LENGTH;      // меняем состояние на определение длины файла
                }
            }

            if (currentState == State.FILE_LENGTH){
                if (buf.readableBytes() >= 8) {
                    fileLenght = buf.readLong();
                    System.out.println();
                    currentState = State.FILE;
                }
            }

            if (currentState == State.FILE){
                while (buf.readableBytes() > 0){
                    outputStream.write(buf.readByte());
                    receiveFileLenght++;
                    if (fileLenght == receiveFileLenght){
                        currentState = State.IDLE;
                        System.out.println("File received");
                        outputStream.close();
                        break;
                    }
                }
            }
        }
        if (buf.readableBytes() == 0) {
            buf.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
