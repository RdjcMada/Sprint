package initialise;

//. All the importation 
import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;

import initialise.annotation.Attribute;
import initialise.annotation.Controller;
import initialise.annotation.Get;
import initialise.annotation.Post;
import initialise.annotation.Url;
import initialise.annotation.RestAPI;

import initialise.properties.AnnotatedParameter;
import initialise.properties.CustomSession;
import initialise.properties.Mapping;
import initialise.properties.ModelView;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

//. The sub class Utilities (that directly iteract with the methods of the FrameWork)
public class Utilities {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    HashMap<String, Mapping> hashMap; // To store all the method of the Controller
    HashMap<String, Mapping> typeMap; // To store the type of the controller methods
                                      // (<TypeAnnotation,name_to_call_the_method_to_the_url>)
    CustomSession session;

    // Getter and setter for the custom session
    public CustomSession getSession() {
        return session;
    }

    public void setSession(CustomSession session) {
        this.session = session;
    }

    public void setTypeMap(HashMap<String, Mapping> typeMap) {
        this.typeMap = typeMap;
    }

    // All the functionnalities
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
                            // See if it's anoted @Url
                            if (method.isAnnotationPresent(Url.class)) {
                                Url annt = method.getAnnotation(Url.class);
                                Mapping map = new Mapping();
                                map.add(clazz.getName(), method.getName());

                                if (method.isAnnotationPresent(Get.class)) {
                                    map.setVerb("Get");
                                }

                                if (method.isAnnotationPresent(Post.class)) {
                                    map.setVerb("Post");
                                }

                                if (method.isAnnotationPresent(RestAPI.class)) {
                                    map.setVerb("RestAPI");
                                }

                                if (map.getVerb().equals("None")) {
                                    map.setVerb("Get");
                                }

                                if (urlMethod.putIfAbsent(annt.path(), map) != null) {
                                    if (!urlMethod.containsKey(annt.path())) {
                                        urlMethod.put(annt.path(), map);
                                    } else {
                                        throw new Exception("url : " + annt.path() + " duplicated");
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

            // Set the value of session to the custom session :
            this.session_To_custom_session(request, this.session);

            // Handle if the the CustomSession is an Attribute
            if (this.containsFieldOfType(obj.getClass(), CustomSession.class)) {
                Method methodSetter = obj.getClass().getMethod("setSession", CustomSession.class);
                methodSetter.invoke(obj, this.session);
            }

            for (Parameter parameter : method.getParameters()) {
                // Exception if the parameter isn't annoted
                if (!parameter.isAnnotationPresent(Attribute.class)
                        && !parameter.getType().equals(CustomSession.class)) {
                    throw new Exception("Tous les paramètres doivent être annotés avec @Attribute");
                }

                /*
                 * Section for the natural selection
                 * // Get the value from get Parameter with the parameter name
                 * String paraName = parameter.getName();
                 * String value = request.getParameter(paraName.trim()); //For the natural
                 * selection
                 */

                if (parameter.getType().equals(CustomSession.class)) {
                    parameterValues.add(this.session);
                }

                Attribute attribute = parameter.getAnnotation(Attribute.class);
                if (parameter.getType() != String.class) {
                    // Send data parameter
                    this.sendDataObject(request, parameter.getType().getName(), parameterValues);
                } else {
                    // Natural selection
                    // if (value != null) {
                    // parameterValues.add(castValue(parameter.getType(), value));
                    // } else
                    if (attribute != null) { // Annotation selection
                        String parameterName = attribute.nom();
                        String parameterValue = request.getParameter(parameterName);
                        if (parameterValue != null) {
                            parameterValues.add(castValue(parameter.getType(), parameterValue));
                        } else {
                            parameterValues.add(castValue(parameter.getType(), null)); // Or handle default values if
                                                                                       // necessary
                        }
                    } else { // set a null value if there no corresponding parameter
                        // Define the value as null if there no coresponding name or annotation
                        parameterValues.add(castValue(parameter.getType(), null));
                    }
                }
            }
            Object val = method.invoke(obj, parameterValues.toArray());

            // Set the actual session to the customSession
            customSession_To_session(request, this.session);

            // Treat the data to JSON if the annotation is
            if (mapping.getVerb().equals("RestAPI")) {
                if (val instanceof ModelView) {
                    return this.convertModelViewToJson((ModelView) val);
                } else {
                    return this.gson.toJson(val);
                }
            }

            return val;
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    public void MappingHandler(HttpServletRequest request, HttpServletResponse response, Mapping mapping)
            throws Exception {
        Object obj = this.callMethod(request, response, mapping);

        //check the mapping 
        this.ifMethodMapping(request,response,mapping);

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
        }
        
        if (obj instanceof String) {
            try (PrintWriter out = response.getWriter()) {
                out.println("<p>Classe : " + mapping.getKey() + "</p>");
                out.println("<p>Méthode : " + mapping.getValue() + "</p>");
                out.println("<p>Value returned : " + obj + "</p>");
            }
        }

        //Check if it is a JSON so print it 
        if (this.checkIfJson(obj)) {
            try (PrintWriter out = response.getWriter()) {
                out.println("<p>Value returned : " + obj + "</p>");
            }
        }

        throw new Exception("The return value of controller methods must be String or ModelView");
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

    /// Function to cast the value of the attribute
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

    // Function for the sprint 7
    /// Function to the objectParameters the value from the get http
    public void sendDataObject(HttpServletRequest request, String nameObject, List<Object> parameterValues)
            throws Exception {
        Enumeration<String> parameterNames = request.getParameterNames();
        String[] packages = nameObject.trim().split("\\."); // Escape dot with double backslash
        List<String> objectTreated = new ArrayList<>();

        while (parameterNames.hasMoreElements()) {
            String parameterName = parameterNames.nextElement();
            // Check if the parameter name contains the last part of the package name and
            // hasn't been processed yet
            if (parameterName.trim().toLowerCase().contains(packages[packages.length - 1].toLowerCase())
                    && !objectTreated.contains(packages[packages.length - 1].toLowerCase())) {
                try {
                    Class<?> clazz = Class.forName(nameObject);
                    Object obj = clazz.getDeclaredConstructor().newInstance();

                    Enumeration<String> innerParameterNames = request.getParameterNames();

                    while (innerParameterNames.hasMoreElements()) {
                        String attrib = innerParameterNames.nextElement();

                        // Check if the attribute name contains the last part of the package name
                        if (attrib.trim().toLowerCase().contains(packages[packages.length - 1].toLowerCase())) {
                            // Get the value to set
                            String value = request.getParameter(attrib);
                            // Determine the attribute name without the package prefix
                            String prefix = packages[packages.length - 1].toLowerCase() + ".";
                            if (attrib.toLowerCase().startsWith(prefix)) {
                                String nameAttribute = attrib.substring(prefix.length());

                                // Capitalize the first letter of the attribute name
                                nameAttribute = nameAttribute.substring(0, 1).toUpperCase()
                                        + nameAttribute.substring(1);

                                // Construct setter method name
                                String setterMethodName = "set" + nameAttribute;

                                // Get the setter method
                                Method[] methods = clazz.getMethods();
                                Method setterMethod = null;
                                for (Method method : methods) {
                                    if (method.getName().equalsIgnoreCase(setterMethodName)) {
                                        setterMethod = method;
                                        break;
                                    }
                                }

                                if (setterMethod != null) {
                                    // Get the parameter type of the setter method
                                    Class<?>[] parameterTypes = setterMethod.getParameterTypes();
                                    // Convert and cast value to the parameter type of the setter method
                                    Object convertedValue = convertAndCastValue(value, parameterTypes[0]);
                                    // Invoke the setter method on the object
                                    setterMethod.invoke(obj, convertedValue);
                                } else {
                                    throw new Exception("Setter method not found for attribute: " + nameAttribute);
                                }
                            }
                        }
                    }

                    // Mark the class name as processed
                    objectTreated.add(packages[packages.length - 1].toLowerCase());

                    // Add the value to the list of objects
                    parameterValues.add(obj);

                } catch (Exception e) {
                    throw new Exception("Error processing object: " + e.getMessage(), e);
                }
            }
        }
    }

    // Method to convert and cast value to the expected parameter type
    private Object convertAndCastValue(String value, Class<?> targetType) {
        if (targetType == String.class) {
            return value;
        } else if (targetType == Integer.class || targetType == int.class) {
            return Integer.valueOf(value);
        } else if (targetType == Double.class || targetType == double.class) {
            return Double.valueOf(value);
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.valueOf(value);
        }
        // Add more type conversions as needed for your application
        return null;
    }

    // Sprint 8 : session handler
    /// If the CustomController is as an parameters
    public void customSession_To_session(HttpServletRequest request, CustomSession c_session) {
        // Set all the variable that will be used
        HttpSession session = request.getSession();
        for (Map.Entry<String, Object> entry : c_session.getProperties().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            session.setAttribute(key, value);
        }
    }

    public void session_To_custom_session(HttpServletRequest request, CustomSession c_session) {
        // Create a new CustomSession if it's null
        if (c_session == null) {
            c_session = new CustomSession();
        }

        // Get all the value from the session
        HttpSession session = request.getSession();
        Enumeration<String> attributeNames = session.getAttributeNames();
        HashMap<String, Object> sessionMap = c_session.getProperties();

        // Initialize sessionMap if it's null
        if (sessionMap == null) {
            sessionMap = new HashMap<>();
            c_session.setProperties(sessionMap); // Set the properties back to CustomSession
        }

        // Populate sessionMap with session attributes
        while (attributeNames.hasMoreElements()) {
            String attributeName = attributeNames.nextElement();
            Object attributeValue = session.getAttribute(attributeName);
            sessionMap.put(attributeName, attributeValue);
        }
    }

    /// If the CustomController is as an attribute
    public boolean containsFieldOfType(Class<?> targetClass, Class<?> fieldType) {
        // Get all declared fields of the class
        Field[] fields = targetClass.getDeclaredFields();

        // Iterate through each field
        for (Field field : fields) {
            // Check if the field's type matches the desired class type
            if (field.getType().equals(fieldType)) {
                return true;
            }
        }
        return false;
    }

    // Sprint 9 :
    public Boolean checkIfJson(Object obj) {
        Gson gson = new Gson();

        // Try to parse the object to JSON
        try {
            String jsonString = gson.toJson(obj); // Try to convert obj to JSON
            return true;
        } catch (JsonSyntaxException e) {
            // obj is not valid JSON
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public String convertModelViewToJson(ModelView view) {
        // Créer une instance de Gson
        Gson gson = new Gson();
        HashMap<String, Object> properties = view.getProperties();
        // Convertir les propriétés en JSON
        return gson.toJson(properties);
    }

    //Sprint 10 : Mapping ver, URL
    public void ifMethodMapping(HttpServletRequest request, HttpServletResponse response, Mapping mapping)throws Exception{   //Method that will check the mapping and the resquest method
        String method = request.getMethod();
        if(mapping.getVerb().toUpperCase().equals("RESTAPI")){
            return;
        }

        if(!method.equals(mapping.getVerb().toUpperCase())){
            throw new Exception("The method "+mapping.getVerb().toUpperCase()+ " can\'t be applied to  "+method);
        }
    }
}
