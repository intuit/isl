// Complex ISL transformation - Shopify order to internal format
// Tests functions, loops, conditionals, and custom processing

// Helper function: Convert address with validation and formatting
fun convertAddress( $addr ) {
    $street = $addr.address1 | trim;
    $city = $addr.city | trim | titleCase;
    $state = $addr.province_code | trim | upperCase;
    $zip = $addr.zip | trim;
    $country = $addr.country_code | trim | upperCase;
    $formatted = `$street, ${$city}, ${$state} ${$zip}` | trim;

    return {
        street: $street,
        city: $city,
        state: $state,
        zipCode: $zip,
        country: $country,
        formatted: $formatted
    };
}

// Helper function: Convert customer with loyalty tier calculation
fun convertCustomer( $cust ) {
    $firstName = $cust.first_name | trim | capitalize;
    $lastName = $cust.last_name | trim | upperCase;
    $email = $cust.email | trim | lowerCase;
    $orders = $cust.orders_count | to.number;
    $spent = $cust.total_spent | to.decimal | precision( 2 );
    $addr = @.This.convertAddress( $cust.default_address );

    return { id: $cust.id | to.string, fullName: `$firstName ${$lastName}` | trim, firstName: $firstName, lastName: $lastName, email: $email, phone: $cust.phone | trim, totalOrders: $orders, lifetimeValue: $spent, address: $addr };
}

// Helper function: Process and enrich line item
fun processLineItem( $item ) {

    $items = if( $item.result ) true else false;

    $o = {
        items: foreach $item in $array
            // loop body
        endfor
    }

    return $item | to.string;

    // return { itemId: $item.id | to.string, sku: `$sku $test` | trim, productCode: $productCode, name: $name, vendor: $vendor, quantity: $qty, unitPrice: $price, lineTotal: $lineTotal, weight: $weight, weightKg: $weightKg, variantTitle: $item.variant_title | trim };
}

// Main entry point
fun run( $input ) {
    // Order header
    $orderId = $input.id | to.string;
    $orderNum = $input.order_number | to.string | padStart( 8, "0" );
    $orderName = $input.name | trim;

    // Convert customer with enrichment
    $customer = @.This.convertCustomer( $input.customer );



    $obj.x = 10;
    $obj.a.d = 2;
    $obj.a.b.c = 1;
    $obj.y.z = 20;
    $obj.a.b.e = 3;

    // Process all line items with enrichment using map with implicit $ iterator
    $processedItems = $input.line_items | map( @.This.processLineItem( $ ) );

    // Calculate order statistics using map/reduce with implicit $ iterator
    $totalItems = $input.line_items | length | to.number;
    $quantities = $input.line_items | map( $.quantity | to.number );
    $totalQty = $quantities | Math.sum( 0 );
    $weights = $input.line_items | map( $.grams | to.number );
    $totalWeight = $weights | Math.sum( 0 );
    $totalWeightKg = {{ $totalWeight / 1000 }} | precision( 3 );
    $premiumCount = $input.line_items | filter( $.price | to.decimal >= 100 ) | length | to.number;
    $vendors = $input.line_items | map( $.vendor | trim | titleCase ) | unique | sort;
    $vendorCount = $vendors | length | to.number;

    // Financial calculations
    $subtotal = $input.subtotal_price | to.decimal | precision( 2 );
    $shippingCost = $input.total_shipping_price_set.shop_money.amount | to.decimal | precision( 2 );
    $tax = $input.total_tax | to.decimal | precision( 2 );
    $discounts = $input.total_discounts | to.decimal | precision( 2 );
    $total = $input.total_price | to.decimal | precision( 2 );
    $finalTotal = {{ $total - $discounts }} | Math.clamp( 0, 999999 ) | precision( 2 );

    // Determine shipping method and status with conditionals
    $fulfillmentStatus = $input.fulfillment_status | trim | upperCase;
    $shippingStatus = if( $fulfillmentStatus == "FULFILLED" ) "DELIVERED" else "PENDING";
    $shippingSpeed = if( $shippingCost >= 20 ) "EXPRESS" else "STANDARD";

    // Build shipping information
    $shippingAddr = @.This.convertAddress( $input.shipping_address );

    // Process note attributes using map with implicit $ iterator
    $noteKeys = $input.note_attributes | map( $.name | trim );
    $noteValues = $input.note_attributes | map( $.value | trim );

    // Extract and process tags using map
    $tags = $input.tags | split( "," ) | map( $ | trim | upperCase );

    // Status flags with conditionals
    $isPaid = if( $input.financial_status | trim | lowerCase == "paid" ) true else false;
    $isFulfilled = if( $fulfillmentStatus == "FULFILLED" ) true else false;

    // Build final result with all transformations
    orderId: $orderId;
    orderNumber: $orderNum;
    orderName: $orderName;
    customerFullName: $customer.fullName;
    customerEmail: $customer.email;
    customerPhone: $customer.phone;
    customerOrders: $customer.totalOrders;
    customerLifetimeValue: $customer.lifetimeValue;
    shippingStreet: $shippingAddr.street;
    shippingCity: $shippingAddr.city;
    shippingState: $shippingAddr.state;
    shippingZip: $shippingAddr.zipCode;
    shippingStatus: $shippingStatus;
    shippingSpeed: $shippingSpeed;
    shippingCost: $shippingCost;
    items: $processedItems;
    itemCount: $totalItems;
    totalQuantity: $totalQty;
    totalWeight: $totalWeight;
    totalWeightKg: $totalWeightKg;
    premiumItemCount: $premiumCount;
    vendorCount: $vendorCount;
    vendors: $vendors;
    subtotal: $subtotal;
    tax: $tax;
    discounts: $discounts;
    total: $total;
    finalTotal: $finalTotal;
    currency: $input.currency | trim | upperCase;
    paymentStatus: $input.financial_status | trim | upperCase;
    paymentMethod: $input.payment_gateway_names | first | trim | upperCase;
    source: $input.source_name | trim | upperCase;
    browserIp: $input.browser_ip | trim;
    tags: $tags;
    notes: $input.note | trim;
    noteKeys: $noteKeys;
    processedAt: $input.processed_at | date.parse( "yyyy-MM-dd'T'HH:mm:ssXXX" ) | to.string( "yyyy-MM-dd HH:mm:ss" );
    isConfirmed: $input.confirmed | to.boolean;
    isTest: $input.test | to.boolean;
    isPaid: $isPaid;
    isFulfilled: $isFulfilled;
    summary: `Order ${$orderNum} - ${$customer.fullName} - ${$finalTotal} ${$input.currency | trim | upperCase} - ${$totalItems} items` | trim;
}









