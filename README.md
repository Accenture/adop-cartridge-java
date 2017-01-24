[![Build Status](https://travis-ci.org/Accenture/adop-cartridge-java.svg?branch=master)](https://travis-ci.org/Accenture/adop-cartridge-java)

# What is Cartridge?

A Cartridge is a set of resources that are loaded into the Platform for a particular project. They may contain anything from a simple reference implementation for a technology to a set of best practice examples for building, deploying, and managing a technology stack that can be used by a project.

This cartridge consists of source code repositories and jenkins jobs.

## Source code repositories

Cartridge loads the source code repositories

* [spring-petclinic](https://github.com/spring-projects/spring-petclinic.git)
* [adop-cartridge-java-regression-tests](https://github.com/Accenture/adop-cartridge-java-regression-tests)
* [adop-cartridge-java-environment-template](https://github.com/Accenture/adop-cartridge-java-environment-template)

## Jenkins Jobs

This cartridge generates the jenkins jobs and pipeline views to -

* Provision the environment.
* Build the source code from sprint petclinic repository.
* Running unit tests on the compiled code.
* Running sonar analysis on the code.
* Deploy to an environment.
* Run regression tests on deployed petclinic application.

## Testing

For unit tests we are using [Spock framework](https://github.com/spockframework/spock) specifications.

`./gradlew test` runs the specs.

By default Spock generates reports in two formats XML and HTML.

* ./build/test-results/* - XML format.
* ./build/reports/tests/* - HTML format.

[JobScriptsSpec](src/test/groovy/com/java/cartridge/JobScriptsSpec.groovy) 
will loop through all DSL files and make sure they don't throw any exceptions when processed.

# License
Please view [license information](LICENSE.md) for the software contained on this image.

## Documentation
Documentation will be captured within this README.md and this repository's Wiki.

## Issues
If you have any problems with or questions about this image, please contact us through a [GitHub issue](https://github.com/Accenture/adop-cartridge-java/issues).

## Contribute
You are invited to contribute new features, fixes, or updates, large or small; we are always thrilled to receive pull requests, and do our best to process them as fast as we can.

Before you start to code, we recommend discussing your plans through a [GitHub issue](https://github.com/Accenture/adop-cartridge-java/issues), especially for more ambitious contributions. This gives other contributors a chance to point you in the right direction, give you feedback on your design, and help you find out if someone else is working on the same thing.


