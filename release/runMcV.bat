SET HEAP_SIZE=512m
jre\bin\java -Xmx%HEAP_SIZE% -cp idv.jar -jar mcidasv.jar %*
REM Use the line below instead if you want to use the D3D version of Java 3D
REM jre\bin\java -Xmx%HEAP_SIZE% -Dj3d.rend=d3d -cp idv.jar -jar mcidasv.jar %*
