# installation concepts

## environment

 * `CATALINA_HOME`  : tomcat installation
 * `ASIO_BASE`      : asio binaries
 * `ASIO_HOME`      : asio configuration folders (?)

## management script

### commands

* `${ASIO_BASE}/bin/asio deploy [name] [path/to/config.ttl]`
* `${ASIO_BASE}/bin/asio undeploy [name]`
* [optional] `${ASIO_BASE}/bin/asio reload [name]`

### implementation

* copy mapping file (`config.ttl`) to `${ASIO_HOME}/[name]/config.ttl`
* [optional] write settings to `${ASIO_HOME}/[name]/asio.properties`
* [optional] asio could watch `${ASIO_HOME}/[name]/` for config changes
* [possibly] create `${ASIO_HOME}/[name]/context.xml` from template
* invoke `http://localhost:${TOMCAT_PORT}/manager/text/deploy?config=file:/${CONTEXT.XML}&war=${ASIO_BASE}/asio.war`
  to deploy the web application

## asio parameterization

### convention
webapp searches config in `${ASIO_HOME}/[context-name]/` (-> use `ServletContext.getContextPath()`)

### JNDI
use `java:comp/env/asio/home` to access path to configuration folder (-> use tomcat `context.xml` to define
separate JNDI resources for each deployed webapp)
