# Publishing the ISL VSCode Extension

This guide explains how to publish the ISL Language Support extension to the VSCode Marketplace and Open VSX Registry.

## Prerequisites

### 1. VSCode Marketplace (Microsoft)

You need a Visual Studio Marketplace publisher account:

1. Go to https://marketplace.visualstudio.com/manage
2. Sign in with your Microsoft account
3. Create a publisher (or use existing one)
4. Generate a Personal Access Token (PAT):
   - Go to https://dev.azure.com/
   - Click on User Settings → Personal Access Tokens
   - Create new token with:
     - **Organization:** All accessible organizations
     - **Scopes:** Marketplace → **Manage**
   - Copy the token (you won't see it again!)

### 2. Open VSX Registry (Eclipse Foundation)

For VS Codium, Code-OSS, and other VSCode-compatible editors:

1. Go to https://open-vsx.org/
2. Sign in with GitHub
3. Go to Settings → Access Tokens
4. Generate a new token
5. Copy the token

## GitHub Secrets Setup

Add these secrets to your GitHub repository (Settings → Secrets and variables → Actions):

- `VSCE_TOKEN`: Your Visual Studio Marketplace Personal Access Token
- `OVSX_TOKEN`: Your Open VSX Registry Access Token

## Manual Publishing

### Publish to VSCode Marketplace

```bash
cd plugin
npx vsce publish
```

You'll be prompted for your PAT if not set in environment.

### Publish to Open VSX

```bash
cd plugin
npx ovsx publish -p YOUR_TOKEN
```

## Automated Publishing via GitHub Actions

### Method 1: Git Tag (Recommended)

Create and push a version tag:

```bash
# Update version in package.json first
cd plugin
npm version patch  # or minor, or major

# Tag the release
git tag plugin-v1.0.1
git push origin plugin-v1.0.1
```

The workflow will automatically:
1. Build the extension
2. Run tests
3. Package the .vsix
4. Publish to VSCode Marketplace
5. Publish to Open VSX Registry
6. Create a GitHub Release with the .vsix file

### Method 2: Manual Workflow Dispatch

Go to GitHub Actions → "Publish VSCode Extension" → Run workflow

Enter the version number and click "Run workflow"

## Version Management

Update version in `plugin/package.json`:

```json
{
  "version": "1.0.1"
}
```

Or use npm:

```bash
cd plugin
npm version patch  # 1.0.0 → 1.0.1
npm version minor  # 1.0.0 → 1.1.0
npm version major  # 1.0.0 → 2.0.0
```

## Pre-publish Checklist

Before publishing, ensure:

- [ ] Version number updated in `package.json`
- [ ] `CHANGELOG.md` updated with new version changes
- [ ] `README.md` is current and accurate
- [ ] All TypeScript compiles without errors (`npm run compile`)
- [ ] Extension icon is present (`images/icon.png`)
- [ ] `lib/isl-cmd-all.jar` is included and up-to-date
- [ ] All AI assistant files are included (`.cursorrules`, `.windsurfrules`, etc.)
- [ ] License file is present
- [ ] Publisher name is set correctly in `package.json`
- [ ] Test locally: `code --install-extension isl-language-support-X.X.X.vsix`

## Updating the Publisher

If you need to change the publisher in `package.json`:

```json
{
  "publisher": "your-publisher-name"
}
```

Make sure this matches your Visual Studio Marketplace publisher ID.

## Troubleshooting

### "Cannot find publisher"

Make sure the `publisher` field in `package.json` matches your marketplace publisher ID exactly.

### "Package size too large"

The VSCode Marketplace has a 100MB limit. Our extension includes the ISL runtime JAR (~35MB), which is well under the limit.

### "Authentication failed"

- Verify your PAT is valid and hasn't expired
- Check that the PAT has the "Marketplace → Manage" scope
- Make sure the secret name in GitHub matches exactly (`VSCE_TOKEN`, `OVSX_TOKEN`)

### "Version already exists"

You can't republish the same version. Update the version number in `package.json`.

## CI/CD Pipeline

The repository includes two workflows:

1. **`plugin-ci.yml`** - Runs on every push/PR to `plugin/`:
   - Builds on Ubuntu, Windows, macOS
   - Tests with Node 18 and 20
   - Creates build artifacts

2. **`publish-plugin.yml`** - Runs on version tags (`plugin-v*`):
   - Builds and packages extension
   - Publishes to VSCode Marketplace
   - Publishes to Open VSX Registry
   - Creates GitHub Release

## Links

- **VSCode Marketplace:** https://marketplace.visualstudio.com/vscode
- **Open VSX Registry:** https://open-vsx.org/
- **Publisher Management:** https://marketplace.visualstudio.com/manage
- **Extension Guidelines:** https://code.visualstudio.com/api/references/extension-guidelines
- **Publishing Extensions:** https://code.visualstudio.com/api/working-with-extensions/publishing-extension
