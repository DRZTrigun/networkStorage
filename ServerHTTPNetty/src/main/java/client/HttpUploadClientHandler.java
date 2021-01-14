package client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

public class HttpUploadClientHandler extends SimpleChannelInboundHandler<HttpObject> {

    private boolean readingChunks;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpResponse){
            HttpResponse response = (HttpResponse) msg;
            System.err.println("status: " + response.status() + " version: " + response.protocolVersion());
            if (!response.headers().isEmpty()){
                for (String name : response.headers().names()) {
                    for (String value : response.headers().getAll(name)) {
                        System.err.println("Header: " + name + " = " + value);
                    }
                }
            }
            if (response.status().code() == 200 && HttpUtil.isTransferEncodingChunked(response)){
                readingChunks = true;
                System.err.println("Chunked Content {");
            } else {
                System.err.println("content {");
            }
        }
        if (msg instanceof HttpContent){
            HttpContent chunk = (HttpContent) msg;
            System.err.println(chunk.content().toString(CharsetUtil.UTF_8));

            if (chunk instanceof LastHttpContent){
                if (readingChunks){
                    System.err.println("} END OF CHUNKED CONTENT");
                } else {
                    System.err.println("} END OF CONTENT");
                }
                readingChunks = false;
            } else {
                System.err.println(chunk.content().toString(CharsetUtil.UTF_8));
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.channel().close();
    }
}