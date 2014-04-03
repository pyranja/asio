# asio

asio is a HTTP based service platform for access to relational databases. It is used to host
datasets in the [VPH-Share](http://vph-share.eu/) cloud. asio provides

 * a SQL query endpoint
 * a [SPARQL Protocol](http://www.w3.org/TR/sparql11-protocol/)endpoint based on [D2R](http://d2rq.org/)
 * metadata from the [VPH repository](http://vphshare.atosresearch.eu/)

## Getting started

### Requirements

 * [Java 7](http://openjdk.java.net/projects/jdk7/)
 * [Apache Tomcat 7](http://tomcat.apache.org/)
 * [MySQL database](http://dev.mysql.com/)

### Quick start

 * Download the [current release](http://homepage.univie.ac.at/chris.borckholder/asio/latest) and
  unzip its contents to `/usr/share`.
 * Make sure, that the `$CATALINA_HOME` variable is set and points to the base directory of your
 tomcat installation.
 * Execute `/usr/share/asio/bin/asio.sh <your_dataset_name> </path/to/d2r-mapping.ttl>` to deploy
  an asio instance and restart the tomcat to let the changes take effect.
 * Open `http://<tomcat_host>/<your_dataset_name>/explore/sql.html` to start browsing your data.

## Release notes

### v0.4

#### Features

 * `asio/bin/asio.sh` shell script to deploy asio instances from the command line.
 * `/meta` resource provides metadata from the VPH repository.
 * Major improvements to the layout and usability of the SQL explorer.
