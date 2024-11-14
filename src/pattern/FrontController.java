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
import initialise.properties.AttributeException;

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
            // --------------------------- Sprint 14 -----------------------------------
            if (e instanceof AttributeException) { // Vérifie si e est une instance de AttributeException
                AttributeException except = (AttributeException) e; // Cast de e en AttributeException pour accéder aux
                                                                    // méthodes spécifiques
                String urlDepart = request.getHeader("Referer");
                if (!except.getErrors().isEmpty()) {
                    // Construire la base URL (sans le contexte de l'application)
                    String baseUrl = request.getScheme() + "://" + request.getServerName() + ":"
                            + request.getServerPort();

                    // Récupérer le nom du projet (contexte de l'application)
                    String contextPath = request.getContextPath(); // Cela renverra "/nom_projet" (par exemple
                                                                   // "/Test_sprint")

                    // Retirer la base URL de l'URL de départ pour obtenir un chemin relatif
                    String cheminRelatif = urlDepart.replace(baseUrl, "");
                    
                    // Transférer la requête avec le chemin relatif
                    request.setAttribute("values", except.getValues());
                    request.setAttribute("errors", except.getErrors());

                    //Treatment of the request
                    if (!cheminRelatif.startsWith(contextPath)) {
                        // Si ce n'est pas le cas, on ajoute le nom du projet (contexte) au début du
                        // chemin relatif
                        cheminRelatif = contextPath + cheminRelatif;
                    } else if (cheminRelatif.toLowerCase().trim().equals(contextPath.toLowerCase().trim()+"/")) {
                        //Bah ca me retourne vers mon : http://localhost:911/nom_projet/ 
                        request.getRequestDispatcher("/").forward(request, response);
                    }

                    request.getRequestDispatcher(cheminRelatif).forward(request, response);
                    return; // To stop the function
                }
            }
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
