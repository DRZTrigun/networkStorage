package client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.internal.SocketUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpMethod.POST;

public class HttpUploadClient {

    static final String Base_URL = System.getProperty("baseUrl", "http://127.0.0.1:8888/");
    static final String FILE = System.getProperty("file", "C:\\torent\\lesson2.MP4");

    public static void main(String[] args) throws Exception{
        String postSimple, postFile, get;
        if (Base_URL.endsWith("/")){
            postSimple = Base_URL + "formpost";
            postFile = Base_URL + "formpostmultipart";
            get = Base_URL + "formget";
        } else {
            postSimple = Base_URL + "/formpost";
            postFile = Base_URL + "/formpostmultipart";
            get = Base_URL + "/formget";
        }

        URI uriSimple = new URI(postSimple);
        String scheme = uriSimple.getScheme() == null? "http" : uriSimple.getScheme();
        String host = uriSimple.getHost() == null? "127.0.0.1" : uriSimple.getHost();
        int port = uriSimple.getPort();
        if (port == -1){
            if ("http".equalsIgnoreCase(scheme)){
                port = 80;
            } else if ("https".equalsIgnoreCase(scheme)){
                port = 443;
            }
        }

        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)){
            System.err.println("Only HTTP(S) is supported.");
            return;
        }

        final boolean ssl = "https".equalsIgnoreCase(scheme);
        final SslContext sslCtx;
        if (ssl){
            sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }

        URI uriFile = new URI(postFile);
        File file = new File(FILE);
        if (!file.canRead()){
            throw new FileNotFoundException(FILE);
        }

        // конфигурация клиента
        EventLoopGroup group = new NioEventLoopGroup();

        // настройка фабрики
        HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE);

        DiskFileUpload.deleteOnExitTemporaryFile = true;
        DiskFileUpload.baseDirectory = null;
        DiskAttribute.deleteOnExitTemporaryFile = true;
        DiskAttribute.baseDirectory = null;

        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class).handler(new HttpUploadClientInitializer(sslCtx));

            // возможно нужен switch case
            // Простой запрос Get
            List<Map.Entry<String, String>> headers = formget(b, host, port, get, uriSimple);
            if (headers == null){
                factory.cleanAllHttpData();
                return;
            }

            // Simple Post form:
//            List<InterfaceHttpData> bodylist = formpost(b, host, port, uriSimple, file, factory, headers);
//            if (bodylist == null){
//                factory.cleanAllHttpData();
//                return;
//            }

            // Multipart Post form:
            formpostmultipart(b, host, port, file, uriFile, factory, headers);

        } finally {
            // закрыие потоков
            group.shutdownGracefully();
            // очистка временных файлов
            factory.cleanAllHttpData();
        }
    }

    private static List<Map.Entry<String, String>>
    formget(Bootstrap b, String host, int port, String get, URI uriSimple) throws Exception {

        Channel channel = b.connect(host, port).sync().channel();
//        // подготовка HTTP запроса
        QueryStringEncoder encoder = new QueryStringEncoder(get);

        encoder.addParam("getform", "GET");
        encoder.addParam("info", "first value");
        encoder.addParam("secondinfo", "secondvalue ���&");
        // not the big one since it is not compatible with GET size
        // encoder.addParam("thirdinfo", textArea);
//        encoder.addParam("thirdinfo", "third value\r\ntest second line\r\n\r\nnew line\r\n");
//        encoder.addParam("Send", "Send");

        URI uriGet = new URI(encoder.toString());
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uriGet.toASCIIString());
        HttpHeaders headers = request.headers();
        headers.set(HttpHeaderNames.HOST, host);
        headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        headers.set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP + " , " + HttpHeaderValues.DEFLATE);

        headers.set(HttpHeaderNames.ACCEPT_CHARSET, "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        headers.set(HttpHeaderNames.ACCEPT_LANGUAGE, "fr");
        headers.set(HttpHeaderNames.REFERER, uriSimple.toString());
        headers.set(HttpHeaderNames.USER_AGENT, "Netty Simple Http Client side");
        headers.set(HttpHeaderNames.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        // отправляем запрос
        List<Map.Entry<String, String>> entries = headers.entries();
        channel.writeAndFlush(request);
//        // ожидаем закрытие соединения с сервером
        channel.closeFuture().sync();
//
        return entries;
    }

    // Простой Post запрос основанный на factory
    private static List<InterfaceHttpData> formpost(
            Bootstrap b, String host, int port, URI uriSimple,
            File file, HttpDataFactory factory, List<Map.Entry<String, String>> headers) throws Exception {
        // старт подключения
        ChannelFuture future = b.connect(SocketUtils.socketAddress(host, port));
        // ожидания подключения
        Channel channel = future.sync().channel();
        // HTTP запрос
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, POST, uriSimple.toASCIIString());
        // используем кодировщик BODY
        HttpPostRequestEncoder bodyRequestEncoder = new HttpPostRequestEncoder(factory, request, false);

        for (Map.Entry<String, String> entry : headers) {
            request.headers().set(entry.getKey(), entry.getValue());
        }
        // если надо то добавляем атрибуты
        bodyRequestEncoder.addBodyAttribute("getform", "POST");
        bodyRequestEncoder.addBodyAttribute("info", "first value");
        bodyRequestEncoder.addBodyAttribute("secondinfo", "secondvalue ���&");
        bodyRequestEncoder.addBodyAttribute("thirdinfo", textArea);
        bodyRequestEncoder.addBodyAttribute("fourthinfo", textArealong);
        bodyRequestEncoder.addBodyFileUpload("myfile ", file, "application/x-zip-compressed", false);

        // завершение запроса
        request = bodyRequestEncoder.finalizeRequest();

        List<InterfaceHttpData> bodylist = bodyRequestEncoder.getBodyListAttributes();

        // отправляем запрос
        channel.write(request);
        // проверка не разбит ли запрос на части, если да то завершаем запись
        if (bodyRequestEncoder.isChunked()){
            channel.write(bodyRequestEncoder);
        }
        channel.flush();

        // очищаем файлы после запроса
        bodyRequestEncoder.cleanFiles();

        // ждем закрытия соединения с сервером
        channel.closeFuture().sync();
        return bodylist;
    }

    private static void formpostmultipart(
            Bootstrap b, String host, int port, File file, URI uriFile, HttpDataFactory factory, List<Map.Entry<String, String>> headers) throws Exception {
        // начинаем соединение с сервером
        ChannelFuture future = b.connect(SocketUtils.socketAddress(host, port));
        // ждем когда будет установлено соединение
        Channel channel = future.sync().channel();
        // подготавливаем запрос
        HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST,  uriFile.toASCIIString());
        // используем кодировщик BODY
        HttpPostRequestEncoder bodyRequestEncoder = new HttpPostRequestEncoder(factory, request, true);

        for (Map.Entry<String, String> entry: headers) {
            request.headers().set(entry.getKey(), entry.getValue());
        }
        bodyRequestEncoder.addBodyAttribute("postform", "POST");
        bodyRequestEncoder.addBodyFileUpload("myfile ", file, "application/x-ups-binary", false);


        // завершение запроса
        bodyRequestEncoder.finalizeRequest();

        // отправляем запрос
        channel.write(request);
        // проверка не разбит ли запрос на части, если да то завершаем запись
        if (bodyRequestEncoder.isChunked()){
            channel.write(bodyRequestEncoder);
        }
        channel.flush();
        // очащием файлы
        bodyRequestEncoder.cleanFiles();
        // ждем закрытия соединения
        channel.closeFuture().sync();
    }

    private static final String textArea = "short text";

    private static final String textArealong =
            "lkjlkjlKJLKJLKJLKJLJlkj lklkj\r\n\r\nLKJJJJJJJJKKKKKKKKKKKKKKK ����&\r\n\r\n" +
                    "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" +
                    "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" +
                    "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" +
                    "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" +
                    "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" +
                    "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" +
                    "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" +
                    "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n";

}
