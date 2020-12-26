package server;

import org.eclipse.jetty.server.Request;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class ServerHandlerJetty extends HttpServlet {
    private static final MultipartConfigElement MULTI_PART_CONFIG = new MultipartConfigElement(
            System.getProperty("java.io.tmpdir"));
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        switch (pathInfo){
            case "/Test":
                // запрос какие файлы есть в папке
                super.doGet(req, resp);
                break;
            case "":
                // запрос
                super.doGet(req, resp);
                break;
            default:
                super.doGet(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain;charsets UTF-8");
        if (request.getContentType() != null && request.getContentType().startsWith("multipart/form-data")) {
            request.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, MULTI_PART_CONFIG);
        }
        ServletOutputStream out = response.getOutputStream();

        String path = "C:\\torent\\";
        Part filePart = request.getPart("myfile");

        String fileName = filePart.getSubmittedFileName();

        InputStream is = filePart.getInputStream();

        Files.copy(is, Paths.get(path + fileName), StandardCopyOption.REPLACE_EXISTING);

        out.print("File successfulle upload");
    }
}
