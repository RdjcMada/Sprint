@echo off

REM créer un dossier qui contient tout 

REM Répertoire contenant vos fichiers .java
set "JAVA_DIR=.\src"

REM Répertoire contenant vos fichiers .class
set "CLASSES_DIR=java"


REM Nom de votre fichier JAR
set "JAR_FILE=application.jar"

REM Compilez tous les fichiers .java
javac -d  %CLASSES_DIR% %JAVA_DIR%\*.java

REM Création du fichier JAR
jar cvf %JAR_FILE% -C %CLASSES_DIR% .

REM supprimer le dossier contenant les .classes 
rmdir /s /q  %CLASSES_DIR%

