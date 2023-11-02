
# Third Party Orchestrator

This orchestrates calls / data from the two third-party services 
i.e. third-party-developer & third-party application

## Requirements 

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs at least a [JRE] to run.

## Tests

The tests include unit tests and integration tests.
In order to run them, use this command line:

```
./run_all_tests.sh
```

## Run the application

To run the application use the `run_local_with_dependencies.sh` script to start the service along with all of
the back end dependencies that it needs (which are started using Service Manager). 

Note that although this service doesn't use MongoDB, the dependent services do - so MongoDB will have be set up locally. 

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").