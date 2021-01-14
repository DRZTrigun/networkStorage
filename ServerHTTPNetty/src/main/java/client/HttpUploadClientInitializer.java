package client;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;

public class HttpUploadClientInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslContext;

    public HttpUploadClientInitializer(SslContext sslContext) {
        this.sslContext = sslContext;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        if (sslContext != null){
            pipeline.addLast("ssl", sslContext.newHandler(ch.alloc()));
        }
        pipeline.addLast("codec", new HttpClientCodec());

        pipeline.addLast("chuckWriter", new ChunkedWriteHandler());
        pipeline.addLast("handler", new HttpUploadClientHandler());
    }
}
