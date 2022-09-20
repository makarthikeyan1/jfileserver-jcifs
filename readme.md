## Setup
1. Copy `test/resources/fileserver.xml` to the base of the home directory
    1. Update it with enterprise license in the license section
2. `./gradlew run` to start the server
3. `./gradlew :test --tests ClientTest` to run the test
    1. logging is configured through `src/test/resources/log4j2.xml`
   