# Publishing ISL to Maven Central

This document explains how to publish ISL artifacts to Maven Central and GitHub Packages.

## Prerequisites
https://intuit-teams.slack.com/archives/C044PJN2NDR/p1749599979127989?thread_ts=1749145553.673049&cid=C044PJN2NDR


### 1. Maven Central (Sonatype OSSRH) Setup

To publish to Maven Central, you need:

1. **Sonatype JIRA Account**: Create an account at https://issues.sonatype.org/
2. **Request Repository Access**: File a ticket to request access to `com.intuit.isl` group
3. **GPG Key**: Generate a GPG key for signing artifacts

#### Generate GPG Key

```bash
# Generate key
gpg --full-generate-key
# Choose RSA (option 1), key size 3072 or 4096, and set a strong passphrase

# List keys to get the key ID
gpg --list-secret-keys --keyid-format=long

# Export the ASCII-armored secret key (replace KEY_ID with your key ID)
gpg --export-secret-keys --armor KEY_ID
```

**For Windows (PowerShell):**
```powershell
# Generate key
gpg --full-generate-key

# List keys
gpg --list-secret-keys --keyid-format=long

# Export and copy to clipboard
gpg --export-secret-keys --armor YOUR_KEY_ID | Set-Clipboard
```

**Note:** You can use the ASCII-armored key directly in the `SIGNING_KEY` secret (starts with `-----BEGIN PGP PRIVATE KEY BLOCK-----`). Base64 encoding is optional.

### 2. GitHub Repository Secrets

Configure the following secrets in your GitHub repository (Settings → Secrets and variables → Actions):

#### Maven Central Secrets:
- `OSSRH_USERNAME`: Your Sonatype JIRA username
- `OSSRH_PASSWORD`: Your Sonatype JIRA password
- `SIGNING_KEY`: Your ASCII-armored GPG private key (or base64-encoded)
- `SIGNING_PASSWORD`: Your GPG key passphrase

#### GitHub Packages:
- `GITHUB_TOKEN`: Automatically provided by GitHub Actions (no setup needed)

## Workflows

### 1. Build and Test (`build.yml`)

**Triggers:** 
- Push to `main` branch
- Pull requests to `main` branch

**Actions:**
- Builds the project
- Runs all tests
- Generates code coverage reports
- Uploads test results and build artifacts

### 2. Publish to Maven Central (`publish.yml`)

**Triggers:**
- Release creation
- Manual workflow dispatch

**Actions:**
- Builds and tests the project
- Signs artifacts with GPG
- Publishes to Maven Central
- Publishes to GitHub Packages

**Manual Trigger:**
```bash
# Go to Actions → Publish to Maven Central → Run workflow
# Specify the version (e.g., 2.4.20)
```

### 3. Performance Benchmarks (`benchmark.yml`)

**Triggers:**
- Manual workflow dispatch
- Weekly schedule (Sundays at midnight UTC)

**Actions:**
- Runs JMH benchmarks
- Uploads benchmark results

## Publishing a Release

### Option 1: Using GitHub Releases (Recommended)

1. **Create a Git tag:**
   ```bash
   git tag -a v2.4.20 -m "Release version 2.4.20"
   git push origin v2.4.20
   ```

2. **Create a GitHub Release:**
   - Go to GitHub → Releases → Create a new release
   - Select the tag you created
   - Add release notes
   - Publish the release

3. **Workflow automatically triggers:**
   - The `publish.yml` workflow will automatically run
   - Artifacts will be built, signed, and published to Maven Central

### Option 2: Manual Workflow Dispatch

1. **Update version in `build.gradle.kts`:**
   ```kotlin
   version = "2.4.20"  // Remove -SNAPSHOT for releases
   ```

2. **Trigger workflow manually:**
   - Go to Actions → Publish to Maven Central
   - Click "Run workflow"
   - Enter the version number
   - Click "Run workflow"

## Version Management

The project uses semantic versioning: `MAJOR.MINOR.PATCH`

- **SNAPSHOT versions** (e.g., `2.4.20-SNAPSHOT`):
  - Published to Sonatype Snapshots repository
  - Used for development builds
  - Automatically overwritten with each publish

- **Release versions** (e.g., `2.4.20`):
  - Published to Maven Central Staging
  - Requires manual promotion in Sonatype OSSRH
  - Immutable once published

## Promoting Releases in Sonatype

After publishing a release version:

1. **Login to Sonatype OSSRH**: https://oss.sonatype.org/
2. **Navigate to Staging Repositories**
3. **Find your staging repository** (com.intuit.isl-XXXX)
4. **Close the repository** (triggers validation)
5. **Release the repository** (publishes to Maven Central)

It takes 10-30 minutes for artifacts to appear on Maven Central after release.

## Local Publishing

For local testing:

```bash
# Publish to local Maven repository
./gradlew publishToMavenLocal

# Artifacts will be in ~/.m2/repository/com/intuit/isl/
```

## Gradle Properties (Optional)

Instead of environment variables, you can use `~/.gradle/gradle.properties`:

```properties
ossrhUsername=your-username
ossrhPassword=your-password
signing.keyId=12345678
signing.password=your-gpg-passphrase
signing.secretKeyRingFile=/path/to/.gnupg/secring.gpg
```

**Note:** Never commit credentials to version control!

## Troubleshooting

### Issue: "Could not read PGP secret key" or "checksum mismatch"
**Solution:** 
1. Use the ASCII-armored format directly (recommended):
   ```bash
   gpg --export-secret-keys --armor YOUR_KEY_ID
   ```
   Copy the entire output (including BEGIN/END markers) into the `SIGNING_KEY` secret
2. Ensure `SIGNING_PASSWORD` matches your GPG key passphrase
3. Verify the key exports correctly before adding to GitHub secrets
4. If using base64 encoding, ensure no extra whitespace or line breaks are introduced

### Issue: "Could not find signing key"
**Solution:** Ensure GPG key is properly exported and base64 encoded. Check `SIGNING_KEY` secret.

### Issue: "401 Unauthorized" for Maven Central
**Solution:** Verify OSSRH credentials and ensure you have access to `com.intuit.isl` group.

### Issue: "Signature verification failed"
**Solution:** Ensure `SIGNING_PASSWORD` matches your GPG key passphrase.

### Issue: Publishing takes too long
**Solution:** Large artifacts may take time. Check Sonatype OSSRH for staging repository status.

## Using Published Artifacts

Once published to Maven Central, users can add the dependency:

### Gradle (Kotlin DSL)
```kotlin
dependencies {
    implementation("com.intuit.isl:isl-transform:2.4.20")
}
```

### Gradle (Groovy)
```groovy
dependencies {
    implementation 'com.intuit.isl:isl-transform:2.4.20'
}
```

### Maven
```xml
<dependency>
    <groupId>com.intuit.isl</groupId>
    <artifactId>isl-transform</artifactId>
    <version>2.4.20</version>
</dependency>
```

## Support

For issues with publishing, contact:
- Sonatype OSSRH: https://issues.sonatype.org/
- GitHub Issues: https://github.com/intuit/isl/issues

