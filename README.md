# ajpbin

Servlet application that is intended to be run in a container that is fronted by an AJP Connector.

## Requirements

ajpbin is built and run on Java-11. Please make sure it is installed and can be used.

ajpbin requires [Maven version 3.6.0](https://maven.apache.org/) (or newer) to be built.

ajpbin requires Tomcat to be configured with an AJP connector.

## Building

This project uses [Maven](https://maven.apache.org/) as its build system. From the project's root directory, run the command below and it will be built.

    mvn clean package

## Installing

Copy the file `PROJECT_BASE/ajpbin/target/ajpbin.war` to the `webapps` directory in your Tomcat instance and start it.

## Configuring

There are 2 configuration components:

1. `conf/server.xml` for Tomcat server-wide configuration.
1. `conf/logging.properties` for `java.util.logging`/`org.apache.tomcat.juli` configuration.

### Configuring Tomcat in conf/server.xml

See [Apache Tomcat 9 Configuration Reference, The Server Component](https://tomcat.apache.org/tomcat-9.0-doc/config/server.html) for details on editing this file. It should be very minimal and contain an [AJP connector](https://tomcat.apache.org/tomcat-9.0-doc/config/ajp.html) on port 8009 (if available, you can use other ports if needed). By default, an AJP connector on port 8009 is configured.

### Configuring Logging in conf/logging.properties

Logging uses the Tomcat default logging system (which is based on the JDK logging system). See [Tomcat Logging](https://tomcat.apache.org/tomcat-9.0-doc/logging.html) and for details.

Loggers have been pre-configured to log at the highest level for each application package. Logs are configured by default to be written to `logs/localhost-yyyy-mm-dd.log` rolling them for 14 days. The application code will write some debugging and error messages to the log so they are useful for diagnosing issues during runtime.

## Running

For general information about running a Tomcat server, see [RUNNING.TXT](https://tomcat.apache.org/tomcat-9.0-doc/RUNNING.txt). These instructions are provided here to get you started with the basic ajpbin configuration.

### Starting

Run the file `$CATALINA_BASE/bin/startup.sh` to start the server. Tomcat will log messages to `$CATALINA_BASE/logs/catalina.out` for startup and `$CATALINA_BASE/logs/localhost-yyyy-mm-dd.log` about application startup and operation.

### Stopping

Run the file `$CATALINA_BASE/bin/shutdown.sh` to stop the server. Tomcat will log messages to `$CATALINA_BASE/logs/catalina.out` for shutdown.

## Troubleshooting

### Application Does Not Run

If the web application is not running, check `$CATALINA_BASE/logs/catalina.out` for any log messages logged at `SEVERE` and look for anything related to `ajpbin` not starting. You will then check `$CATALINA_BASE/logs/localhost-yyyy-mm-dd.log` for messages and stack traces for any unhandled exceptions. 

## Etymology

The name is the same as the [httpbin](https://github.com/postmanlabs/httpbin) project by Kenneth Reitz. Its intent is the same as httpbin which is not able to be deployed into a Servlet container behind an AJP connector.
