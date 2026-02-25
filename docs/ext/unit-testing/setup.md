---
title: Test Setup
parent: Unit Testing
grand_parent: Advanced Topics
nav_order: 1
---

# Test Setup

## Running Tests via CLI (Recommended)

The easiest way to run ISL tests is with the `isl test` command:

```bash
# Run tests in current directory (discovers **/*.isl with @test or @setup)
isl test

# Run tests in a specific directory
isl test tests/

# Run a specific file
isl test tests/sample.isl

# Custom glob pattern
isl test tests/ --glob "**/*.test.isl"

# Write results to JSON
isl test -o results.json
```

The CLI discovers all `.isl` files containing `@setup` or `@test` annotations and runs them.

## Running Tests Programmatically

### Maven

Add the `isl-test` dependency:

```xml
<dependency>
    <groupId>com.intuit.isl</groupId>
    <artifactId>isl-test</artifactId>
    <version>1.1.19+</version>
</dependency>
```

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.intuit.isl:isl-test:1.1.19+")
}
```

### Kotlin Example

```kotlin
import com.intuit.isl.runtime.FileInfo
import com.intuit.isl.test.TransformTestPackageBuilder
import java.nio.file.Path
import java.nio.file.Paths

// Build test package from files
val basePath = Paths.get(".").toAbsolutePath()
val fileInfos = listOf(
    FileInfo("tests/sample.isl", Path.of("tests/sample.isl").toFile().readText())
).toMutableList()

val testPackage = TransformTestPackageBuilder().build(
    fileInfos,
    findExternalModule = null,
    basePath = basePath  // Required for @.Load.From
)

// Run all tests
val results = testPackage.runAllTests()

// Run a specific test
val singleResult = testPackage.runTest("tests/sample.isl", "test_simpleAssertion")

// Check results
results.testResults.forEach { tr ->
    println("${tr.testName}: ${if (tr.success) "PASS" else "FAIL"} ${tr.message ?: ""}")
}
```

### Java Example

```java
import com.intuit.isl.runtime.FileInfo;
import com.intuit.isl.test.TransformTestPackageBuilder;
import com.intuit.isl.test.annotations.TestResultContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;

Path basePath = Paths.get(".").toAbsolutePath();
List<FileInfo> fileInfos = new ArrayList<>();
fileInfos.add(new FileInfo("tests/sample.isl", Files.readString(Path.of("tests/sample.isl"))));

var builder = new TransformTestPackageBuilder();
var testPackage = builder.build(fileInfos, null, basePath);

TestResultContext results = testPackage.runAllTests();

// Run specific test
TestResultContext singleResult = testPackage.runTest("tests/sample.isl", "test_simpleAssertion");
```

## basePath and Load.From

When running tests programmatically, pass `basePath` to `TransformTestPackageBuilder.build()` so that `@.Load.From(fileName)` can resolve relative paths correctly. If `basePath` is `null`, `Load.From` will throw when used.

## Test Result Structure

`TestResultContext` contains:

- `testResults` – List of `TestResult` with:
  - `testFile` – Source file
  - `functionName` – Function name
  - `testName` – Display name (from `@test` or function name)
  - `testGroup` – Group (from `@test` or file name)
  - `success` – Whether the test passed
  - `message` – Failure message if any
  - `errorPosition` – File/line/column if available
