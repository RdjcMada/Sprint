package initialise.properties;

import initialise.annotation.attribute.*;
import java.lang.reflect.Field;

public class AnnotationHandler {
    public static void check(String nameObject, String nameAttribute, String value, Class<?> targetType,
            AttributeException except) throws Exception {
        try {
            // Charger la classe spécifiée par son nom
            Class<?> clazz = Class.forName(nameObject);

            //Firstly, put the value of the attribute in the request into the exception attribute
            except.getValues().put(nameAttribute,value);
            
            // Obtenir le champ spécifié par nameAttribute
            Field field = clazz.getDeclaredField(nameAttribute);

            // Vérifier si le champ est annoté avec @Requiered
            if (field.isAnnotationPresent(Requiered.class)) {
                // Si le champ est requis, vérifier que la valeur n'est pas nulle ou vide
                if (value == null || value.trim().isEmpty()) {
                    except.getErrors().put(nameAttribute,
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
                        except.getErrors().put(nameAttribute,
                                "La valeur pour " + nameAttribute + " doit être inférieure ou égale à "
                                        + maxValue + ".");
                    }
                } catch (Exception e) {
                    except.getErrors().put(nameAttribute, "La valeur pour " + nameAttribute + " doit être un nombre.");
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
                        except.getErrors().put(nameAttribute,
                                "La valeur pour " + nameAttribute + " doit être supérieure ou égale à " + minValue
                                        + ".");
                    }
                } catch (Exception e) {
                    except.getErrors().put(nameAttribute, "La valeur pour " + nameAttribute + " doit être un nombre.");
                    throw e;
                }
            }

        } catch (Exception e) {
            throw e;
        }
    }
}
