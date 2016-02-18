
@rem use AGENT_HOME to locate installed byteman release
@rem if not "%AGENTMAN_HOME%" == "" goto gotHome
set "CURRENT_DIR=%cd%"
cd %~dp0
cd ..
set "AGENT_HOME=%cd%"
cd "%CURRENT_DIR%"

:gotHome
if exist "%AGENT_HOME%\lib\agent.jar" goto okJar
echo Cannot locate agent jar
exit /b 1

:okJar
set "AGENT_JAR=%AGENT_HOME%\lib\agent.jar"
