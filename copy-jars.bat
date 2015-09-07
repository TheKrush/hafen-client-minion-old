rmdir ".\build\" /S /Q
rmdir ".\nbproject\" /S /Q

xcopy ".\dist\hafen.jar" ".\files\hafen.jar" /R /Y
xcopy ".\updater\dist\hafen-minion.jar" ".\files\hafen-minion.jar" /R /Y

rmdir ".\dist\" /S /Q
rmdir ".\updater\" /S /Q