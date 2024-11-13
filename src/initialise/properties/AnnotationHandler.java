package initialise.properties;

import initialise.annotation.attribute.*;
import java.lang.reflect.Field;

public class AnnotationHandler {
    public static void check(String nameObject, String nameAttribute, String value, Class<?> targetType) throws Exception {
        try {
            // Charger la classe spécifiée par son nom
            Class<?> clazz = Class.forName(nameObject);

            // Obtenir le champ spécifié par nameAttribute
            Field field = clazz.getDeclaredField(nameAttribute);

            // Vérifier si le champ est annoté avec @Requiered
            if (field.isAnnotationPresent(Requiered.class)) {
                // Si le champ est requis, vérifier que la valeur n'est pas nulle ou vide
                if (value == null || value.trim().isEmpty()) {
                    throw new Exception("Le champ " + nameAttribute + " est requis et ne peut pas être vide.");
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
                } catch (Exception e) {
                    throw new Exception("La valeur pour " + nameAttribute + " doit être un nombre.");
                }

                if (numericValue > maxValue) {
                    throw new Exception("La valeur pour " + nameAttribute + " doit être inférieure ou égale à " + maxValue + ".");
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
                } catch (Exception e) {
                    throw new Exception("La valeur pour " + nameAttribute + " doit être un nombre.");
                }

                if (numericValue < minValue) {
                    throw new Exception("La valeur pour " + nameAttribute + " doit être supérieure ou égale à " + minValue + ".");
                }
            }

        } catch (Exception e) {
            throw e;
        }
    }
}
