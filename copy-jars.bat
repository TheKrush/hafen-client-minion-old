@echo off
echo Copying Files ...
xcopy ".\hafen\dist\hafen.jar" ".\files\hafen.jar" /R /Y
xcopy ".\hafen-minion\dist\hafen-minion.jar" ".\files\hafen-minion.jar" /R /Y
echo Removing Project Folders ...
rmdir ".\hafen\" /S /Q
rmdir ".\hafen-map-combiner\" /S /Q
rmdir ".\hafen-minion\" /S /Q
