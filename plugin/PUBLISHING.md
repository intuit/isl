# Publishing the ISL Extension

This guide explains how to publish the ISL Language Support extension to the VS Code Marketplace.

## Prerequisites

1. **Node.js and npm**: Ensure you have Node.js 14+ installed
2. **Visual Studio Code**: Install VS Code for testing
3. **vsce**: Install the VS Code Extension Manager
   ```bash
   npm install -g @vscode/vsce
   ```
4. **Azure DevOps Account**: Create a free account at https://dev.azure.com
5. **Personal Access Token (PAT)**: Generate a PAT with "Marketplace (Manage)" scope

## Setup

### 1. Install Dependencies

```bash
cd plugin
npm install
```

### 2. Compile the Extension

```bash
npm run compile
```

### 3. Test Locally

Press `F5` in VS Code to launch the Extension Development Host and test your extension.

## Publishing Steps

### 1. Create a Publisher

If you don't have a publisher ID yet:

```bash
vsce create-publisher <your-publisher-name>
```

Or login with an existing publisher:

```bash
vsce login <your-publisher-name>
```

### 2. Update package.json

Update the `publisher` field in `package.json` with your publisher name:

```json
{
  "publisher": "your-publisher-name"
}
```

### 3. Package the Extension

Create a `.vsix` file:

```bash
vsce package
```

This creates a `isl-language-support-1.0.0.vsix` file.

### 4. Test the VSIX Package

Install the packaged extension locally to test:

```bash
code --install-extension isl-language-support-1.0.0.vsix
```

### 5. Publish to Marketplace

Publish using your PAT:

```bash
vsce publish
```

Or publish with a specific version bump:

```bash
vsce publish patch  # 1.0.0 -> 1.0.1
vsce publish minor  # 1.0.0 -> 1.1.0
vsce publish major  # 1.0.0 -> 2.0.0
```

### 6. Verify Publication

Visit the [VS Code Marketplace](https://marketplace.visualstudio.com/) and search for your extension.

## Updating the Extension

1. Make your changes
2. Update version in `package.json`
3. Update `CHANGELOG.md`
4. Commit changes
5. Run `vsce publish` or `vsce publish <patch|minor|major>`

## Important Files

- **icon.png**: Replace placeholder with actual 128x128 icon before publishing
- **README.md**: Main documentation (displayed on marketplace)
- **CHANGELOG.md**: Version history (displayed on marketplace)
- **LICENSE**: Apache 2.0 license

## Pre-Publishing Checklist

- [x] Icon image (ISL logo from docs/img/isl_small.png copied to images/icon.png)
  - [ ] Verify icon is exactly 128x128 pixels (see ICON-SETUP.md if resizing needed)
- [x] README.md is complete with screenshots/examples
- [x] CHANGELOG.md is up to date
- [x] LICENSE file is present
- [ ] All tests pass
- [ ] Extension works in Extension Development Host
- [ ] package.json metadata is correct (description, keywords, repository, etc.)
- [ ] Version number follows semver
- [ ] No sensitive data in code

## Marketplace Categories

The extension is listed in:
- Programming Languages
- Formatters
- Linters

## Resources

- [VS Code Publishing Guide](https://code.visualstudio.com/api/working-with-extensions/publishing-extension)
- [Extension Manifest Reference](https://code.visualstudio.com/api/references/extension-manifest)
- [Marketplace Publisher Portal](https://marketplace.visualstudio.com/manage)

## Troubleshooting

### "Publisher not found"
Run `vsce login <publisher-name>` with your PAT.

### "Missing icon"
Add a 128x128 PNG icon at `images/icon.png` or remove the icon field from package.json.

### "Activation event not found"
Ensure `activationEvents` in package.json matches your language configuration.

### Package size too large
Check `.vscodeignore` to exclude unnecessary files (node_modules, src files, etc.).

