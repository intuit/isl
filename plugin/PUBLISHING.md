# Publishing the ISL VSCode Extension

Quick guide for publishing the extension to VSCode Marketplace and Open VSX Registry.

## Prerequisites

### 1. VSCode Marketplace Account

1. Go to https://marketplace.visualstudio.com/manage
2. Sign in with Microsoft account
3. Create a publisher (or use existing)
4. Generate Personal Access Token (PAT):
   - Visit https://dev.azure.com/
   - User Settings → Personal Access Tokens
   - Create token with scope: **Marketplace → Manage**
   - Save the token securely

### 2. Open VSX Account (Optional)

For VS Codium and other VSCode-compatible editors:

1. Go to https://open-vsx.org/
2. Sign in with GitHub
3. Settings → Access Tokens → Generate
4. Save the token

### 3. Install VSCE

```bash
npm install -g @vscode/vsce
```

## Publishing Workflow

### Update Version

```bash
cd plugin
npm version patch  # or minor, or major
```

This updates `package.json` and creates a git tag.

### Pre-Publish Checklist

- [ ] Version updated in `package.json`
- [ ] `CHANGELOG.md` updated with release notes
- [ ] Code compiles without errors: `npm run compile`
- [ ] Extension tested locally
- [ ] `publisher` field correct in `package.json`

### Publish to VSCode Marketplace

```bash
cd plugin
vsce publish
```

Or with explicit token:
```bash
vsce publish -p YOUR_PAT
```

### Publish to Open VSX (Optional)

```bash
npx ovsx publish -p YOUR_OVSX_TOKEN
```

## GitHub Actions (Automated)

### Setup Secrets

Add to GitHub repository (Settings → Secrets → Actions):
- `VSCE_TOKEN`: VSCode Marketplace PAT
- `OVSX_TOKEN`: Open VSX token

### Trigger Release

```bash
# Tag and push
git tag plugin-v1.1.0
git push origin plugin-v1.1.0
```

The workflow automatically:
1. Builds extension
2. Packages `.vsix`
3. Publishes to both marketplaces
4. Creates GitHub release

## Troubleshooting

### "Cannot find publisher"
Ensure `publisher` in `package.json` matches your marketplace publisher ID exactly.

### "Authentication failed"
- Verify PAT is valid and not expired
- Check PAT has "Marketplace → Manage" scope
- Ensure secret name matches exactly: `VSCE_TOKEN`

### "Version already exists"
You cannot republish the same version. Increment version number.

### "Package too large"
VSCode Marketplace limit is 100MB. Our extension is well under this.

## Manual Testing Before Publishing

```bash
# Package locally
cd plugin
vsce package

# Install in VSCode
code --install-extension isl-language-support-X.X.X.vsix

# Test all features
```

## Links

- **VSCode Marketplace**: https://marketplace.visualstudio.com/vscode
- **Publisher Management**: https://marketplace.visualstudio.com/manage
- **Open VSX Registry**: https://open-vsx.org/
- **Publishing Guide**: https://code.visualstudio.com/api/working-with-extensions/publishing-extension

---

For technical details, see [PLUGIN-OVERVIEW.md](PLUGIN-OVERVIEW.md)
