---
title: Contributing
parent: Developer Guide
nav_order: 2
---

Before you embark on the journey to contribute, improve or change the ISL, make sure you understand the **principles of ISL**:

1. **LowCode** - ISL is a Low-code Interpreted Language highly focused on JSON to JSON transformations.
2. **Extensible** - ISL is highly and easily extensible, without the need to change the actual language. [ISL Hosts](./hosting.md#overview) can provide [extensions specific to their needs](./hosting.md#how-to-run-an-isl-file).
3. **Sandboxed** - ISL out of the box is designed to be 100% sandboxed with no capability for a developer to escape the sandbox by default.
   Any calls outside the sandbox **[are provided by the host](./hosting.md#overview)** not the ISL language. E.g.:

   - ApiCalls `@.Call.Api()`
   - FileAccess `@.Storage.Save()`
   - Calls to Other Services `@.Service.Get()`

4. **Secure by Default** - Because of its sandboxed design, ISL is **secure by default** blocking any code from escaping the sandbox. Any extension YOU or a HOST adds can break out of that sandbox and that becomes your responsibility of securing.
5. **Fault-Free** - ISL is designed to be pretty much fault-free, with no exceptions support and generally taking the path of doing the transformation no matter what, even if fields are missing.

   - Selectors like `$input.data.customer.lastname` should not crash if any of the elements in the path is missing. A `null` is the expected result.
   - Generally external functions like `@.Call.Api()` should return the complete result as a payload format

ISL was designed to be easily extensible to the point where a developer should not really be able to differentiate between an ISL native function (or modifier) or an extension function. Just like when you write Java you don't really differentiate between Java methods from the JDK vs methods from imported libraries.

## Understanding the ISL

Please review the following documents and make sure you understand how ISL works under the hood:

- [Hosting the ISL](./hosting.md#overview) - Make sure you understand how ISL is hosted and how it can be extended.

## ISL Improvements

If you understand the principles of ISL, then you should understand what changes are part of the language and what are part of the host.

Remember that any change you do to the ISL will be reflected in the hosts that are using the ISL so they need to be generic.

- Any processing or transformation that can be apply to any JSON
- Processing of strings, numbers, objects, xml, json, csv
- Processing of lists or arrays (maps, filters, groupby, reduce)
- Standard encodings
- Standard encryption, decryption, hashing
- Standard compressions
- Data Parsing and Conversions

## Host Improvements

Any extensions to the language that involve working with external api calls, custom features of the host, calling external services or capabilities are the responsibility of the host.
Read more in the [Hosting the ISL](./hosting.md#overview).
