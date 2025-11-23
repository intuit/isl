---
title: Cryptography
parent: Advanced Topics
nav_order: 2
description: "ISL cryptography utilities for SHA256, HMAC signatures, and Base64 encoding. Create secure hashes and sign data in your transformations."
excerpt: "ISL cryptography utilities for SHA256, HMAC signatures, and Base64 encoding. Create secure hashes and sign data in your transformations."
---

## Overview
- `$value | crypto.sha256` - generates a SHA256 signature for the specified value. The result is a byte array which should be manually convert to hex using `to.hex` or `encode.base64` for a Base64 encoding.
   By default the value will be converted to a string using Base64 encoding.
- `$value | crypto.hmacsha256( key )` - generate an HMAC Sha256 for a value using a specified key.
- Supported Secure Hash Algorithms:
    - `crypto.sha1`
    - `crypto.sha256`
    - `crypto.sha512`
- Supported HMAC hash algorithms:
    - `crypto.hmacsha256( key )`
    - `crypto.hmacsha384( key )`
    - `crypto.hmacsha512( key )`
- Supported RSA has algorithms:
  - `crypto.rsasha256`
- Support for MD5
  - `crypto.md5`

### Sha256 examples
```isl
$r: "my sha test" | crypto.sha256 | to.hex;	
// $r is "77f03ce7057b27586d97cc432ac033b628494e651c90131c63ee8b651a6f9e18"
```

### Sha512 examples
```isl
$r: "my sha test" | crypto.sha512 | to.hex;	
// $r is "5e3c77245fa1ceeafccc815f2e34c65b39e00c85bcb95556d5ff4913fc8544aa461b604db1f5e5dd2b2fe37f84236bf263694cdd5adf4040faa10e1c37addbbb"
```

### HmacSha256 Examples
Example of calculating the [custom Amazon AWS API Signature](../examples/signatures.md#amazon-merchant-services-signature)

### rsaSha256 Examples
```java

// Pass PrivateKey to the crypto method
$r: "my rsa test"  | crypto.rsasha256( $privateKey );

// Pass Keystore to the crypto method
$r: "my rsa test"  | crypto.rsasha256( $keystore,  $keystorePassword, $keystorealias );
```

### MD5 Example
```isl

// This is Base64 encoded by default
$md5_hash: "string_to_hash" | crypto.md5;
// Results in $md5_hash="nsv3c2mnRugLZzQNmYBrfw=="

// Explicitly convert to hex
$md5_hash_hex: "string_to_hash" | crypto.md5 | to.hex;
// Results in $md5_hash_hex="9ecbf77369a746e80b67340d99806b7f"
```

### Helper Methods
- `$keystore | @.Crypto.toKeyStore( keystoreType, keystorePassword )` - Load a keystore from a base64 encoded file.

