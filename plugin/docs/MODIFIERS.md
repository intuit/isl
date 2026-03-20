# ISL Modifiers Reference

This document lists all built-in modifiers available in ISL.

## String Modifiers

### Trimming
- `trim` - Trim whitespace from both sides
- `trimStart` - Trim whitespace from start
- `trimEnd` - Trim whitespace from end

### Case Conversion
- `upperCase` - Convert to uppercase
- `lowerCase` - Convert to lowercase
- `capitalize` - Capitalize first letter
- `titleCase` - Convert to title case (capitalize each word)
- `camelCase` - Convert to camelCase
- `snakeCase` - Convert to snake_case

### Substring Operations
- `left(n)` - Get first n characters
- `right(n)` - Get last n characters
- `cap(n)` - Cap string at length (alias for left)
- `substring(start, end)` - Get substring
- `substringUpto(delimiter)` - Get substring up to delimiter
- `substringAfter(delimiter)` - Get substring after delimiter

### String Manipulation
- `replace(find, replace)` - Replace all occurrences
- `remove(text)` - Remove substring
- `concat(other, delimiter)` - Concatenate with delimiter
- `append(values...)` - Append multiple strings
- `split(delimiter)` - Split into array
- `truncate(length, suffix)` - Truncate with suffix (default "...")
- `padStart(length, char)` - Pad at start
- `padEnd(length, char)` - Pad at end
- `reverse` - Reverse string

### HTML
- `html.escape` - Escape HTML entities
- `html.unescape` - Unescape HTML entities

### CSV
- `csv.parsemultiline` - Parse CSV string
- `csv.findrow` - Find specific row in CSV

### Other
- `sanitizeTid` - Sanitize/validate UUID/TID

## Array Modifiers

### High-Order Functions
- `filter(condition)` - Filter array by condition (use $fit or $ for current item)
- `map(expression)` - Transform each element (use $ for current item)
- `reduce(expression, initial)` - Reduce to single value (use $acc and $it)

### Element Access
- `first` - Get first element
- `last` - Get last element
- `at(index)` - Get element at index (supports negative indices)
- `take(n)` - Take first n elements
- `drop(n)` - Drop first n elements

### Search
- `indexOf(element)` - Find index of element
- `lastIndexOf(element)` - Find last index of element

### Manipulation
- `push(item)` - Add item to end
- `pop` - Remove last item
- `pushItems(array)` - Append entire array
- `slice(start, end)` - Extract portion
- `chunk(size)` - Split into chunks of size

### Transformation
- `sort` - Sort array
- `reverse` - Reverse array
- `unique` - Get unique values

### Checks
- `isEmpty` - Check if empty
- `isNotEmpty` - Check if not empty
- `length` - Get length

## Object Modifiers

### Property Access
- `keys` - Get object keys as array
- `kv` - Get key-value pairs as array of {key, value}
- `getProperty(name)` - Get property (case-insensitive)
- `setProperty(name, value)` - Set property
- `has(key)` - Check if object has key
- `delete(property)` - Delete property

### Object Operations
- `sort` - Sort object by keys
- `select(path)` - Select nested property by JSON path
- `merge(other)` - Merge with another object
- `pick(keys...)` - Pick specific properties
- `omit(keys...)` - Omit specific properties
- `rename(oldName, newName)` - Rename property
- `default(value)` - Return default if null/empty

## Math Modifiers

### Array Math (use with arrays)
- `Math.sum(initial)` - Sum all values
- `Math.average` - Average of values
- `Math.mean` - Mean of values
- `Math.min` - Minimum value
- `Math.max` - Maximum value

### Numeric Operations
- `Math.mod(divisor)` - Modulo operation
- `Math.sqrt` - Square root
- `Math.round` - Round to nearest integer
- `Math.floor` - Round down
- `Math.ceil` - Round up
- `Math.abs` - Absolute value

### Number Modifiers
- `negate` - Negate number
- `absolute` - Absolute value
- `precision(digits)` - Set decimal precision
- `round.up` - Round up
- `round.down` - Round down
- `round.half` - Round half

### Random Numbers
- `Math.RandInt(min, max)` - Random integer
- `Math.RandFloat()` - Random float
- `Math.RandDouble()` - Random double

## Type Conversion Modifiers

### Basic Types
- `to.string` - Convert to string
- `to.number` - Convert to number
- `to.decimal` - Convert to decimal
- `to.boolean` - Convert to boolean
- `to.array` - Convert to array
- `to.object` - Convert to object

### Format Conversions
- `to.json` - Convert to JSON string
- `to.yaml` - Convert to YAML string
- `to.csv` - Convert to CSV string
- `to.xml(rootName)` - Convert to XML
- `to.hex` - Convert to hex string
- `to.bytes` - Convert to byte array
- `to.epochmillis` - Convert date to epoch milliseconds

### Hex Conversion
- `hex.tobinary` - Convert hex string to binary

### Join Operations
- `join.string(delimiter)` - Join array to string
- `join.path(delimiter)` - Join with URL path encoding
- `join.query(delimiter)` - Join with URL query encoding

## Parsing Modifiers

### Data Formats
- `json.parse` - Parse JSON string
- `yaml.parse` - Parse YAML string
- `xml.parse` - Parse XML string

### Email
- `email.parse` - Parse email addresses

## Encoding Modifiers

### Base64
- `encode.base64` - Encode to Base64
- `encode.base64url` - Encode to Base64 URL-safe
- `decode.base64` - Decode from Base64
- `decode.base64url` - Decode from Base64 URL-safe

### URL Encoding
- `encode.path` - URL path encoding
- `encode.query` - URL query encoding
- `decode.query` - URL query decoding

## Compression Modifiers

- `gzip` - GZip compress
- `gunzip` - GZip decompress to string
- `gunzipToByte` - GZip decompress to byte array

## Regex Modifiers

- `regex.find(pattern)` - Find all matches
- `regex.matches(pattern)` - Test if matches
- `regex.replace(pattern, replacement)` - Replace all matches
- `regex.replacefirst(pattern, replacement)` - Replace first match

Options can be passed as third parameter: `{ignoreCase: true, multiLine: true, comments: true}`

## Type Checking

- `typeof` - Get type of value (returns: string, number, boolean, array, object, null, etc.)

## Legacy/Common Modifiers

- `contains(value)` - Check if string contains substring
- `startsWith(prefix)` - Check if starts with
- `endsWith(suffix)` - Check if ends with

## Date Modifiers

- `date.parse(format)` - Parse date string
- `date.add(value, unit)` - Add to date (units: YEARS, MONTHS, DAYS, HOURS, MINUTES, SECONDS)
- `date.part(part)` - Get date part (YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, DAYOFYEAR, DAYOFWEEK)
- `date.fromEpochSeconds` - Create date from epoch seconds
- `date.fromEpochMillis` - Create date from epoch milliseconds

## Static Functions (use with @.)

### Math Functions
- `@.Math.min(values...)` - Minimum of values
- `@.Math.max(values...)` - Maximum of values
- `@.Math.mean(values...)` - Mean of values
- `@.Math.mod(value, divisor)` - Modulo operation
- `@.Math.sqrt(value)` - Square root
- `@.Math.RandInt(min, max)` - Random integer
- `@.Math.RandFloat()` - Random float
- `@.Math.RandDouble()` - Random double

### Array Functions
- `@.Array.range(start, count, increment)` - Create numeric range array

### Date Functions
- `@.Date.now()` - Current date/time
- `@.Date.parse(string, format)` - Parse date
- `@.Date.fromEpochSeconds(seconds)` - From epoch seconds
- `@.Date.fromEpochMillis(millis)` - From epoch milliseconds

## Special Variables in Modifiers

### filter
- `$fit` or `$` - Current item being filtered

### map
- `$` - Current item being mapped

### reduce
- `$acc` - Accumulator
- `$it` - Current item

### foreach
- `$item` - Loop variable
- `$itemIndex` - Zero-based index

### retry
- `$` - Current value being tested

