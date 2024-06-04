package pattern;

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

public class FrontController extends HttpServlet {
    List<String> controllerList;
    HashMap<String, Mapping> urlMethod;
    Utilities utl;
    List<Exception> errors = new ArrayList<Exception>();

    @Override
    public void init() throws ServletException {
        controllerList = new ArrayList<>();
        urlMethod = new HashMap<>();
        utl = new Utilities();
        try {
            utl.initializeControllers(this, this.controllerList, urlMethod, errors);
        } catch (Exception e) {

        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, Exception {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        if (!this.errors.isEmpty()) {
            for (Exception e : this.errors) {
                out.println(e.getMessage());
            }
            errors.clear();
        } else {
            try {
                utl.runFramework(request, response, this.errors);
                if (!this.errors.isEmpty()) {
                    for (Exception e : this.errors) {
                        out.println(e.getMessage());
                    }
                    errors.clear();
                }
            } catch (Exception e) {
                out.println("Error: " + e.getMessage());
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
