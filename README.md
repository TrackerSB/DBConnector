# DBConnector
![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/bayern.steinbrecher/DBConnector?server=https%3A%2F%2Foss.sonatype.org)
![Sonatype Nexus (Releases)](https://img.shields.io/nexus/r/bayern.steinbrecher/DBConnector?server=https%3A%2F%2Foss.sonatype.org)
![Maven Central](https://img.shields.io/maven-central/v/bayern.steinbrecher/DBConnector)
## Goal
Interaction with databases in a statically typesafe and generic way.
## Main features
- Querying and Schemes
    - Generation of `CREATE` statements
    - Parameterized column names
    - Generation of type safe queries
    - Static type safety in usage of any column information
    - Static type safety in any query of any tables or columns
- Connection
    - via JDBC
    - via SSH
