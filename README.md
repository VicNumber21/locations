## BUILD
The following command is used to build Docker containers:
```
sbt build
```


## RUN
The following command is used to run locally:
```
docker compose -f docker/compose.yaml up
```
After it is done, http://localhost:8080/docs/ link enables one to look through API and to play with it.

NOTE: docker/.env file could be used as example of customization of running instances.


## TEST
Before to run tests the following command needs to be executed:
```
sudo ln -s $HOME/.docker/run/docker.sock /var/run/docker.sock
```

The following command is used to run full testing with coverage:
```
sbt clean coverage test IntegrationTest/test coverageAggregate
```
NOTE: in Scala 3 it is impossible to filter out generated code from coverage report.
      (com.vportnov.locations.grpc is an example of generated code)
      Also some statements are not marked as covered although there passed over during test execution.
      Probably also Scala 3 related issue with sbt-coverage.
      These things make coverage statistic worse in report.

The following command is used to run integration tests fast:
```
sbt IntegrationTest/"testOnly * -- -DfastRun=true"
```
NOTE: during fast run it does not re-create database volume folder so some tests
      which expects empty database fail.

The following command is used to run integration test for whole solution:
```
sbt autotest/IntegrationTest/test
```
NOTE: should not run with coverage since apps are placed into Docker images and
      it is not expected that coverage output directories exist inside the images

Full testing (without coverage) may be run as the following:
```
sbt  test IntegrationTest/test autotest/IntegrationTest/test 
```
