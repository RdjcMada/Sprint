@echo off
REM Répertoire contenant vos fichiers .class
set "CLASSES_DIR=bin"

REM Nom de votre fichier JAR
set "JAR_FILE=Framework.jar"

REM Création du fichier JAR
jar cvf %JAR_FILE% -C %CLASSES_DIR% .

REM couper le fichier .jar vers lib 
xcopy /s /q /y "Framework.jar" "D:\itu\S5\Clustering\Sprint\Test_sprint\lib"

REM supp .jar
rm "Framework.jar"

