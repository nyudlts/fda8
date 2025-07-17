## Overview

Faculty Digital Archive is a repository of NYU scholarship, allowing digital works—text, audio, video, data, and more—to be reliably shared and securely stored. Faculty Digital Archive uses DSpace open source software.
This repositoy is a fork of Dspace version 8. 

Faculty Digital Archive consists of both a Java-based backend and an Angular-based frontend.

* Backend (this codebase) provides a REST API, along with other machine-based interfaces (e.g. OAI-PMH, SWORD, etc)
    * The REST Contract is at https://github.com/DSpace/RestContract
* Frontend (https://github.com/nyudlts/fda8-angular/ which is the fork of https://github.com/DSpace/dspace-angular/) is the User Interface built on the REST API

For more information about DSpace, visit http://www.dspace.org/

Detailed Documentation for  DSpace 8 release may be viewed online or downloaded via  [Documentation Wiki](https://wiki.lyrasis.org/display/DSDOC8x).

Instructions provided below are mostly copied from upstream [dspace repository](https://github.com/DSpace/DSpace) repository with some local modifications

## Documentation / Installation


The latest DSpace Installation instructions are available at:
https://wiki.lyrasis.org/display/DSDOC8x/Installing+DSpace

Please be aware that, as a Java web application, DSpace requires a database (PostgreSQL)
and a servlet container (usually Tomcat) in order to function.
More information about these and all other prerequisites can be found in the Installation instructions above.

## Running DSpace 8 in Docker

NOTE: At this time, DSpace do not have production-ready Docker images.
We use quick-start Docker Compose scripts to build development or testing environment.
Below are quick instructions on how you can build development environment with local test data

## To build DSpace images using code in your branch
```
docker compose -f docker-compose.yml -f docker-compose-cli.yml build
```

OPTIONALLY, you can build DSpace images using a different JDK_VERSION like this:
```
docker compose -f docker-compose.yml -f docker-compose-cli.yml build --build-arg JDK_VERSION=17
```
Default is Java 11, but other LTS releases (e.g. 17) are also supported.

## Run DSpace 8 REST from your current branch
```
docker compose -p d8 up -d
```
Application will be available at http://localhost:8080/server/#/server/api

## Rebuild database and index with local data

1. Delete existing database
   ```
   # Before doing so, it sets "db.cleanDisabled=false".
   # WARNING: This will delete all your data. It's just an example of how to do so.
  docker compose -p d8 exec -e "db__P__cleanDisabled=false" dspace /dspace/bin/dspace database clean
  ```
2. Copy local data to db container
    ```
    docker cp local_dump.sql dspacedb:local_dump.sql
    docker compose -p d8 exec dspacedb psql -U dspace -f local_dump.sql
   ```

3. Finally, reindex all database contents into Solr 
    ```
    docker compose -p d8 exec dspace /dspace/bin/dspace index-discovery -b
   ```

Here are more detailed instructions on [running DSpace 8 with Docker Compose](dspace/src/main/docker-compose/README.md)

## Contributing

See [Contributing documentation](CONTRIBUTING.md)

## Getting Help

We have internal slack channel dedicated to Faculty Digital Archive upgrade to Dspace version 8.

DSpace provides public mailing lists where you can post questions or raise topics for discussion.


* [dspace-community@googlegroups.com](https://groups.google.com/d/forum/dspace-community) : General discussion about DSpace platform, announcements, sharing of best practices
* [dspace-tech@googlegroups.com](https://groups.google.com/d/forum/dspace-tech) : Technical support mailing list. See also our guide for [How to troubleshoot an error](https://wiki.lyrasis.org/display/DSPACE/Troubleshoot+an+error).
* [dspace-devel@googlegroups.com](https://groups.google.com/d/forum/dspace-devel) : Developers / Development mailing list

Great Q&A is also available under the [DSpace tag on Stackoverflow](http://stackoverflow.com/questions/tagged/dspace)


## Testing

### Running Tests

By default, in DSpace, Unit Tests and Integration Tests are disabled. However, they are
run automatically by [GitHub Actions](https://github.com/DSpace/DSpace/actions?query=workflow%3ABuild) for all Pull Requests and code commits.

* How to run both Unit Tests (via `maven-surefire-plugin`) and Integration Tests (via `maven-failsafe-plugin`):
  ```
  mvn install -DskipUnitTests=false -DskipIntegrationTests=false
  ```
* How to run _only_ Unit Tests:
  ```
  mvn test -DskipUnitTests=false
  ```
* How to run a *single* Unit Test
  ```
  # Run all tests in a specific test class
  # NOTE: failIfNoTests=false is required to skip tests in other modules
  mvn test -DskipUnitTests=false -Dtest=[full.package.testClassName] -DfailIfNoTests=false

  # Run one test method in a specific test class
  mvn test -DskipUnitTests=false -Dtest=[full.package.testClassName]#[testMethodName] -DfailIfNoTests=false
  ```
* How to run _only_ Integration Tests
  ```
  mvn install -DskipIntegrationTests=false
  ```
* How to run a *single* Integration Test
  ```
  # Run all integration tests in a specific test class
  # NOTE: failIfNoTests=false is required to skip tests in other modules
  mvn install -DskipIntegrationTests=false -Dit.test=[full.package.testClassName] -DfailIfNoTests=false

  # Run one test method in a specific test class
  mvn install -DskipIntegrationTests=false -Dit.test=[full.package.testClassName]#[testMethodName] -DfailIfNoTests=false
  ```
* How to run only tests of a specific DSpace module
  ```
  # Before you can run only one module's tests, other modules may need to be installed into your ~/.m2
  cd [dspace-src]
  mvn clean install

  # Then, move into a module subdirectory, and run the test command
  cd [dspace-src]/dspace-server-webapp
  # Choose your test command from the lists above
  ```

## License

DSpace source code is freely available under a standard [BSD 3-Clause license](https://opensource.org/licenses/BSD-3-Clause).
The full license is available in the [LICENSE](LICENSE) file or online at http://www.dspace.org/license/

DSpace uses third-party libraries which may be distributed under different licenses. Those licenses are listed
in the [LICENSES_THIRD_PARTY](LICENSES_THIRD_PARTY) file.
