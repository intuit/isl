---
title: Object Examples
parent: Examples
nav_order: 2
---

When building deep object structures the recommendation is to build them in a structured JSON format way instead of using long property names:

Old:
```isl
$t = {
    headers: $qboAddress.headers,
    headers.entity_namespace: $eventNamespace,
    headers.entity_type: $eventName,
    headers.intuit_realmid: $realmId,
    headers.intuit_tid: $headers.intuitTid,
    headers.entity_version: $eventVersion,
    headers.provider_id: `mailchimp_$eventName` | lowerCase,
    headers.connection_id: if ( $realmId ) `$realmId-com.mailchimp` endif,

    payload.data.id.externalId :  $qboAddress.payload.id.entityId | append('-', $qboAddress.payload.id.accountId),
    payload.data: @.This.transformAddress($qboAddress.payload),
    payload.data.externalIds: @.This.transformExternalIds($qboAddress.payload),
    payload.data.deletedAt: if ($qboAddress.payload.active == false) @.This.transformDate() endif,

    transform.status: if ( $realmId ) "VALID" else "INVALID" endif,
    transform.failureReasons: if ( !$realmId ) "Missing $payload.id.accountId due to which connection_id could not be created." endif | to.array
}
```

New:
```isl
$t = {
    headers: {
        ...$qboAddress.headers,  // spread existing headers into the new headers
        entity_namespace: $eventNamespace,
        entity_type: $eventName,
        intuit_realmid: $realmId,
        intuit_tid: $headers.intuitTid,
        entity_version: $eventVersion,
        provider_id: `mailchimp_$eventName` | lowerCase,
        connection_id: if ( $realmId ) `$realmId-com.mailchimp` endif,
    },

    payload: {
        data: {
            id: {
                externalId: `${ $qboAddress.payload.id.entityId }-${ $qboAddress.payload.id.accountId }`,
            },
            ... @.This.transformAddress($qboAddress.payload),   // spread the result into the data
            externalIds: @.This.transformExternalIds($qboAddress.payload),
            deletedAt: if ($qboAddress.payload.active == false) @.This.transformDate() endif,
        },
    },

    transform: {
        status: if ( $realmId ) "VALID" else "INVALID" endif,
        failureReasons: if ( !$realmId ) "Missing $payload.id.accountId due to which connection_id could not be created." endif | to.array
    }
}
```