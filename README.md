# Integration of Complex Event Processing Tool (Esper) with Apache Cassandra

This repository contains code, which shows how [Esper](http://www.espertech.com/esper/) - can be integrated with [Apache Cassandra](http://cassandra.apache.org/) using [Esper-Cassandra Java library](https://bitbucket.org/scharrenbach/cassandra-esper/wiki/Home) and [virtual data window mechanism.](http://www.espertech.com/esper/release-6.1.0/esper-reference/html/extension.html#extension-virtualdw)

## Motivation

All of these tools are publicly available. However, it couldn't be found an example showing how their users can integrated them with each other.

## Tools
* [Esper](http://www.espertech.com/esper/) - Esper is a component for [complex event processing (CEP)](http://www.espertech.com/esper/faq_esper.php#whatiscep) and event series analysis, available for Java.
* [Apache Cassandra](http://cassandra.apache.org/) - Apache Cassandra is a free and open-source distributed NoSQL database management system designed to handle large amounts of data across many commodity servers, providing high availability with no single point of failure. Cassandra offers robust support for clusters spanning multiple datacenters, with asynchronous masterless replication allowing low latency operations for all clients.
* [Esper-Cassandra Java Library](https://bitbucket.org/scharrenbach/cassandra-esper/wiki/Home) - is a library, that provides support for accessing a Cassandra store from the Esper complex event processing engine.

## Getting Started
1. Download [Cassandra Docker image](https://hub.docker.com/_/cassandra/) using command ```docker pull cassandra:3.11 ``` (image size: 386MB)
2. Download repository with example code using ```git clone https://github.com/patrykks/esper-cassandra-virtualdw-example.git ```
3. In the next step the esper-cassandra library preparing  will be needed. There is a problem because this library uses class ```com.datastax.driver.core.Session``` (Java Driver For Cassandra) in old version. The API of this class have changed, so this code will not compile with newest version of ```cassandra-driver-core```tIn order to use it, this problem must be solved. In this moment three ideas come to my mind to solve this problem.
    1. Implementation of custom library supporting same type of functionality as [esper-cassandra](https://bitbucket.org/scharrenbach/cassandra-esper/wiki/Home)
    2. Source code of [esper-cassandra library](https://bitbucket.org/scharrenbach/cassandra-esper/wiki/Home) is not available, but updating depracated API is not a difficult problem. Maybe it is worth trying to ask the authors of this library to update code and to publish new version to Maven central repository.
    3. Find code in ```.jar``` file of [esper-cassandra library](https://bitbucket.org/scharrenbach/cassandra-esper/wiki/Home), Then decompile it using for example [JD-GUI](http://jd.benow.ca/). Then change of occurences of ```session.shutdown()``` with ```session.close()```. Then new ```.jar``` file can be create and publish to the Maven local repository using command ```mvn clean install```. In the last step it will be required to update ```.pom``` file in ```esper-cassandra-virtualdw-example``` project.
4. After resolving problem described in point 3, finally we can run Cassandra Docker Container using command ```docker run --name some-cassandra -d cassandra```
5. In the next step the CONTAINER_ID of created container is needed. It can be accessed using command ```docker ps -l -q```
6. Then, an IP address of created docker container will be needed. It can be retrieved using command ```docker inspect CONTAINER_ID```
7. Then property ```CASSANDRA_HOST``` contained in ```esper-cassandra-virtualdw-example``` repository under path ```src/main/resources/application.properties``` have to be updated
8.  Finally we can compile and run example code using below commands from the main directory of repository ```esper-cassandra-virtualdw-example```:
    1. ```mvn clean compile assembly:single```
    2. ```java -jar target/esper-cassandra-virtualdw-example-1.0-SNAPSHOT-jar-with-dependencies.jar```
9. Optionally, state of Cassandra database can be verified using ```CQL Shell``` It can be opened by typing command ```docker run -it --link some-cassandra:cassandra --rm cassandra sh -c 'exec cqlsh "$CASSANDRA_PORT_9042_TCP_ADDR"'```

### Prerequisites
* Java 8
* Maven
* Docker


