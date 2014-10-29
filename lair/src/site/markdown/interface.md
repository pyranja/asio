
# Using asio

## Explore

Each dataset instance provides a HTML/JavaScript based client, which runs in the browser. Use the
  following URLs to access the
  
  * SQL view    : `http://host:port/[dataset-name]/read/explore/sql.html`
  
  * SPARQL view : `http://host:port/[dataset-name]/read/explore/sparql.html`
   
The client should work in every modern browser and is tested to work with the latest versions of 
Internet Explorer, Google Chrome and Mozilla Firefox. Note: JavaScript must be enabled.

## HTTP interface

Asio supports *machine-to-machine* interactions through a Web API. It consists of two endpoints for
  SQL- and SPARQL-based requests and a separate one, providing metadata about this instance. The
  common URI format of asio service endpoints is

> `http://host:port/[dataset-name]/[permission]/[language]`

### Common path parameters

<dl>
  <dt>[dataset-name]</dt>
  <dd>the name of the dataset instance</dd>

  <dt>[permission]</dt>
  <dd>Identifier of the set of access permissions required for an operation. Possible values are
   <code>read</code> and <code>full</code>. <code>full</code> permission is required to execute commands,
   which modify the dataset, like INSERT.</dd>

  <dt>[language]</dt>
  <dd>Specifies the language of the operation, <code>sql</code>, <code>sparql</code> or <code>meta</code>.</dd>
</dl>
  
### Operations

> `http://host:port/[dataset-name]/[permission]/sparql`

is a W3C [SPARQL Protocol](http://www.w3.org/TR/sparql11-protocol/) compliant endpoint.  

> `http://host:port/[dataset-name]/[permission]/sql`

adopts the SPARQL protocol specification to support SQL queries and updates.

The request and response format is described [here](protocol.html) 

### Metadata

> `http://host:port/[dataset-name]/[permission]/meta`

serves metadata about this instance from the [VPH repository](http://vphshare.atosresearch.eu/). Both
XML and JSON representations are available.
