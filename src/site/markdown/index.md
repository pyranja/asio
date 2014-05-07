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
  untar its contents to `/usr/share`.
 * Execute `/usr/share/asio/bin/asio.sh <your_dataset_name> </path/to/d2r-mapping.ttl>` to deploy
  an asio instance and restart the tomcat to let the changes take effect.
 * Open `http://<tomcat_host>/<your_dataset_name>/explore/sql.html` to start browsing your data.

## Release notes

### v0.4.4

 * Various design improvements in the SQL explorer
 * XML and JSON representations of error messages
 * New `upgrade` command for asio.sh : batch redeploy asio instances when installing a new version

### v0.4.3

 * SQL explorer pages are now SSL ready
 * application/xml is now the default response content type
 * Updating mappings of deployed asio instances is now supported
 * MySQL compatibility options are now set by default

### v0.4.2

 * SQL and SPARQL explorer pages are now included when deploying an asio instance

### v0.4.1

 * Releases are now also packaged as .tar.gz
 * `migrate` command added to `asio.sh` - converts legacy vce/d2r installations
 * Dataset metadata from the VPH-Share repository is displayed in the SQL explorer

### v0.4

 * `asio/bin/asio.sh` shell script to deploy asio instances from the command line.
 * `/meta` resource provides metadata from the VPH repository.
 * Major improvements to the layout and usability of the SQL explorer.
