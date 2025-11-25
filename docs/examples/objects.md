---
title: Object Examples
parent: Examples
nav_order: 2
---

When building deep object structures the recommendation is to build them in a structured JSON format way instead of using long property names:

Old:
```isl
$t = {
    headers: $address.headers,
    headers.namespace: $eventNamespace,
    headers.type: $eventName,
    headers.version: $eventVersion,

    payload.data: @.This.transformAddress($address.payload),
    payload.data.id :  $address.payload.id | concat('-' ) | concat( $address.payload.customerId ),
    payload.data.otherIds: @.This.transformExternalIds($address.payload),
    payload.data.deletedAt: if ($address.payload.active == false) @.This.transformDate() endif,

    transform.status: if ( $realmId ) "VALID" else "INVALID" endif,
    transform.failureReasons: if ( !$realmId ) "Missing $payload.id.accountId due to which entity could not be created." endif | to.array
}
```

New:
```isl
$t = {
    headers: {
        ...$source.headers,  // spread existing headers into the new headers
        namespace: $eventNamespace,
        type: $eventName,
        version: $eventVersion,
        eventName: `source_$eventName` | lowerCase,
    },

    payload: {
        data: {
            id: `${ $address.payload.id }-${ $address.payload.customerId }`,

            ... @.This.transformAddress($address.payload),   // spread the result into the data

            otherIds: @.This.transformExternalIds($address.payload),
            deletedAt: if ($address.payload.active == false) @.This.transformDate() endif,
        },
    },

    transform: {
        status: if ( $realmId ) "VALID" else "INVALID" endif,
        failureReasons: if ( !$realmId ) "Missing $payload.id.accountId due to which entity could not be created." endif | to.array
    }
}
```