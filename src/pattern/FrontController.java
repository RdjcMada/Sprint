package pattern;

//All the importation 
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.annotation.MultipartConfig;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import initialise.*;
import initialise.properties.CustomSession;
import initialise.properties.Mapping;

//the main class FrontController
@MultipartConfig // To ensure that the servlet will handle an input file
public class FrontController extends HttpServlet {
    List<String> controllerList;
    HashMap<String, Mapping> urlMethod;
    HashMap<String, Mapping> typeMap;
    Utilities utl;
    Exception except = null;

    @Override
    public void init() throws ServletException {
        controllerList = new ArrayList<>();
        urlMethod = new HashMap<>();
        typeMap = new HashMap<>();
        CustomSession session = new CustomSession();
        utl = new Utilities();
        try {
            utl.setStatus(500);
            utl.setSession(session);
            utl.initializeControllers(this, this.controllerList, urlMethod);
        } catch (Exception e) {
            this.except = e;
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        if (this.except != null) {
            response.setStatus(utl.getStatus());
            this.utl.showException(request, response, this.except.getMessage(), except.getStackTrace());
            return;
        }
        try {
            utl.runFramework(request, response);
        } catch (Exception e) {
            response.setStatus(utl.getStatus());
            this.utl.showException(request, response, e.getMessage(), e.getStackTrace());
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
