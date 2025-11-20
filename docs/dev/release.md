---
title: Release Process
parent: Developer Guide
nav_order: 5
---

This document describes how to create a release of ISL.

## Building

### With Gradle

```bash
./gradlew clean build
```

### Running Tests

```bash
./gradlew test
```

### Publishing to Maven Local

```bash
./gradlew publishToMavenLocal
```

## Versioning

ISL uses semantic versioning:

- Major version: Breaking changes
- Minor version: New features, backward compatible
- Patch version: Bug fixes

## Creating a Release

1. Update version in `gradle.properties` and `pom.xml`
2. Update `CHANGELOG.md` with release notes
3. Create a git tag with the version number
4. Build and test the release
5. Publish artifacts
6. Create a GitHub release with release notes
