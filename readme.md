# admit-one

## Overview
The application server exposes simple orders endpoint to provide access to the AdmitOne order system.

### JsonApi
The API follows an older version of JsonApi. In particular, Links are used to represent relationships, and related entities are included by default. Also, This API supports the PATCH operation on the collection.

### Endpoint versioning
Rest endpoints are versioned by their URL endpoint using the major version of the api in the path. i.e. http://localhost:8080/v2/orders/12345

### Technology Stack
* Java 8 - Current version of Java with latest features and active public support
* Gradle - Improves upon its predecessors with excellent multi project support
* Dropwizard - Packages best of breed components for creating RESTful services
* Jetty - The most popular web servlet library freely available
* Jersey - Reference implementation of JAX-RS for REST
* Jackson - Lightning fast JSON library with pojo mapping
* Metrics - Metrics library for capturing performance data
* Guava - The best swiss army knife for JAVA. Futures, Collections, and more
* JDBI - Abstraction over JDBC for fluent database access
* Liquibase - Manages database schema and versioning over time
* JSR-310 (javax.time) - Best date time library available, Joda-time’s successor

### Actions

* GET /orders - Query all orders. Filters consist of javascript boolean expressions (see src/main/antlr/Filter.g4). Order is specified via comma separated properties and options ascending/descending keywords. Returns 200 on success.
* POST /orders - Create a new order. If using admin credentials, one must specify the user in the document. Otherwise, user must match the Authorization token or be omitted. Returns 201 + Location header on success. 
* PATCH /orders - Bulk update the collection via differential objects. If id is specified, the order is patched, otherwise inserted. Set tickets to 0 to delete the order. Returns empty 202 on success.

* GET /orders/{id} - Retrieve a specific order by id. 200 on success
* PATCH /orders/{id} - Update an individual order. 

## Building

To build:

```
$ git clone git@github.com:jpmeyer/admit-one-server.git
$ cd admit-one-server/
$ ./gradlew build
```

## Running / Testing

### Initial Setup
* Install java 8 on the local machine.
* Ensure mysql is installed either locally or is accessible via network.
* The `admitone` schema needs to be created on the mysql server separately due to various permission conflicts that may arise.
* Adjust the connection string and credentials to be used in local.yml. By default, it points to localhost:3306 root/password 
* Run `./gradlew migrate`
* Execute the example-data.sql script against the database if desired.

### Command
* Simply execute `./gradlew run` from then to start up a local server on port 8080. The port can be adjusted in the local.yml file.
