# installation concepts

## environment

 * `CATALINA_HOME`  : tomcat installation
 * `ASIO_BASE`      : asio binaries
 * `ASIO_HOME`      : asio configuration folders (?)

## deploying

### explicit

`${ASIO_BASE}/bin/asio deploy [name] [path/to/config.ttl]`

* how to pass additional parameters?

### central config folder

`${ASIO_BASE}/bin/asio deploy [name]`

* will expect to find /[name]/config.ttl and /[name]/asio.properties at `ASIO_HOME`
* can be used to redeploy a dataset
* asio could watch `ASIO_HOME` for config changes

## undeploying

`${ASIO_BASE}/bin/asio undeploy [name]`
