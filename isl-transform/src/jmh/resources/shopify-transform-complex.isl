// Complex ISL transformation - Shopify order to internal format

// Helper function: Convert address
fun convertAddress( $addr ) {
    $street = $addr.address1 | trim;
    $city = $addr.city | trim;
    $state = $addr.province_code | trim | upperCase;
    $zip = $addr.zip | trim;
    
    return {
        street: $street,
        city: $city,
        state: $state,
        zipCode: $zip,
        country: $addr.country_code | upperCase,
        formatted: `${$street}, ${$city}, ${$state} ${$zip}`
    };
}

// Helper function: Convert customer
fun convertCustomer( $cust ) {
    $firstName = $cust.first_name | trim;
    $lastName = $cust.last_name | trim;
    
    return {
        id: $cust.id | to.string,
        fullName: `${$firstName} ${$lastName}`,
        firstName: $firstName,
        lastName: $lastName,
        email: $cust.email | lowerCase,
        phone: $cust.phone,
        totalOrders: $cust.orders_count | to.number,
        lifetimeValue: $cust.total_spent | to.decimal | precision(2),
        address: @.This.convertAddress( $cust.default_address )
    };
}

// Helper function: Process line item
fun processLineItem( $item ) {
    $price = $item.price | to.decimal;
    $qty = $item.quantity | to.number;
    
    return {
        itemId: $item.id | to.string,
        sku: $item.sku | upperCase,
        name: $item.name | trim,
        vendor: $item.vendor,
        quantity: $qty,
        unitPrice: $price,
        lineTotal: {{ $price * $qty }} | precision(2),
        weight: $item.grams
    };
}

// Main entry point
fun run( $input ) {
    // Pre-compute reused values
    $customer = @.This.convertCustomer( $input.customer );
    $shippingAddr = @.This.convertAddress( $input.shipping_address );
    $items = $input.line_items;
    $processedItems = $items | map( @.This.processLineItem( $ ) );
    
    // Financial calculations
    $total = $input.total_price | to.decimal;
    $discounts = $input.total_discounts | to.decimal;
    $finalTotal = {{ $total - $discounts }} | precision(2);
    
    // Status flags
    $fulfillmentStatus = $input.fulfillment_status | upperCase;
    $isPaid = if( $input.financial_status | lowerCase == "paid" ) true else false;
    $isFulfilled = if( $fulfillmentStatus == "FULFILLED" ) true else false;
    
    return {
        orderId: $input.id | to.string,
        orderNumber: $input.order_number | to.string,
        orderName: $input.name,
        
        // Customer info (from cached object)
        customer: $customer,
        
        // Shipping info (from cached object)
        shipping: {
            ...$shippingAddr,
            
            status: if( $isFulfilled ) "DELIVERED" else "PENDING",
            speed: if( $input.total_shipping_price_set.shop_money.amount | to.decimal >= 20 ) "EXPRESS" else "STANDARD",
            cost: $input.total_shipping_price_set.shop_money.amount | to.decimal | precision(2),
        },
        
        // Items (pre-processed)
        items: $processedItems,
        itemCount: $items | length,
        totalQuantity: $items | map( $.quantity | to.number ) | Math.sum(0),
        
        // Aggregations (showcasing ISL power)
        premiumItemCount: $items | filter( $.price | to.decimal >= 100 ) | length,
        vendors: $items | map( $.vendor ) | unique | sort,
        
        // Financial
        subtotal: $input.subtotal_price | to.decimal | precision(2),
        tax: $input.total_tax | to.decimal | precision(2),
        discounts: $discounts | precision(2),
        total: $total | precision(2),
        finalTotal: $finalTotal,
        currency: $input.currency | upperCase,
        
        // Payment & Status
        paymentStatus: $input.financial_status | upperCase,
        paymentMethod: $input.payment_gateway_names | first | upperCase,
        source: $input.source_name | upperCase,
        browserIp: $input.browser_ip,
        
        // Arrays (showcasing map/split)
        tags: $input.tags | split(",") | map( $ | trim | upperCase ),
        
        // Dates (showcasing date parsing)
        processedAt: $input.processed_at | date.parse("yyyy-MM-dd'T'HH:mm:ssXXX") | to.string("yyyy-MM-dd HH:mm:ss"),
        
        // Booleans & Conditionals
        isConfirmed: $input.confirmed | to.boolean,
        isTest: $input.test | to.boolean,
        isPaid: $isPaid,
        isFulfilled: $isFulfilled,
        
        // Summary (showcasing string templates)
        summary: `Order ${$input.order_number} - ${$customer.fullName} - ${$finalTotal} ${$input.currency | upperCase}`
    }
}

