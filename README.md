Bonjour à vous utilisateur : 
1 - Présentation framework 
    > Le framework nom_framework est un framework pour les projets web en java qui utilise le méthode MVC (Model View Controller) comme tout les frameworks
    > ci_dessous je vous présente comment utilisé framework  
    
2 - Initialisation de l'environnemt : 
    > Copier le fichier,"application.jar" dans votre projet java ,dans le répertoire "WEB-INF/lib"
    
3 - Comment l'utiliser : 
    >   Définir le package qui contiendra les controllers: 
        . Pour ce faire : allez dans le web.xml de votre projet web application java dans le dossier "WEB-INF/"
        . Si il y en a pas créer le fichier web.xml 
        . Inclure le framework dans le web.xml comme suit: 
            '
              <servlet>
                <servlet-name>frontController</servlet-name>
                <servlet-class>pattern.FrontController</servlet-class>
                <load-on-startup>1</load-on-startup>
              </servlet> 
              '
        . Définir le package qui va contenir vos controller comme suit : 
            '
                <context-param>
                    <param-name> Controller </param-name>
                    <param-value> Nom_de_votre_package_controller </param-value>
                </context-param>
            '
        . Puis, mettre ce ligne de code dans votre web.xml : 
            '
                <servlet-mapping>
                    <servlet-name>frontController</servlet-name>
                    <url-pattern>/</url-pattern>
                </servlet-mapping>
            '
        . Donc en général vous aurez un code similaire à : 
            '
                <?xml version="1.0" encoding="UTF-8"?>
                    <web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee
                                 http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
                             version="4.0">

                          <!-- Configuration du Framework -->
                          <servlet>
                            <servlet-name>frontController</servlet-name>
                            <servlet-class>pattern.FrontController</servlet-class>
                            <load-on-startup>1</load-on-startup>
                          </servlet>

                        <!-- Configuration de votre controller -->
                          <context-param>
                            <param-name> Controller </param-name>
                            <param-value> Nom_du_package </param-value>
                          </context-param>


                          <!-- Mapping des URLs pour le Framework -->
                          <servlet-mapping>
                            <servlet-name>frontController</servlet-name>
                            <url-pattern>/</url-pattern>
                            </servlet-mapping>
                    </web-app>
            '
    >       Définir un controller : 
            . Pour ce faire, créer une nouvelle classe dans votre package : Nom_du_package (que vous avez définie dans votre web.xml précédement)
            . Maintenant, faire les importation suivante dans votre classe : 
                '
                    import initialise.annotation.*;
                    import initialise.properties.*;
                '
            . Juste avant de définir votre classe : public class Nom_class : annoté la classe (comme ceci  '@Controller()' ) pour définir que votre classe est un controller 
    >       Définir votre méthode : 
            . Dans votre controller 
