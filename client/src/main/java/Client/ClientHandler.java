package Client;


import io.netty.channel.ChannelOutboundHandlerAdapter;

import java.util.concurrent.CountDownLatch;

public class ClientHandler extends ChannelOutboundHandlerAdapter {

    private final ClientHandler clientHandler;

    public ClientHandler(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
    }

    CountDownLatch NettyClient = new CountDownLatch(1);
    ClientHandler clientHandler = new ClientHandler(NettyClient);



}
