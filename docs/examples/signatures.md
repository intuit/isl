---
title: Signature Examples
parent: Examples
nav_order: 3
---

## Amazon Merchant Services Signature

[AMS Signature Documentation](https://docs.aws.amazon.com/general/latest/gr/sigv4-calculate-signature.html)

```isl
fun generateSignature( $request ){
    // ToDo: Replace this with a call to get the actual URL we will get
    // This will be based on the users Marketplace selection.
    //$baseUrl = @.Properties.Auth("baseUrl");
    $baseUrl = "https://sellingpartnerapi-na.amazon.com";

    $region = @.This.getRegion($baseUrl);

    // Read Task 4 for details https://docs.aws.amazon.com/general/latest/gr/sigv4-add-signature-to-request.html

    $accessKeyId = "accesskey" ;
    $key = "secretkey";

    $awsRequest = "aws4_request";
    $awsService = "execute-api";
    $signingAlgorithm = "AWS4-HMAC-SHA256";
    $body = if ( $request.body ) $request.body else "";

    // If Authorization header exists delete it.
    if( $request.headers.Authorization )
        $request.headers: $request.headers | delete( "Authorization" )
    endif

    // if the NextToken is null remove it.
    if( !$request.query.NextToken )
        $request.query: $request.query | delete( "NextToken" )
    endif
    // NextToken can be lower case
    if( !$request.query.nextToken )
        $request.query: $request.query | delete( "nextToken" )
    endif

    // todo: sort the query params
    // $sorted: $request.query | sort;
    // $request.query: null;
    // $request.query: $sorted;

    $nowTime = @.Date.Now() | to.string( "yyyyMMdd'T'HHmmss'Z'" );
    $date = @.Date.Now() | to.string( "yyyyMMdd" );

    // Task 1: Create & Hash the Canonical Request
    // https://docs.aws.amazon.com/general/latest/gr/sigv4-create-canonical-request.html

    // Step 4: Calculate canonical headers
    $request.headers.["x-amz-date"] = $nowTime;	// add the time stamp
    // Get the latest OAuth token.
    $request.headers.["x-amz-access-token"] = @.Properties.Auth( "Authorization" );

    $sortedHeaders = $request.headers | sort;

    $signedHeaders = $sortedHeaders | keys | join.string ( ";" );

    // Step 7: Prepare Canonical Request
    $toSign: [
        "GET",
        $request.path | encode.path,				// path encoding %
        $request.query | join.query( "&", "=" ),	// query encoding +
        $sortedHeaders | join.string( "\n", ":" ),
        "",
        $signedHeaders,
        $body | crypto.sha256 | to.hex
    ] | join.string( "\n" );

    $hashedRequest = $toSign | crypto.sha256 | to.hex;

    // Task 2: Calculate a string to sign
    // https://docs.aws.amazon.com/general/latest/gr/sigv4-create-string-to-sign.html
    $toSign = [
        $signingAlgorithm,
        $nowTime,
        `$date/$region/$awsService/$awsRequest`,
        $hashedRequest
    ] | join.string("\n");


    // Task 3: Calculate the Signature for AWS Signature V4
    // https://docs.aws.amazon.com/general/latest/gr/sigv4-calculate-signature.html

    // Step 1: Sign
    $fullKey = `AWS4$key`;
    $kDate =      $date | crypto.hmacsha256 ( $fullKey );

    $kRegion =    $region | crypto.hmacsha256 ( $kDate );
    $kService =   $awsService | crypto.hmacsha256 ( $kRegion );
    $signingKey = $awsRequest | crypto.hmacsha256 ( $kService );

    $signature =  $toSign | crypto.hmacsha256( $signingKey ) | to.hex;

    // Task 4: Add the signature to the request
    // https://docs.aws.amazon.com/general/latest/gr/sigv4-add-signature-to-request.html

    $authHeader = `$signingAlgorithm Credential=$accessKeyId/$date/$region/$awsService/$awsRequest, SignedHeaders=$signedHeaders, Signature=$signature`;

    // Set Authorization Header - we can do the API call now
    $request.headers.Authorization = $authHeader;

    // Removing the host from the headers for testing....
    // $request.headers: $request.headers | delete( "host" )

    $request.defaultHeaders: false;

    return $request;
}
```

## Kraken Signature

Generate a [Kraken signature](https://docs.kraken.com/rest/#section/Authentication/Headers-and-Signature)

```isl
fun test(){
    // https://docs.kraken.com/rest/#section/Authentication/Headers-and-Signature
    // HMAC-SHA512 of (URI path + SHA256(nonce + POST data)) and base64 decoded secret API key

    $privateKey = '...'
    $nonce = '1616492376594'
    $payload = `nonce=$nonce&ordertype=limit&pair=XBTUSD&price=37500&type=buy&volume=1.25`
    $uriPath = '/0/private/AddOrder'

    $sig = @.This.calculateSignature( $privateKey, $nonce, $payload, $uriPath );
}

fun calculateSignature($privateKey, $nonce, $uriPath, $payload) {
    $decodedKey = $privateKey | decode.base64;

    $toSha = `$nonce$payload`;

    // sha of the nonce and payload
    $sha = `$nonce$payload` | crypto.sha256 | to.hex;
    $hexPath = $uriPath | to.hex;

    // build the two parts as their hex values
    $toHmac = `$hexPath$sha`;

    $sig = $toHmac | hex.tobinary | crypto.hmacsha512($decodedKey)
    return $sig;
}
```
