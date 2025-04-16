del .\tage\*.class
del .\tage\ai\behaviortrees\*.class
del .\tage\audio\*.class
del .\tage\audio\joal\*.class
del .\tage\input\*.class
del .\tage\input\action\*.class
del .\tage\networking\*.class
del .\tage\networking\client\*.class
del .\tage\networking\server\*.class
del .\tage\nodeControllers\*.class
del .\tage\objectRenderers\*.class
del .\tage\physics\*.class
del .\tage\physics\JBullet\*.class
del .\tage\rml\*.class
del .\tage\shapes\*.class

javac tage\*.java
javac tage\input\*.java
javac tage\input\action\*.java
javac tage\networking\*.java
javac tage\networking\client\*.java
javac tage\networking\server\*.java
javac tage\nodeControllers\*.java
javac tage\shapes\*.java
javac tage\objectRenderers\*.java
javac tage\physics\*.java
javac tage\physics\JBullet\*.java
javac tage\rml\*.java
javac tage\ai\behaviortrees\*.java
javac tage\audio\*.java
javac tage\audio\joal\*.java

del client/*.class

javac -Xlint:unchecked client/*.java

java --add-exports java.base/java.lang=ALL-UNNAMED --add-exports java.desktop/sun.awt=ALL-UNNAMED --add-exports java.desktop/sun.java2d=ALL-UNNAMED -Dsun.java2d.d3d=false -Dsun.java2d.uiScale=1 client.MyGame