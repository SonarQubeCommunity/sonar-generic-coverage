Sonar Generic Coverage
======================

[![Build Status](https://api.travis-ci.org/SonarSource/sonar-generic-coverage.svg)](https://travis-ci.org/SonarSource/sonar-generic-coverage)

<aside class="warning">
**Deprecated** since SonarQube 6.2 because the functionality was absorbed into SonarQube core.
</aside>

| Property | Example | Description |
|---------|---------|--------|
| sonar.genericcoverage.reportPaths | report1.xml, report2.xml | Comma separated paths to the Coverage by UT Reports |
| sonar.genericcoverage.itReportPaths | it_report.xml | Comma separated paths to the Coverage by IT Reports |
| sonar.genericcoverage.unitTestReportPaths | ut_report.xml | Comma separated paths to the Unit Tests Execution Results Report|

## Unit Tests Execution Results Report Format
The project for which you want to import unit tests results should contain an XML file complying with the attached XSD schema.
It should look like the following sample:

```<unitTest version="1">
  <file path="src/test/java/com/example/MyTestClass.java">
    <testCase name="test1" duration="500"/>
    <testCase name="test2" duration="600"/>
    <testCase name="test3" duration="600">
      <failure message="sort message">long stacktrace</failure>
    </testCase>
    <testCase name="test4" duration="600">
      <error message="sort message">long stacktrace</error>
    </testCase>
    <testCase name="test5" duration="600">
      <skipped message="sort message">long stacktrace</skipped>
    </testCase>
  </file>
</unitTest>
```

The root node should be named "unitTest". Its version attribute should be set to "1".
Insert a "file" element for each test file. Its "path" attribute can be either absolute or relative to the root of the module.
(warning) Unlike for coverage reports, the files present in the report must be test file names, not source code files covered by tests.
Inside a "file" element, insert a "testCase" for each test run by unit tests. It can have the following attributes/children:

* "testCase" (mandatory)
* "name" (mandatory): name of the test case
* "duration (mandatory): long value in milliseconds
* "failure|error|skipped" (optional): if the test is not ok report the cause with a message and a long description
* "message" (mandatory): short message describing the cause
* "stacktrace" (optional): long message containing details about "failure|error|skipped" status

## Coverage by Unit Tests or Integration Tests Report Format
The project for which you want to import coverage data and integration tests coverage data should contain an XML file complying with the attached XSD schema.
It should look like the following sample:

```<coverage version="1">
  <file path="src/main/java/com/example/MyClass.java">
    <lineToCover lineNumber="2" covered="false"/>
    <lineToCover lineNumber="3" covered="true" branchesToCover="8" coveredBranches="7"/>
  </file>
</coverage>
```

The root node should be named "coverage". Its version attribute should be set to "1".
Insert a "file" element for each file which can be covered by tests. Its "path" attribute can be either absolute or relative to the root of the module.
Inside a "file" element, insert a "lineToCover" for each line which can be covered by unit tests. It can have the following attributes:
* "lineNumber" (mandatory)
* "covered" (mandatory): boolean value indicating whether tests actually hit that line
* "branchesToCover" (optional): number of branches which can be covered
* "coveredBranches" (optional): number of branches which are actually covered by tests
 
