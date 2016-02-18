
@rem
@rem usage: bminstall [-p port] [-h host] [-b] [-s] [-Dname[=value]]* pid
@rem   pid is the process id of the target JVM
@rem   -h host selects the host name or address the agent listener binds to
@rem   -p port selects the port the agent listener binds to
@rem   -b adds the agent jar to the bootstrap classpath
@rem   -s sets an access-all-areas security policy for the  agent code
@rem   -Dname=value can be used to set system properties whose name starts with "com.hengtiansoft.peg.byteagent."
@rem   expects to find a agent jar in AGENT_HOME
@rem
@rem -----------------------------------------------------------------------------------
if "%OS%" == "Windows_NT" setlocal

if "%~1" == "" goto showUsage

@rem set agent environment
call "%~dp0\bmsetenv.bat"
if %ERRORLEVEL% == 1 goto exitBatch

@rem the Install class is in the agent install jar
if exist "%AGENT_HOME%\lib\install.jar" goto okInstallJar
echo "Cannot locate agent install jar"
goto exitBatch

:okInstallJar
set AGENT_INSTALL_JAR=%AGENT_HOME%\lib\install.jar

set CP=%AGENT_INSTALL_JAR%

@rem we also need a tools jar from JAVA_HOME
if not "%JAVA_HOME%" == "" goto okJavaHome
echo please set JAVA_HOME
@rem carry on anyway as this is legitimate for jdk9
goto noTools

:okJavaHome

if exist "%JAVA_HOME%\lib\tools.jar" goto okTools
echo "Cannot locate tools jar"
@rem carry on anyway as this is legitimate for jdk9
goto noTools

:okTools
set CP=%AGENT_INSTALL_JAR%;%JAVA_HOME%\lib\tools.jar

:noTools

@rem exception avoidance; java.lang.UnsatisfiedLinkError: no attach in java.library.path
if exist "%JAVA_HOME%\jre\bin" set PATH=%PATH%;%JAVA_HOME%\jre\bin

@rem allow for extra java opts via setting BYTEMAN_JAVA_OPTS
@rem attach class will validate arguments
java %BYTEMAN_JAVA_OPTS% -classpath "%CP%" com.hengtiansoft.peg.byteagent.install.Install %*

:exitBatch
if "%OS%" == "Windows_NT" endlocal
exit /b

:showUsage
echo usage: bminstall [-p port] [-h host] [-b] [-s] [-Dname[=value]]* pid
echo   pid is the process id of the target JVM
echo   -h host selects the host name or address the agent listener binds to
echo   -p port selects the port the agent listener binds to
echo   -b adds the agent jar to the bootstrap classpath
echo   -s sets an access-all-areas security policy for the agent code
echo   -Dname=value can be used to set system properties whose name starts with "com.hengtiansoft.peg.byteagent."
echo   expects to find an agent jar in BYTEMAN_HOME
goto exitBatch
