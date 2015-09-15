@echo off
echo Copying Files ...
echo f|xcopy ".\hafen\dist\hafen.jar" "..\TheKrush.github.io\files\hafen-client-minion\hafen.jar" /R /Y
echo f|xcopy ".\hafen-minion\dist\hafen-minion.jar" "..\TheKrush.github.io\files\hafen-client-minion\hafen-minion.jar" /R /Y
echo Removing Project Folders ...
rmdir ".\hafen\" /S /Q
rmdir ".\hafen-layer-util\" /S /Q
rmdir ".\hafen-map-combiner\" /S /Q
rmdir ".\hafen-minion\" /S /Q
