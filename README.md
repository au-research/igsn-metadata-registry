# IGSN Metadata Registry

IGSN Metadata Registry is a metadata registry solution that enables the creation and management of metadata records. While IGSN Metadata Registry was primarily developed by ARDC as a solution for providing [International Geo Sample Numbers](https://www.ands.org.au/working-with-data/citation-and-identifiers/igsn) (IGSN) identifier minting and management services to end user clients, the solution has been designed in a way that makes it reusable for other metadata registry requirements. Two of the main design principles underpinning IGSN Metadata Registry is extensibility and schema agnosticism. Through the addition of custom handlers, IGSN Metadata Registry can be extended to support the management and creation of virtually any schema and serialisation format. 

The solution also supports integration with external services such as [IGSN](https://doidb.wdc-terra.org/igsn/) for the minting and management of persistent identifiers associated with metadata records. insertNameHereis is Java based and backed by a MySQL database.

## Features
* Metadata Store
* REST APIs to manage metadata
* Configurable metadata schema
* Configurable document Transformation
  * Ability to store metadata for a single record in multiple schemas/formats
  * Versioning 
* OAI-PMH server 
* Global IGSN Integration 
* Stateful Request Management 
* Centralised authentication and authorisation 
* GUI for metadata editing and management via IGSN Metadata Editor 
* GUI for displaying metadata content via IGSN Metadata Portal

## Requirements

* JDK 8+
* Apache Maven 3.2.1+ 
* Keycloak 10.0.1

For detailed requirements and installation instructions, please refer to the [Installation Guide](docs/Install.md) and [Keycloak Guide](docs/Keycloak.md)

## Quick Start
```
cp /src/main/resources/application.properties.sample /src/main/resources/application.properties

mvn clean install -Dmaven.test.skip=true
mvn spring-boot:run
```

### Test
```
mvn verify
```
By default, the `src/test/resources/application.properties` will be used for testing

### Code Coverage
Code coverage is avaiable in `JaCoCo` and `Clover` in the form of [OpenClover](http://openclover.org/)

Test and generate code coverage
```
mvn clean org.openclover:clover-maven-plugin:4.4.1:setup verify org.openclover:clover-maven-plugin:4.4.1:aggregate org.openclover:clover-maven-plugin:4.4.1:clover jacoco:report
```
JaCoCo HTML test coverage report will now be available with `open target/site/jacoco/index.html` and the binary available at `target/jacoco.exec`

Clover HTML test report will now be available at `target/site/clover/index.html` with the XML report available at `target/site/clover/clover.xml`

## License
IGSN Metadata Registry is licensed under the Apache license, Version 2.0 See [LICENSE](LICENSE) for the the full license text