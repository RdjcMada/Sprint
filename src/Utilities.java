package initialise;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class Utilities {
    HashMap<String, Mapping> hashMap;

    // Sprint 1 : show the url
    public void initializeControllers(HttpServlet svr, List<String> controllerList,
            HashMap<String, Mapping> urlMethod) {
        try {
            ServletContext context = svr.getServletContext();
            String packageName = context.getInitParameter("Controller");

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(packageName.replace('.', '/'));

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (resource.getProtocol().equals("file")) {
                    File file = new File(resource.toURI());
                    scanControllers(file, packageName, controllerList, urlMethod);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Sprint 2 : show Controller
    public void scanControllers(File directory, String packageName, List<String> controllerList,
            HashMap<String, Mapping> urlMethod) {
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                scanControllers(file, packageName + "." + file.getName(), controllerList, urlMethod);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(AnnotationController.class)) {
                        controllerList.add(className);
                        Method[] methods = clazz.getDeclaredMethods();
                        for (Method method : methods) {
                            if (method.isAnnotationPresent(AnnotationMethode.class)) {
                                AnnotationMethode annt = method.getAnnotation(AnnotationMethode.class);
                                Mapping map = new Mapping();
                                map.add(clazz.getName(), method.getName());
                                urlMethod.put(annt.value(), map);
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        this.hashMap = urlMethod;
    }

    public String extractRelativePath(HttpServletRequest request) {
        String fullUrl = request.getRequestURL().toString(); // Obtenez l'URL complet
        String[] relativePath = fullUrl.split("/");// Supprimer le chemin de base de l'URL complet
        return relativePath[relativePath.length - 1];
    }

    public Mapping ifMethod(HttpServletRequest request, HashMap<String, Mapping> urlMethod) {
        String method = this.extractRelativePath(request);
        if (urlMethod.containsKey(method)) {
            return urlMethod.get(method);
        }
        return null;
    }

    // Sprint 3 : call the method of the controller
    public Object callMethod(HttpServletRequest request, HttpServletResponse response, Mapping mapping)
            throws Exception {
        try {
            // get the class
            Class<?> clazz = Class.forName(mapping.getKey());

            // Class method
            Object obj = clazz.getDeclaredConstructor().newInstance();
            Method method = clazz.getMethod(mapping.getValue().trim());
            return (Object) method.invoke(obj);
        } catch (Exception e) {
            throw e;
        }
    }

    // Sprint 4 : redirect to another page and send all the attribut if the returned
    // value is ModelVIew
    public void MappingHandler(HttpServletRequest request, HttpServletResponse response, Mapping mapping)
            throws Exception {
        Object obj = this.callMethod(request, response, mapping);
        if (obj instanceof ModelView) {
            ModelView mv = (ModelView) obj;
            if (mv.getProperties() != null && !mv.getProperties().isEmpty()) {
                System.out.println("here1");
                for (Map.Entry<String, Object> entry : mv.getProperties().entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (key != null && value != null) {
                        request.setAttribute(key, value);
                    } else {
                        System.out.println("Null key or value found: key = " + key + ", value = " + value);
                    }
                }
                System.out.println("here");
            } else {
                System.out.println("The properties HashMap is null or empty.");
            }

            // Construct the correct relative URL
            String relativeUrl = mv.getUrl();
            if (!relativeUrl.startsWith("/")) {
                relativeUrl = "/" + relativeUrl;
            }

            RequestDispatcher dispatcher = request.getRequestDispatcher(relativeUrl);
            dispatcher.forward(request, response);
        } else {
            try (PrintWriter out = response.getWriter()) {
                out.println("<p>Classe : " + mapping.getKey() + "</p>");
                out.println("<p>Méthode : " + mapping.getValue() + "</p>");
                out.println("<p>Value returned : " + obj + "</p>");
            }
        }
    }
}
