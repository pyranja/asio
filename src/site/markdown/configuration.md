# Configuring asio

There are two main domains in asio's configuration, deployment and runtime configurations.

## Deployment

Dataset instances can be deployed through the `bin/asio.sh` script. Following environment variables
are recognized by it. They can be set in the `bin/setenv.sh` script.

### Prerequisites

  * [Java 7](http://openjdk.java.net/projects/jdk7/)
  * [Apache Tomcat 7](http://tomcat.apache.org/)
  * [MySQL database](http://dev.mysql.com/)

### Environment

#### `ASIO_BASE`

  asio's installation directory, defaults to `/usr/share/asio`.

  Note: this variable **cannot** be set in the `/bin/setenv.sh`!

#### `ASIO_HOME`

  asio's runtime files, defaults to `/var/lib/asio`.

#### `CATALINA_HOME`

  Base directory of the local tomcat installation, defaults to `/usr/share/tomcat`

#### `ASIO_OWNER`

  The user that will own created asio instances, defaults to the user executing the script.

## Runtime

asio uses a D2R mapping file to initialize both SPARQL and SQL engines. The mapping must provide
valid JDBC connection parameters for the backing database.
