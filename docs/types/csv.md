---
title: CSV
parent: Data Types
nav_order: 3
---

`| csv.parsemultiline( options )` - parses a multi line CSV text and returns an array of objects.
By default the `csv.parsemultiline` will assume first row is the `headers` and the delimiter is `,`.

**Available Options:**

- `{ separator: "|" }` - use the `|` separator. Default value: `,`.
- `{ escapeChar: "\\" }` - char used for escaping the separator. Default value: `\`. There is no way to completely disable the `escapeChar`.
- `{ quoteChar: "\"" }` - char used for quotes. If null or an empty string are provided, it will ignore quote characters.
- `{ skipLines: 3 }` - skip a number of lines. Default Value: `0`.
- `{ headers: [ "c1", "c2" ]}` - use these default headers. This automatically assumes Row 0 is a valid row.
Columns with no known header name are automatically created with name `ColX` with the `X` being the positional column name.

Expression:
```
// default parsing
result1: "a,b,c\n1,2,3" | csv.parsemultiline
// using custom headers
result2: "a,b,c\n1,2,3" | csv.parsemultiline( { headers: ["x", "y", "z"] })
// custom headers & skipLines
result3: "a,b,c\n1,2,3" | csv.parsemultiline( { headers: ["x", "y", "z"], skipLines: 1 })
// extra fields
result4: "a,b,c\n1,2,3,4,5" | csv.parsemultiline
// missing fields
result5: "a,b,c\n1,2" | csv.parsemultiline
```
Evaluates to:
```json
{
	"result1": [ { "a": "1", "b": "2", "c": "3" } ],
	"result2": [ { "x": "a", "y": "b", "z": "c" }, { "x": "1", "y": "2", "z": "3" } ],
	"result3": [ { "x": "1", "y": "2", "z": "3" } ],
	"result4": [ { "a": "1", "b": "2", "c": "3", "Col4": "4", "Col5": "5" } ],
	"result5": [ { "a": "1", "b": "2" } ]
}
```

`| csv.findRow( options )` - returns the zero based index of first row that contains a specific set of values. This can be used to search for the header row if it's location in the file is not known. Use the `skipLines` option of `parsemultiline` to ignore unwanted lines.

**Available Options:**

- `{ seek: [ "c1", "c2" ]}` - list of the values to look for in each line. The order does not matter. Not all values have to be listed. This is case sensitive.
- `{ maxRows: value }` - stop searching after encountering a certain numebr of rows. Prevents scanning the entire csv if we know the row we are looking for is towards the beginning of the file.
- `{ separator: "|" }` - use the `|` separator. Default value: `,`.
- `{ escapeChar: "\\" }` - char used for escaping the separator. Default value: `\`. There is no way to completely disable the `escapeChar`.
- `{ quoteChar: "\"" }` - char used for quotes. If null or an empty string are provided, it will ignore quote characters.
- `{ skipLines: 3 }` - skip a number of lines. Default Value: `0`.

Expression:
```
result1 : "a,b,c\n1,2,3" | csv.findRow( { seek: ["a", "b", "c"] })
result2 : "a,b,c\n1,2,3" | csv.findRow( { seek: ["3", "1"] })
result3 : "a,b,c\n1,2,3" | csv.findRow( { seek: ["x", "y", "z"] })
result3 : "a,b,c\n1,2,3" | csv.findRow( { seek: ["a", "b", "c", "d"] })
```
Evaluates to:
```json
{
	"result1": 0,
	"result2": 1,
	"result3": null,
	"result4": null
}
```