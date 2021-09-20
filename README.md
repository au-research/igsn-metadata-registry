# IGSN Registry

## Requirements

* JDK 8+
* Apache Maven 3.2.1+ 
* Keycloak 10.0.1

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