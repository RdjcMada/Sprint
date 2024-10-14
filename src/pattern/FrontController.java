package pattern;

//All the importation 
import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import initialise.*;
import initialise.properties.CustomSession;
import initialise.properties.Mapping;

//the main class FrontController
public class FrontController extends HttpServlet {
    List<String> controllerList;
    HashMap<String, Mapping> urlMethod;
    HashMap<String,Mapping> typeMap;    
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
            utl.setSession(session);
            utl.initializeControllers(this, this.controllerList, urlMethod);
        } catch (Exception e) {
            this.except = e;
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        if(this.except != null){
            out.print(this.except.getMessage());
            return;
        }
        try {
            utl.runFramework(request, response);
        } catch (Exception e) {
            out.print(e.getMessage());
            e.printStackTrace();
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
