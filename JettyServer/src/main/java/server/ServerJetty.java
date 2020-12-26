package server;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import javax.servlet.MultipartConfigElement;


public class ServerJetty {

    private static Server server;
    static int port = 8080;
    static int maxThreads = 100;
    static int minThreads = 10;
    static int idleTimeout = 120;

    public static void main(String [] args) {

       /*maxThreads - Чтобы указать максимальное количество потоков, которое может выполнять Jetty
       создать и использовать в пуле
        minThreads – Чтобы установить начальное количество потоков в пуле, которое Jetty будет использовать
       idleTimeout - это значение в миллисекундах определяет, как долго поток
       может быть простаивающим, прежде чем он будет остановлен и удален из пула потоков.
       * */
        QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads, idleTimeout);
        server = new Server(threadPool);
        ServerConnector serverConnector = new ServerConnector(server);
        serverConnector.setPort(port);
        server.addConnector(serverConnector);
        server.setConnectors(new Connector[]{serverConnector });

        ServletHandler servletHandler = new ServletHandler();
        servletHandler.addServletWithMapping(ServerHandlerJetty.class, "/haevy/async/*").getRegistration();
        server.setHandler(servletHandler);

        try {
            server.start();
            server.join();
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }

    }

    void stop () throws Exception{
        server.stop();
    }
}
