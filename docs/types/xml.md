---
title: XML
parent: Data Types
nav_order: 4
---

## XML Parsing
`| xml.parse` - parses an XML text and converts it to a JSON payload. The root object of the XML is dropped and the root JSON will contain just the attributes and children of the root.

```xml
<Simple>
  <x>1</x>
  <y>2</y>
</Simple>
```
Evaluates to:
```json
{
	"x": "1",
	"y": "2"
}
```

`| xml.parse` - returns `null` when parsing an invalid XML text.

```isl
$t: "abc";
$result: $t | xml.parse;
```
Evaluates to:
```json
null
```

## XML Rendering
`| to.xml( rootName )` - output an object to XML Format.
**Note:** The output of this is not a direct reverse of `| xml.parse`.

Output rules:

- Property Names starting with `@` are rendered as XML Attributes: `{ a: { '@b': 'value', c: 1 }}` > `<a b="value"><c>1</c></a>`
- Property Named `#text` is rendered as the contents `{ a: { '@b': 'value', '#text': 'my text' }}` > `<a b="value">my text</a>`
- Property Names are rendered as XML Elements `{ a: 1 }` > `<a>1</a>`
- You can't mix `#text` with normal XML Elements
- Property name of Array is rendered as the Array Item `{ a: [ 1, 2 ]}` > `<root><a>1</a><a>2</a></root>`
- Root Element is `root` by default unless overwritten by the `to.xml( rootName )`

```isl
{
	ke: {
		'@name': 'my name',
		items: {
			item: [ 1, 2 ]
		}
	},
	other: {
		'#text': 'my text'
	}
} | to.xml('myRoot')
```
Evaluates to:
```xml
<myRoot>
	<ke name="my name">
		<items>
			<item>1</item>
			<item>2</item>
		</items>
	</ke>
	<other>my text</other>
</myRoot>
```

### Text Handling
The text inside an XML Element is created in property `#text` if the element has any other attributes 
and can be accessed through the `$var.["#text"]` selector.
If the element has no other attributes or children then the text is the actual value of the property:


```xml
<Simple>
	<A>text</A>
	<B attr1="a1">text2</B>
</Simple>
```

Evaluates to:
```json
{
	"A": "text",
	"B": {
		"attr1": "a1",
		"#text": "text2"
	}
}
```

### Arrays Handling
Arrays will generate array elements inside the duplicated property:
```xml
<Simple>
  <x>a</x>
  <x>b</x>
</Simple>
```

Evaluates to:
```json
{
	"x": [
		"a",
		"b"
	]
}
```