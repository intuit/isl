# ISL Playground Integration

This document explains how to add "Run in Playground" buttons to the ISL documentation.

## Overview

The playground integration allows documentation examples to be opened directly in the ISL Playground with pre-populated code and input JSON. This provides an interactive way for users to test and experiment with ISL code samples.

## How It Works

The system consists of three components:

1. **Playground Frontend** (`playground/frontend/src/App.tsx`) - Modified to accept URL parameters
2. **Jekyll Plugin** (`docs/_plugins/auto_playground_buttons.rb`) - Auto-adds buttons at build time
3. **Jekyll Include** (`docs/_includes/playground-button.html`) - Generates pre-encoded URLs

**Key Feature:** All encoding and button injection happens at build time via Ruby plugin!

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

### Method 1: Using Jekyll Include (Recommended)

Add the include statement after your code block. The encoding happens automatically at build time:

```markdown
**ISL Code:**
```isl
{
    id: $input.id,
    name: $input.name | upperCase
}
```

{% include playground-button.html 
   isl="{
    id: $input.id,
    name: $input.name | upperCase
}"
   input="{
  \"id\": 123,
  \"name\": \"John Doe\"
}" 
%}
```

**Parameters:**
- `isl` (required) - The ISL code to load
- `input` (optional) - The input JSON, defaults to `{}`
- `url` (optional) - Custom playground URL, defaults to `https://isl-playground.up.railway.app`

**Example with custom URL for production:**
```liquid
{% include playground-button.html 
   isl="{ result: $input.value }"
   input="{\"value\": 42}"
   url="https://your-playground.railway.app" 
%}
```

### Method 2: Pre-encode Manually

If Jekyll isn't available, pre-encode your strings using any base64 encoder:

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
```isl
{
    fullName: `${$input.firstName} ${$input.lastName}`,
    email: $input.email | lowerCase
}
```

{% include playground-button.html 
   isl="{
    fullName: `${$input.firstName} ${$input.lastName}`,
    email: $input.email | lowerCase
}"
   input="{
  \"firstName\": \"John\",
  \"lastName\": \"Doe\",
  \"email\": \"JOHN@EXAMPLE.COM\"
}" 
%}
```

### Example 2: Array Operations

```markdown
**Filter and map array items:**
```isl
fun run($data) {
    activeItems: foreach $item in $data.items | filter($item.active) {
        id: $item.id,
        name: $item.name | upperCase
    }
    endfor
}
```

{% include playground-button.html 
   isl="fun run($data) {
    activeItems: foreach $item in $data.items | filter($item.active) {
        id: $item.id,
        name: $item.name | upperCase
    }
    endfor
}"
   input="{
  \"items\": [
    {\"id\": 1, \"name\": \"apple\", \"active\": true},
    {\"id\": 2, \"name\": \"banana\", \"active\": false},
    {\"id\": 3, \"name\": \"orange\", \"active\": true}
  ]
}" 
%}
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

