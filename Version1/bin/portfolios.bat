@echo on

REM  Starts the BackTester
REM  Set JAVA_HOME for JAVA Home
REM  Set APP_OPTS for setting special system properties of the application
REM  Set APP_HOME to be able to run the batch file from anywhere . Defaults to parent directory of current-folder ie., './..'


if "%APP_HOME%" == "" set APP_HOME=..

if "%APP_OPTS%" == "" set APP_OPTS=-Xmx1024m -Xms1024m -server

set _JAVACMD=%JAVACMD%

if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=%JAVA_HOME%\bin\java.exe

:noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=java.exe

set JDBC_JAR=%APP_HOME%\..\lib\sqljdbc4.jar

set LIB_CLASSPATH=%JDBC_JAR%;"%APP_HOME%\..\lib\com.springsource.org.apache.commons.logging-1.1.1.jar";"%APP_HOME%\..\lib\org.springframework.beans-3.0.5.RELEASE.jar";"%APP_HOME%\..\lib\com.springsource.org.apache.commons.logging-1.1.1.jar";"%APP_HOME%\..\lib\org.springframework.context-3.0.5.RELEASE.jar";"%APP_HOME%\..\lib\org.springframework.core-3.0.5.RELEASE.jar";"%APP_HOME%\..\lib\org.springframework.asm-3.0.5.RELEASE.jar";"%APP_HOME%\..\lib\org.springframework.expression-3.0.5.RELEASE.jar";"%APP_HOME%\..\lib\XSJava.jar";"%APP_HOME%\..\lib\org.springframework.transaction-3.0.5.RELEASE.jar";"%APP_HOME%\..\lib\org.springframework.jdbc-3.0.5.RELEASE.jar";"%APP_HOME%\..\lib\commons-dbcp-1.4.jar";"%APP_HOME%\..\lib\com.springsource.org.apache.commons.pool-1.5.3.jar";"%APP_HOME%\..\lib\joda-time-2.0.jar"
set DIST_CLASSPATH="%APP_HOME%\dist\portfolios.jar";"%APP_HOME%\ecbin"
set CONFIG_CLASSPATH="%APP_HOME%\config";"%APP_HOME%\..\lib"


"%_JAVACMD%" %APP_OPTS% -Dapp.config=application.xml  -classpath %LIB_CLASSPATH%;%DIST_CLASSPATH%;%CONFIG_CLASSPATH% com.williamoneil.backtesting.Main %*
