package pattern;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import initialise.Controller;
import java.util.List;
import java.util.ArrayList;
import jakarta.servlet.ServletContext;
import java.io.File;
import java.net.URL;
import java.util.Enumeration;

public class FrontController extends HttpServlet {
    List<String> controllerList;
    boolean initialized = false;

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

        if (!initialized) {
            initializeControllers();
        }

        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>FrontController</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Servlet FrontController</h1>");
            out.println("<p>URL: " + request.getRequestURL() + "</p>");
            if (controllerList.size() != 0) {
                for (String controller : controllerList) {
                    out.println("<p> Controller: " + controller + "</p>");
                }
            }
            out.println("</body>");
            out.println("</html>");
        }
    }

    private void initializeControllers() {
        controllerList = new ArrayList<>();
        try {
            ServletContext context = getServletContext();
            String packageName = context.getInitParameter("Controller");

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(packageName.replace('.', '/'));

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (resource.getProtocol().equals("file")) {
                    File file = new File(resource.toURI());
                    scanControllers(file, packageName);
                }
            }

            initialized = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scanControllers(File directory, String packageName) {
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                scanControllers(file, packageName + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(Controller.class)) {
                        controllerList.add(className);
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}
