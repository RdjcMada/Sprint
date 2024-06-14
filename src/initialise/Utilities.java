package initialise;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;

import initialise.annotation.Attribute;
import initialise.annotation.Controller;
import initialise.annotation.Get;
import initialise.properties.AnnotatedParameter;
import initialise.properties.Mapping;
import initialise.properties.ModelView;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class Utilities {
    HashMap<String, Mapping> hashMap;

    public void initializeControllers(HttpServlet svr, List<String> controllerList,
            HashMap<String, Mapping> urlMethod) throws Exception {
        ServletContext context = svr.getServletContext();
        String packageName = context.getInitParameter("Controller");

        if (packageName == null || packageName.trim().isEmpty()) {
            throw new Exception("No package controller defined");
        } else if (!this.ifPackageExist(packageName)) {
            throw new Exception("Package '" + packageName + "' not found");
        } else {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(packageName.replace('.', '/'));

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (resource.getProtocol().equals("file")) {
                    File file = new File(resource.toURI());
                    scanControllers(file, packageName, controllerList, urlMethod);
                }
            }
        }
    }

    public void scanControllers(File directory, String packageName, List<String> controllerList,
            HashMap<String, Mapping> urlMethod) throws Exception {
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
                    if (clazz.isAnnotationPresent(Controller.class)) {
                        controllerList.add(className);
                        Method[] methods = clazz.getDeclaredMethods();
                        for (Method method : methods) {
                            if (method.isAnnotationPresent(Get.class)) {
                                Get annt = method.getAnnotation(Get.class);
                                Mapping map = new Mapping();
                                map.add(clazz.getName(), method.getName());
                                if (urlMethod.putIfAbsent(annt.value(), map) != null) {
                                    if (!urlMethod.containsKey(annt.value())) {
                                        urlMethod.put(annt.value(), map);
                                    } else {
                                        throw new Exception("url : " + annt.value() + " duplicated");
                                    }
                                }
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    throw new Exception(e);
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

    public Object callMethod(HttpServletRequest request, HttpServletResponse response, Mapping mapping)
            throws Exception {
        try {
            Class<?> clazz = Class.forName(mapping.getKey());
            Object obj = clazz.getDeclaredConstructor().newInstance();
            Method method = this.getMethod(request, response, mapping);

            List<Object> parameterValues = new ArrayList<>();
            for (Parameter parameter : method.getParameters()) {
                // Get the value from get Parameter with the parameter name
                String paraName = parameter.getName();
                String value = request.getParameter(paraName.trim());
                Attribute attribute = parameter.getAnnotation(Attribute.class);

                if (value != null) {
                    parameterValues.add(castValue(parameter.getType(), value));
                } else if (attribute != null) {

                    String parameterName = attribute.nom();
                    String parameterValue = request.getParameter(parameterName);
                    if (parameterValue != null) {
                        parameterValues.add(castValue(parameter.getType(), parameterValue));
                    } else {
                        parameterValues.add(castValue(parameter.getType(), null)); // Or handle default values if
                                                                                   // necessary
                    }
                } else {
                    // Define the value as null if there no coresponding name or annotation
                    parameterValues.add(castValue(parameter.getType(), null));
                }
            }

            return method.invoke(obj, parameterValues.toArray());
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    public void MappingHandler(HttpServletRequest request, HttpServletResponse response, Mapping mapping)
            throws Exception {
        Object obj = this.callMethod(request, response, mapping);
        if (obj instanceof ModelView) {
            ModelView mv = (ModelView) obj;
            if (mv.getProperties() != null && !mv.getProperties().isEmpty()) {
                for (Map.Entry<String, Object> entry : mv.getProperties().entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (key != null) {
                        request.setAttribute(key, value);
                    } else {
                        throw new Exception("Null key found: key = " + key);
                    }
                }
            } else {
                throw new Exception("The properties HashMap is null or empty.");
            }

            String relativeUrl = mv.getUrl();
            if (!relativeUrl.startsWith("/")) {
                relativeUrl = "/" + relativeUrl;
            }

            RequestDispatcher dispatcher = request.getRequestDispatcher(relativeUrl);
            dispatcher.forward(request, response);
        } else if (obj instanceof String) {
            try (PrintWriter out = response.getWriter()) {
                out.println("<p>Classe : " + mapping.getKey() + "</p>");
                out.println("<p>Méthode : " + mapping.getValue() + "</p>");
                out.println("<p>Value returned : " + obj + "</p>");
            }
        } else {
            throw new Exception("The return value of controller methods must be String or ModelView");
        }
    }

    public void runFramework(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (this.ifMethod(request, this.hashMap) != null) {
            Mapping mapping = this.ifMethod(request, this.hashMap);
            this.MappingHandler(request, response, mapping);
        } else {
            throw new Exception(
                    "Error 404: \"" + request.getRequestURL().toString() + "\" Not found");
        }
    }

    public boolean ifPackageExist(String namePackage) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(namePackage);
        if (resource != null) {
            return true;
        } else {
            return false;
        }
    }

    // Sprint 6:
    public List<AnnotatedParameter> getAttributes(Method method) {
        List<AnnotatedParameter> annotatedParameters = new ArrayList<>();

        Parameter[] parameters = method.getParameters();
        for (Parameter parameter : parameters) {
            if (parameter.isAnnotationPresent(Attribute.class)) {
                Attribute attribute = parameter.getAnnotation(Attribute.class);
                annotatedParameters.add(new AnnotatedParameter(attribute.nom(), parameter.getType()));
            }
        }
        return annotatedParameters;
    }

    public Method getMethod(HttpServletRequest request, HttpServletResponse response, Mapping mapping)
            throws Exception {
        try {
            Class<?> clazz = Class.forName(mapping.getKey());
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(mapping.getValue().trim())) {
                    return method;
                }
            }
            throw new Exception("No such method: " + mapping.getValue());
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    public Object castValue(Class<?> type, String value) throws Exception {
        if (type == String.class) {
            return value;
        } else if (type == int.class || type == Integer.class) {
            if (value == null) {
                return 0;
            }
            return Integer.parseInt(value);
        } else if (type == float.class || type == Float.class) {
            if (value == null) {
                return 0;
            }
            return Float.parseFloat(value);
        } else if (type == double.class || type == Double.class) {
            if (value == null) {
                return 0;
            }
            return Double.parseDouble(value);
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        // Ajoutez d'autres types selon les besoins
        throw new Exception("Unsupported type: " + type);
    }
}
