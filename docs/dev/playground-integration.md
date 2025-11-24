# ISL Playground Integration

This document explains how to add "Run in Playground" buttons to the ISL documentation.

## Overview

The playground integration allows documentation examples to be opened directly in the ISL Playground with pre-populated code and input JSON. This provides an interactive way for users to test and experiment with ISL code samples.

## How It Works

The system consists of two components:

1. **Playground Frontend** (`playground/frontend/src/App.tsx`) - Modified to accept URL parameters
2. **JavaScript Auto-Injector** (`docs/assets/js/playground-auto-buttons.js`) - Automatically detects ISL code blocks and adds buttons at runtime

**Key Feature:** Buttons are automatically added to all ISL code blocks - no manual work needed! Works on GitHub Pages since it's pure JavaScript.

## URL Parameters

The playground accepts the following URL parameters:

### Encoded (Recommended)
- `isl_encoded` - Base64 URL-safe encoded ISL code
- `input_encoded` - Base64 URL-safe encoded input JSON

### Plain Text (Alternative)
- `isl` - Plain text ISL code (URL encoded)
- `input` - Plain text input JSON (URL encoded)

**Example URL:**
```
https://isl-playground.up.railway.app?isl_encoded=ZnVuIHJ1bigkaW5wdXQpe...&input_encoded=eyJtZXNzYWdlIjoiSGVsbG8h...
```

## Adding Buttons to Documentation

### Automatic Detection (Default - No Work Required!)

**The JavaScript auto-injector automatically adds buttons** - you don't need to do anything special!

Just write your markdown documentation normally:

```markdown
**ISL Code:**
```isl
{
    id: $input.id,
    name: $input.name | upperCase
}
```

**Input JSON:**
```json
{
  "id": 123,
  "name": "John Doe"
}
```
```

The auto-injector will:
1. Detect the ISL code block (looks for `$input`, `fun`, `|` modifiers, etc.)
2. Find the "Input JSON" code block above it (if present)
3. Encode both to base64 URL-safe format
4. Generate the playground URL
5. Add a "▶️ Run in Playground" button after the ISL code

### How ISL Detection Works

The auto-injector identifies ISL code by looking for these patterns:
- `$input` variable references
- `fun functionName(...)` function declarations
- Pipe modifiers: `| upperCase`, `| map`, `| filter`, etc.
- `foreach $` loop syntax
- `@.functionCall` syntax

### How Input JSON Detection Works

The auto-injector searches **backward** from the ISL code block (up to 10 elements) looking for:
1. A `<pre><code>` block containing JSON (starts with `{` or `[`)
2. A preceding heading/label containing "Input JSON" (case-insensitive)

If no input is found, it defaults to `{}`.

### Manual Button Creation

If you need to manually create a button (rare), use pre-encoded URLs:

```html
<div class="playground-button-container">
  <a href="https://isl-playground.up.railway.app?isl_encoded=BASE64_HERE&input_encoded=BASE64_HERE" 
     target="_blank"
     rel="noopener noreferrer"
     class="btn-playground">
    ▶️ Run in Playground
  </a>
</div>
```

**Encoding requirements:**
- Use base64 encoding
- Make it URL-safe: replace `+` with `-`, `/` with `_`, remove `=`

## Examples

### Example 1: Simple Transformation

```markdown
**Transform customer data:**

**ISL Code:**
```isl
{
    fullName: `${$input.firstName} ${$input.lastName}`,
    email: $input.email | lowerCase
}
```

**Input JSON:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "JOHN@EXAMPLE.COM"
}
```
```

✨ **Button will be auto-added!**

### Example 2: Array Operations

```markdown
**Filter and map array items:**

**ISL Code:**
```isl
fun run($data) {
    activeItems: foreach $item in $data.items | filter($item.active) {
        id: $item.id,
        name: $item.name | upperCase
    }
    endfor
}
```

**Input JSON:**
```json
{
  "items": [
    {"id": 1, "name": "apple", "active": true},
    {"id": 2, "name": "banana", "active": false},
    {"id": 3, "name": "orange", "active": true}
  ]
}
```
```

✨ **Button will be auto-added!**
```

## Styling

The playground button uses the `.btn-playground` CSS class defined in `docs/assets/css/main.scss`. You can customize the appearance by modifying this class.

Current styling features:
- Gradient purple background
- Hover effects with elevation
- Responsive sizing
- Mobile-friendly

## Configuration

### Playground URL

The default playground URL is `https://isl-playground.up.railway.app`. To change this:

1. Edit `docs/_plugins/auto_playground_buttons.rb`
2. Update the `playground_url` constant:

```ruby
playground_url = 'https://your-new-url.com'
```

### Jekyll Configuration

The Jekyll plugin runs automatically during site build. No additional configuration needed in `_config.yml`.

## Browser Compatibility

- Modern browsers (Chrome, Firefox, Safari, Edge)
- Requires JavaScript enabled
- Uses ES6 features (arrow functions, template literals, etc.)

## Testing

To test the integration:

1. Start the playground frontend:
   ```bash
   cd playground/frontend
   npm run dev
   ```

2. Serve the documentation locally:
   ```bash
   cd docs
   bundle exec jekyll serve
   ```

3. Navigate to a page with a playground button and click it
4. Verify the playground opens with the correct code and input

## Troubleshooting

### Button doesn't appear
- Check that code block has `isl` language tag
- Verify Jekyll build completed successfully  
- Check build logs for plugin errors

### Code doesn't load in playground
- Verify URL encoding is correct (check generated HTML)
- Check for special characters that need escaping
- Test with simpler code first

### Wrong playground URL
- Update `playground_url` in `auto_playground_buttons.rb`
- Rebuild the site
- Verify the playground is running

## Best Practices

1. **Keep examples simple** - Focus on one concept per example
2. **Provide complete input** - Include all required fields in sample JSON
3. **Test before committing** - Always verify the button works
4. **Use meaningful input** - Make examples realistic and relatable
5. **Add context** - Explain what the example demonstrates

## Future Enhancements

Potential improvements:
- [ ] Support for multiple ISL files (imports)
- [ ] Syntax highlighting in URL preview
- [ ] Share/copy URL functionality
- [ ] Embed playground directly in docs
- [ ] Support for expected output validation

