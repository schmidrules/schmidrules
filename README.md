# Schmidrules Application Architecture

Easy application architecture documentation and assertion.
Supported languages:
* Java
* C/C++

### Project status ###
This project has been created, because we had an internal need to simply describe our application architecture, enforce it by having a build breaker in our CI pipeline and create a visualization based on the architecture descriptor file. At the time of the creation [ArchUnit](https://www.archunit.org/) was not available or at least not publicly available. 
In the meantime, [ArchUnit](https://www.archunit.org/) has become an actively maintained, sophisticated solution to test the application architecture and we recommend using it for new Java applications instead of schmidrules.

### The Maven Plugin ###
In order to include the architecture assertion in your CI Build Pipeline (Jenkins, Bamboo, TeamCity etc.) 
use the maven plugin of SchmidRules. The plugin will give you meaningful messages, when your architecture 
description is not inline with the real architecture and will produce a build failure in such a case.  

* [Schmidrules-maven-plugin](https://github.com/schmidrules/schmidrules-maven-plugin)
