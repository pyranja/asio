# Configuring asio

There are two main domains in asio's configuration, deployment and runtime configurations.

## Deployment

Dataset instances can be deployed through the `bin/asio.sh` script. It recognizes two environment
variables.

### Environment

#### `ASIO_HOME`

  asio's installation directory, defaults to `/usr/share/asio`

#### `CATALINA_HOME`

  Base directory of the local tomcat installation, defaults to `/usr/share/tomcat`

### Commands

Following commands are supported:

####`deploy <name> <path/to/config.ttl>`

  Create an asio instance by creating the web application folder with the given *<name>* in the
  local tomcat installation. The given *<config.ttl>* is copied to the dataset instance.

#### `undeploy <name>`

  Delete the web application folder with the given *<name>*, if it exists.

#### `migrate`

  Attempt to convert legacy d2r/vce installations to asio instances. Backups of converted web
  applications are stored in `$ASIO_HOME/backups`.

#### `help`

  Show command overview

## Runtime

asio uses a D2R mapping file to initialize both SPARQL and SQL engines. The mapping must provide
valid JDBC connection parameters for the backing database.
