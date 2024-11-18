package initialise.properties;

import initialise.annotation.attribute.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AnnotationHandler {
    public AnnotationHandler(){

    }
    public void check(HttpServletRequest request,HttpServletResponse response,String nameObject, String nameAttribute, String value, Class<?> targetType)
            throws Exception {
        Map<String, String> errors = new HashMap<String, String>();

        try {
            // Charger la classe spécifiée par son nom
            Class<?> clazz = Class.forName(nameObject);

            // Obtenir le champ spécifié par nameAttribute
            Field field = clazz.getDeclaredField(nameAttribute);

            // Vérifier si le champ est annoté avec @Requiered
            if (field.isAnnotationPresent(Requiered.class)) {
                // Si le champ est requis, vérifier que la valeur n'est pas nulle ou vide
                if (value == null || value.trim().isEmpty()) {
                    errors.put(nameAttribute,
                            "Le champ  " + nameAttribute + " est requis et ne peut pas être vide.");
                }
            }

            // Vérifier si le champ est annoté avec @Max
            if (field.isAnnotationPresent(Max.class)) {
                Max maxAnnotation = field.getAnnotation(Max.class);
                double maxValue = maxAnnotation.value();

                // Convertir la valeur en double et vérifier la contrainte
                double numericValue;
                try {
                    numericValue = Double.parseDouble(value);
                    if (numericValue > maxValue) {
                        errors.put(nameAttribute,
                                "La valeur pour " + nameAttribute + " doit être inférieure ou égale à "
                                        + maxValue + ".");
                    }
                } catch (Exception e) {
                    errors.put(nameAttribute, "La valeur pour " + nameAttribute + " doit être un nombre.");
                }

            }

            // Vérifier si le champ est annoté avec @Min
            if (field.isAnnotationPresent(Min.class)) {
                Min minAnnotation = field.getAnnotation(Min.class);
                double minValue = minAnnotation.value();

                // Convertir la valeur en double et vérifier la contrainte
                double numericValue;
                try {
                    numericValue = Double.parseDouble(value);

                    if (numericValue < minValue) {
                        errors.put(nameAttribute,
                                "La valeur pour " + nameAttribute + " doit être supérieure ou égale à " + minValue
                                        + ".");
                    }
                } catch (Exception e) {
                    errors.put(nameAttribute, "La valeur pour " + nameAttribute + " doit être un nombre.");
                    throw e;
                }
            }

            // --------------------------- Sprint 14 -----------------------------------
            String urlDepart = request.getHeader("Referer");
            if (!errors.isEmpty()) {
                // Construire la base URL (sans le contexte de l'application)
                String baseUrl = request.getScheme() + "://" + request.getServerName() + ":"
                        + request.getServerPort();

                // Récupérer le nom du projet (contexte de l'application)
                String contextPath = request.getContextPath(); // Cela renverra "/nom_projet" (par exemple
                                                               // "/Test_sprint")

                // Retirer la base URL de l'URL de départ pour obtenir un chemin relatif
                String cheminRelatif = urlDepart.replace(baseUrl, "");

                // Transférer la requête avec le chemin relatif
                request.setAttribute("errors", errors);

                // Treatment of the request
                if (!cheminRelatif.startsWith(contextPath)) {
                    // Si ce n'est pas le cas, on ajoute le nom du projet (contexte) au début du
                    // chemin relatif
                    cheminRelatif = contextPath + cheminRelatif;
                } else if (cheminRelatif.toLowerCase().trim().equals(contextPath.toLowerCase().trim() + "/")) {
                    // Bah ca me retourne vers mon : http://localhost:911/nom_projet/
                    request.getRequestDispatcher("/").forward(request, response);
                    
                }

                // request.getRequestDispatcher(cheminRelatif).forward(request, response);

            }

        } catch (Exception e) {
            throw e;
        }
    }
}
