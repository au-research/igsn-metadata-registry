# Installation
## Building from Source Code
### System Requirements
#### Java 8
IGSN Metadata Registry is a Java 8 application and requires a Java Development Kit (JDK) to build and run it. You can obtain a JDK distribution from [AdoptOpenJDK](https://adoptopenjdk.net/). IGSN Metadata Registry is developed with the Java 8 (LTS), this means
* Java 11 (LTS) is not supported 
* Earlier versions of Java (ie Java 7) is not supported

#### Development Tools
* Java 8 - OpenJDK 
* Maven 3.2.1+
* Git

#### Database
IGSN Metadata Registry is developed with a Relational Database Management System in mind. IGSN Metadata Registry comes with limited support for an embedded DBMS (h2) which is used by default during installation and testing. It's recommended to use [Percona MySQL](https://www.percona.com/) for production deployment

#### Application Environment
IGSN Metadata Registry requires at least 1 GB of RAM for acceptable performance, the suggested amount is 2GB of RAM. For storage space, you have to consider the growing needs of data, which could grow up to 50GB or more. It's recommended to have at least 250 GB of storage space (SSD) for production deployment.  

#### Keycloak
IGSN Metadata Registry currently relies on [Keycloak](https://www.keycloak.org/) for Centralised Authentication and Authorization. It's recommended to use [Keycloak v10.0.1](https://www.keycloak.org/archive/downloads-10.0.1.html) for compatibility purpose. 

### Build
To build the application
```
cp /src/main/resources/application.properties.sample /src/main/resources/application.properties

mvn clean install -Dmaven.test.skip=true
```
The application `.jar` file will be created in the `target` directory
### Run
```
java -jar igsn-metadata-registry.jar
```

### Configuration
Most of the configuration is done via the `application.properties` file and this file can be placed next to the `.jar` file for it to take into effect

See the `application.properties.example` file for an example configuration