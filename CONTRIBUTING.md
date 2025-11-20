# Contributing to ISL

Thank you for your interest in contributing to the ISL project!

## How to Contribute

### Reporting Issues

If you find a bug or have a feature request:

1. Check if the issue already exists in the GitHub Issues
2. If not, create a new issue with a clear title and description
3. Include:
   - Steps to reproduce (for bugs)
   - Expected vs actual behavior
   - ISL version
   - Sample code/script if applicable

### Pull Requests

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass (`./gradlew test`)
6. Commit your changes (`git commit -m 'Add amazing feature'`)
7. Push to the branch (`git push origin feature/amazing-feature`)
8. Open a Pull Request

### Code Style

- Follow Kotlin coding conventions
- Write clear, descriptive commit messages
- Add comments for complex logic
- Update documentation for new features

### Testing

- Write unit tests for new features
- Ensure existing tests pass
- Run the full test suite before submitting PRs

```bash
./gradlew test
```

### Documentation

- Update relevant documentation in the `docs/` folder
- Add examples for new features
- Keep the README.md up to date

### Developer Certificate of Origin (DCO)

This project uses the [Developer Certificate of Origin](https://developercertificate.org/) to verify contribution rights.

1. Configure Git with your real name and email (matching your DCO identity):
   ```bash
   git config user.name "Your Real Name"
   git config user.email "your.email@example.com"
   ```
2. Sign every commit with the `Signed-off-by` trailer:
   ```bash
   git commit -s -m "Add amazing feature"
   ```
   This appends `Signed-off-by: Your Name <you@example.com>` automatically.
3. If you forget to sign, you can amend the latest commit:
   ```bash
   git commit --amend -s
   ```
4. Pull requests with unsigned commits will be asked to rebase or amend before review.

## Development Setup

### Prerequisites

- Java 21 or higher
- Kotlin 2.1+
- Gradle 8.5+

### Build the Project

```bash
# Build all modules
./gradlew build

# Build specific module
./gradlew :isl-transform:build
./gradlew :isl-cmd:build
./gradlew :isl-validation:build

# Run tests
./gradlew test

# Build fat JAR for command-line tool
./gradlew :isl-cmd:shadowJar
```

### Project Structure

```
isl/
├── isl-transform/      # Core ISL runtime and parser
├── isl-validation/     # Schema validation module
├── isl-cmd/           # Command-line interface
└── docs/              # Documentation
```

## Code of Conduct

- Be respectful and inclusive
- Welcome newcomers
- Focus on constructive feedback
- Help others learn and grow

## Questions?

If you have questions about contributing, feel free to:
- Open an issue with the "question" label
- Start a discussion in GitHub Discussions

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.

---

## Contributors

Special thanks to all contributors:
- @corneliutusnea
- Francois Beaussier
- Paulo Miguel Magalhaes
- And many others!

Your contributions help make ISL better for everyone!

