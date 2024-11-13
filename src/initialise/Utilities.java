package initialise;

//. All the importation 
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;

import initialise.annotation.entity.Controller;
import initialise.annotation.argument.Param;
import initialise.annotation.mapping.Get;
import initialise.annotation.mapping.Post;
import initialise.annotation.mapping.RestAPI;
import initialise.annotation.mapping.Url;
import initialise.properties.AnnotatedParameter;
import initialise.properties.AnnotationHandler;
import initialise.properties.CustomSession;
import initialise.properties.Mapping;
import initialise.properties.ModelView;
import initialise.properties.VerbAction;
import initialise.properties.Files;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

//. The sub class Utilities (that directly iteract with the methods of the FrameWork)
public class Utilities {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    HashMap<String, Mapping> hashMap; // To store all the method of the Controller
    CustomSession session;
    int status;

    // Getter and setter for the custom session
    public CustomSession getSession() {
        return session;
    }

    public void setSession(CustomSession session) {
        this.session = session;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return this.status;
    }

    // All the functionnalities
    public void initializeControllers(HttpServlet svr, List<String> controllerList,
            HashMap<String, Mapping> urlMethod) throws Exception {
        try {
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
        } catch (Exception e) {
            throw e;
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
                                // Initialize the variable that will be used
                                Url annt = method.getAnnotation(Url.class);
                                Mapping map = new Mapping();
                                VerbAction action = new VerbAction();
                                action.setNameMethod(method.getName());

                                if (method.isAnnotationPresent(Get.class)) {
                                    action.setVerb("Get");
                                }

                                if (method.isAnnotationPresent(Post.class)) {
                                    action.setVerb("Post");
                                }

                                if (method.isAnnotationPresent(RestAPI.class)) {
                                    action.setVerb("RestAPI");
                                }

                                if (action.getVerb().equals("None")) {
                                    action.setVerb("Get");
                                }

                                if (!urlMethod.containsKey(annt.path())) {
                                    List<VerbAction> actions = new ArrayList<VerbAction>();
                                    actions.add(action);
                                    map.add(clazz.getName(), actions);
                                    urlMethod.put(annt.path(), map);
                                } else {
                                    // Treatment to see if there is a duplicated verb in the same url
                                    this.checkDuplicatedVerbUrl(urlMethod.get(annt.path().trim()), annt.path(), action);
                                    // Treatment to set the new VerbAction to the verb
                                    urlMethod.get(annt.path().trim()).getVerbActions().add(action);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    throw e;
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
            VerbAction action = this.getVerb(request, response, mapping);

            // Set the value of session to the custom session :
            this.session_To_custom_session(request, this.session);

            // Handle if the the CustomSession is an Attribute
            if (this.containsFieldOfType(obj.getClass(), CustomSession.class)) {
                Method methodSetter = obj.getClass().getMethod("setSession", CustomSession.class);
                methodSetter.invoke(obj, this.session);
            }

            for (Parameter parameter : method.getParameters()) {
                // Exception if the parameter isn't annoted
                if (!parameter.isAnnotationPresent(Param.class)
                        && !parameter.getType().equals(CustomSession.class)) {
                    throw new Exception("Tous les paramètres doivent être annotés avec @Param");
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

                Param param = parameter.getAnnotation(Param.class);

                if (parameter.getType() != String.class && parameter.getType() != initialise.properties.Files.class) {
                    // Send data parameter
                    this.sendDataObject(request, parameter.getType().getName(), parameterValues, param);

                } else if (parameter.getType() == initialise.properties.Files.class) {
                    String parameterName = param.nom();
                    Part file = request.getPart(parameterName);
                    if (file != null) {
                        String fileName = this.getFileName(file);
                        Files fl = Files.convertPartToFile(file, fileName);
                        parameterValues.add(fl);
                    } else {
                        throw new Exception("Aucun Fichier recu");
                    }
                } else {
                    // Natural selection
                    // if (value != null) {
                    // parameterValues.add(castValue(parameter.getType(), value));
                    // } else
                    if (param != null) { // Annotation selection
                        String parameterName = param.nom();
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
            if (action.getVerb().equals("RestAPI")) {
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

        // check the mapping
        this.ifMethodMapping(request, response, mapping);

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
                // out.println("<p>Méthode : " + mapping.getValue() + "</p>");
                out.println("<p>Value returned : " + obj + "</p>");
            }
        }

        // Check if it is a JSON so print it
        if (this.checkIfJson(obj)) {
            try (PrintWriter out = response.getWriter()) {
                out.println("<p>Value returned : " + obj + "</p>");
            }
        }

        throw new Exception("The return value of controller methods must be String or ModelView");
    }

    public void runFramework(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            if (this.ifMethod(request, this.hashMap) != null) {
                Mapping mapping = this.ifMethod(request, this.hashMap);
                this.MappingHandler(request, response, mapping);
            } else {
                this.status = 404;
                throw new Exception(request.getRequestURL().toString() + "\" Not found");
            }
        } catch (Exception e) {
            throw e;
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
            if (parameter.isAnnotationPresent(Param.class)) {
                Param param = parameter.getAnnotation(Param.class);
                annotatedParameters.add(new AnnotatedParameter(param.nom(), parameter.getType()));
            }
        }
        return annotatedParameters;
    }

    public Method getMethod(HttpServletRequest request, HttpServletResponse response, Mapping mapping)
            throws Exception {
        try {
            Class<?> clazz = Class.forName(mapping.getKey());
            Method[] methods = clazz.getDeclaredMethods();
            Method meth = null;
            for (VerbAction act : mapping.getVerbActions()) {
                for (Method method : methods) {
                    if (method.getName().equals(act.getNameMethod().trim())) {
                        if (request.getMethod().trim().toUpperCase().equals(act.getVerb().trim().toUpperCase())) {
                            return method;
                        }
                        meth = method;
                    }
                }
            }
            return meth;
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
    public void sendDataObject(HttpServletRequest request, String nameObject, List<Object> parameterValues, Param param)
            throws Exception {
        Class<?> clazz = Class.forName(nameObject);
        Object obj = clazz.getDeclaredConstructor().newInstance();



        Enumeration<String> innerParameterNames = request.getParameterNames();

        try {
            while (innerParameterNames.hasMoreElements()) {
                // Get the current value and doing another iteration
                String attrib = innerParameterNames.nextElement();

                // Get the value of the HttpRequest
                String value = request.getParameter(attrib);

                // Treat the attribute return
                if (parameterNameValue(attrib, nameObject) != null) {
                    Field fieldAttribute = parameterNameValue(attrib, nameObject);
                    fieldAttribute.setAccessible(true);
                    String nameAttribute = fieldAttribute.getName();
                    nameAttribute = nameAttribute.substring(0, 1).toUpperCase() + nameAttribute.substring(1);
                    
                    String setterMethodName = "set" + nameAttribute;

                    // Get the setter method
                    Method[] methods = clazz.getMethods();
                    Method setterMethod = null;
                    for (Method method : methods) {
                        if (method.getName().equals(setterMethodName)) {
                            setterMethod = method;
                            break;
                        }
                    }

                    if (setterMethod != null) {
                        AnnotationHandler.check(nameObject, fieldAttribute.getName(), value, fieldAttribute.getType());
                        // Convert and cast value to the parameter type of the setter method
                        Object convertedValue = convertAndCastValue(value, fieldAttribute.getType());
                        // Invoke the setter method on the object
                        setterMethod.invoke(obj, convertedValue);
                    } else {
                        throw new Exception("Setter method not found for attribute: " + nameAttribute);
                    }
                    fieldAttribute.setAccessible(false);
                }
            }

            // Add the value to the list of objects
            parameterValues.add(obj);

        } catch (Exception e) {
            throw e;
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

    // Sprint 10 : Mapping ver, URL
    public void ifMethodMapping(HttpServletRequest request, HttpServletResponse response, Mapping mapping)
            throws Exception { // Method that will check the mapping and the resquest method
        String method = request.getMethod();
        VerbAction action = this.getVerb(request, response, mapping);
        if (action.getVerb().toUpperCase().equals("RESTAPI")) {
            return;
        }

        if (!method.equals(action.getVerb().toUpperCase())) {
            throw new Exception("The method " + action.getVerb().toUpperCase() + " can\'t be applied to  " + method);
        }
    }

    // Sprint 10 modified version
    public void checkDuplicatedVerbUrl(Mapping mapping, String nameUrl, VerbAction action) throws Exception {
        for (VerbAction act : mapping.getVerbActions()) {
            if (act.getVerb().toUpperCase().trim().equals(action.getVerb().toUpperCase().trim())) {
                throw new Exception("Duplicated verb with the same URL with the method : " + act.getNameMethod() + " ("
                        + act.getVerb() + ") "
                        + " and " + action.getNameMethod() + " (" + action.getVerb() + ") ");
            }
        }
    }

    public VerbAction getVerb(HttpServletRequest request, HttpServletResponse response, Mapping mapping)
            throws Exception {
        try {
            Class<?> clazz = Class.forName(mapping.getKey());
            Method[] methods = clazz.getDeclaredMethods();
            VerbAction verb = null;
            for (VerbAction act : mapping.getVerbActions()) {
                for (Method method : methods) {
                    if (method.getName().equals(act.getNameMethod().trim())) {
                        if (request.getMethod().trim().toUpperCase().equals(act.getVerb().trim().toUpperCase())) {
                            return act;
                        }
                        verb = act;
                    }
                }
            }
            return verb;
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    // Sprint 11 : Show error
    public void showException(HttpServletRequest request, HttpServletResponse response, String exception,
            StackTraceElement[] elements)
            throws IOException {
        // Définir le type de contenu de la réponse comme HTML
        response.setContentType("text/html");
        response.setStatus(status);

        // Obtenir le PrintWriter pour écrire la réponse
        PrintWriter out = response.getWriter();

        // Écrire le contenu HTML avec le CSS incorporé
        out.println("<!DOCTYPE html>");
        out.println("<html lang='fr'>");
        out.println("<head>");
        out.println("<meta charset='UTF-8'>");
        out.println("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        out.println("<title>Erreur du Serveur</title>");
        out.println("<style>");
        out.println(
                "body { font-family: Arial, sans-serif; background-color: #1b1b1b; color: #eaeaea; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }");
        out.println(
                ".error-container { max-width: 600px; background: #2c2c2c; padding: 30px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3); text-align: left; }");
        out.println(".status-code { font-size: 36px; font-weight: bold; color: #ff6b6b; margin-bottom: 5px; }");
        out.println(".error-title { font-size: 24px; margin-bottom: 20px; color: #ffab00; }");
        out.println(".description { font-size: 16px; line-height: 1.5; color: #bbbbbb; margin-bottom: 20px; }");
        out.println(
                ".stack-trace { background: #1e1e1e; padding: 10px; border-radius: 5px; overflow-y: auto; max-height: 200px; font-size: 14px; line-height: 1.4; color: #aaa; }");
        out.println(".stack-trace div { padding: 2px 0; }");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
        out.println("<div class='error-container'>");
        out.println("<div class='status-code'>Erreur " + status + "</div>");
        out.println("<div class='error-title'>Un problème est survenu</div>");
        out.println("<div class='description'>" + exception + "</div>");
        out.println("<div class='stack-trace'>");
        for (StackTraceElement element : elements) {
            out.println("<div>&bull; " + element.toString() + "</div>");
        }
        out.println("</div>");
        out.println("</div>");
        out.println("</body>");
        out.println("</html>");
    }

    // Sprint 12 : Treatment of the file type
    private String getFileName(Part part) { //// Function to extract the file name by the Part class
        String contentDisposition = part.getHeader("content-disposition");
        if (contentDisposition != null) {
            // Le nom du fichier est inclus dans l'en-tête content-disposition
            for (String cdPart : contentDisposition.split(";")) {
                if (cdPart.trim().startsWith("filename")) {
                    return cdPart.substring(cdPart.indexOf('=') + 1).trim().replace("\"", "");
                }
            }
        }
        return null;
    }

    // Sprint 13 : Annotation condition Handler By the class
    public Field parameterNameValue(String parameterRequest, String nameClass) throws Exception {
        // that will check if the parameterName in the request Http is contained in the
        // class as an attribute

        // Treatment of the parameter
        try {
            Class<?> clazz = Class.forName(nameClass);
            Field[] fields = clazz.getDeclaredFields();

            for (Field field : fields) {
                String[] partsParemeterRequest = parameterRequest.trim().split("\\.");
                if (partsParemeterRequest[partsParemeterRequest.length - 1].trim().toLowerCase()
                        .equals(field.getName().trim().toLowerCase())) {
                    return field;
                }
            }
        } catch (Exception e) {
            throw e;
        }

        // Return null if there is no corresponding parameter
        return null;
    }
}
