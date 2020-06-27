# IGSN Metadata Registry

## Requirements

* JDK 8+

## Quick Start
```
cp /src/main/resources/application.properties.sample /src/main/resources/application.properties
mvn clean install -DskipTests
mvn spring-boot:run
```

## Test
```
mvn test
```
By default will use the `src/test/resources/application.properties`

Test & generate Jacoco code coverage report
```
mvn test jacoco:report
```
jacoco HTML test coverage report will now be available with `open target/site/jacoco/index.html` and the binary available at `target/jacoco.exec`