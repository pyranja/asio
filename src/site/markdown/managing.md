# Managing asio

You can use the `bin/asio.sh` script to manage dataset instances.

## Preparations

Make sure that asio's configuration defaults match your system or modify them accordingly.

## Commands

Following commands are supported:

#### `deploy <name> <path/to/config.ttl>`

  Ensure that an asio instance with the given `<name>` and `<config.ttl>` is present. This command
  will check whether a web application with that name is already present and act accordingly:

  * No existing web application found: A new asio instance is created.
  * An asio instance with the same name is found: Its mapping is replaced with the given one.
  * Another web application is found: Deployment fails with an error.

  If the command succeeds, an asio instance with the given `<name>` in the local tomcat
  installation exists, which uses the given `<config.ttl>`. This can also be used to reload an
  existing instance with a modified mapping.

#### `undeploy <name>`

  Ensure that no asio instance with the given `<name>` exists. An existing asio web application will
  be removed and its mapping is lost. Other web applications will remain in place and the command
  fails with an error.

#### `upgrade`

  All asio web applications present in the local tomcat installation will be redeployed, using the
  currently installed binaries. Instances which are already up to date or non-asio applications are
  skipped.

#### `migrate`

  Attempt to convert legacy d2r/dse installations to asio instances. Backups of d2r mapping files
  are stored in `$ASIO_HOME/migrated`.

#### `help`

  Show command overview.
