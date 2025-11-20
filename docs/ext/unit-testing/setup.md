---
title: Test Setup
parent: Unit Testing
grand_parent: Advanced Topics
nav_order: 1
---

## Setting up your first test

In order to utilise the library, add the following to your `pom.xml`

```xml
<dependency>
   <groupId>com.intuit.isl</groupId>
   <artifactId>isl-test</artifactId>
   <version>...</version> <!-- The version must be 1.1.19+ -->
</dependency>
```

To evoke the unit test files, run the following:

### Kotlin

```kotlin
val builder = TransformPackageBuilder()

val scripts : List<FileInfo>

val transformPackage = builder.build(fileInfo)

val transformTestPackage = TransformTestPackage(transformPackage)

// Run all tests
val testResults = testPackage.runAllTests()

// Run a specific test function within defined test file.
val individualTestResult = testPackage.runTest("test.isl", "test")
```

### Java

```java
TransformPackageBuilder builder = new TransformPackageBuilder();

List<FileInfo> scripts;

TransformPackage transformPackage = builder.build(fileInfo)

TransformTestPackage transformTestPackage = new TransformTestPackage(transformPackage)

// Run all tests
TestResultContext testResults = testPackage.runAllTests()

// Run a specific test function within defined test file.
TestResultContext individualTestResult = testPackage.runTest("test.isl", "test")
```
