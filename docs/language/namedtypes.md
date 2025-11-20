---
title: Named Types
parent: Language Reference
nav_order: 9
---

Since ISL 2.4.0, Named Types Declaration is supported, in which a specific entity can be declared as having a type that the host understands.

For example for hosts can declare a variable as having a known types.
Type declaration follows the format of `$variable: Type = value`

```
$account: mycompany.account = {
	id: {
		externalId: "1234"
	},
	accountNumber: "123",
	accountType: "DEPOSIT"
}
```

ISL will not directly validate the type however the host (e.g. ACE) can request the type to be validated before it's used in operations like Publishing to the EventBus.

The JSON object received back in the Kotlin on any operation will be of type `TypedObjectNode` that will carry the extra type information of `mycompany.account` to allow the host to validate the schema of the object.

Schema validations can be done either explicitly in ISL by using a registered `| validate.type` modifier or in Kotlin before the object is serialized.

**We recommend the explicit Kotlin validation to be used to reduce the risk of someone not calling the validation from ISL.**

## Kotlin Named Type Validations

In order to validate the objects from Kotlin you need to use the `IslSchemaProcessor`.
The constructor requires a callback that will be called to ask the host for a copy of the schema with the specified `schemaName` or `schemaUrl`.
For the example above the `schemaName` will be `mycompany.account`. If that schema has references to other schemas via `$ref` then the callback will be called with the extra urls in the `schemaUrl`.

```kotlin
val schemaProcessor = IslSchemaProcessor(options) { schemaName, schemaUrl ->
	// TODO: find either the `schemaName` or the `schemaUrl` and return it back to the processor
	val schemaContents = ...
	return@IslSchemaProcessor IslSchema(schemaContents)
};


val result = schemaProcessor.processObject(jsonValue)
```

`result` will contain a list of issues as reported by the validation in format:

```json
{
  "success": false, // or true
  "issues": [
    {
      "type": "Required", // error type
      "location": "$.externalId", // json path to error
      "expected": "expected value or type",
      "message": "detailed error message"
    }
  ]
}
```

The `type` can be one of the followings:

- `Required` - required field missing
- `Type` - incorrect field type
- `Rules` - value does not match the rules defined on the type
- `Advanced` - advanced validation error - see below for [Advanced Validations](#advanced-validations)

## Advanced Validations

ISL objects support advanced ISL Validations where you can add custom ISL to the schema to enhance the validation.
For example you can do math calculations of totals, validate a tax amount is correct or do any other custom validations.

In order to use advanced validations just add an extra `validate` attribute containing the ISL doing a validation to the fields in the schema definition.
The script will be executed and passed two values `$field` the actual field being validated and `$entity` the top level entity the field is part of.

Here's an example of a schema that contains advanced ISL validations. The fields `externalId` and the main entity will have the advanced ISL triggered to validate it. To raise an error just call `@.Validation.Error( code, message, expected )`:

```yaml
$id: "https://basicschema/schemas/basicschema.yaml"
required:
  - id
  - externalId
properties:
  id:
    type: string
  externalId:
    type: string
    validate: | # field level validation - you'll receive $field and $entity
      if( $field contains "xyz" )
        @.Validation.Error("error", "The external Id can't contain 'xyz'")
      endif
  type:
    $ref: "./ExternalIdType.yaml#/components/schemas/ExternalIdType"
  version:
    type: number
    description: "Version number as per connectionSource"
    minimum: 0
    multipleOf: 5

# object level validation
validate: |
  if( $entity.version > 55 )
    @.Validation.Error("mycode", "$.version needs to be smaller than 55" )
  endif
```

## Custom Processing

The `IslSchemaProcessor` also allows other types of post-processing on the received objects based on custom processing attributes declared in the schema.
This allows for very advanced post processing, for example field masking, field encryption or field tokenization.

In order to use the custom processing just define the trigger property in the schema that will be used to trigger your processing:

For example this schema ads an extra `x-tokenize:true` property:

```yaml
$id: "https://basicschema/schemas/basicschema.yaml"
required:
  - id
  - externalId
properties:
  id:
    type: string
  externalId:
    type: string
    validate: | # field level validation - you'll receive $field and $entity
      if( $field contains "xyz" )
        @.Validation.Error("error", "The external Id can't contain 'xyz'")
      endif
    x-tokenize: true # trigger custom processing
  type:
    $ref: "./ExternalIdType.yaml#/components/schemas/ExternalIdType"
  version:
    type: number
    description: "Version number as per connectionSource"
    minimum: 0
    multipleOf: 5
```

Then in the Kotlin code yuo can define your own processor for any field that's marked as `x-tokenize:true` and do any processing you want.
You can mutate the node or you can return any custom validation errors:

```kotlin
// define a processor
val tokenize: SchemaKeywordProcessor = { node ->
	// we can either modify the node or return a validation error
	// let's assume I'm tokenizing this
	val newNode = JsonNodeFactory.instance.textNode(
		node.textValue() + "-TOKENIZED"
	)
	println("Tokenized ${node} > ${newNode}")
	// return a Pair containing either:
	// newNode -  that will replace the current node
	// Array<ValidationMessage> - array of validation messages that will append to the list of validations
	Pair(newNode, null)
};

val options = IslSchemaProcessingOptions(
	keywordProcessors = mapOf(
		// no register the processor for the `x-tokenize`
		"x-tokenize" to tokenize
	)
)

val schemaProcessor = IslSchemaProcessor(options) { schemaName, schemaUrl ->
	// TODO: find either the `schemaName` or the `schemaUrl` and return it back to the processor
	return@IslSchemaProcessor IslSchema(schemaContents)
};

val result = schemaProcessor.processObject(jsonValue, processOptions)
```

If the schema is found and the schema contained an `x-tokenize` then the `result` will contain the updated and tokenized field for the `externalId`.
