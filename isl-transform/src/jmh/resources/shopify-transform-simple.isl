// Simple ISL transformation matching JOLT capabilities
// No modifiers, no functions, just basic field mapping like JOLT

fun run($input) {
    orderId: $input.id;
    orderNumber: $input.order_number;
    orderName: $input.name;
    
    // Customer fields
    customerId: $input.customer.id;
    customerFirstName: $input.customer.first_name;
    customerLastName: $input.customer.last_name;
    customerEmail: $input.customer.email;
    customerPhone: $input.customer.phone;
    customerOrders: $input.customer.orders_count;
    customerLifetimeValue: $input.customer.total_spent;
    
    // Customer address
    customerStreet: $input.customer.default_address.address1;
    customerCity: $input.customer.default_address.city;
    customerState: $input.customer.default_address.province_code;
    customerZip: $input.customer.default_address.zip;
    customerCountry: $input.customer.default_address.country_code;
    
    // Shipping address
    shippingStreet: $input.shipping_address.address1;
    shippingCity: $input.shipping_address.city;
    shippingState: $input.shipping_address.province_code;
    shippingZip: $input.shipping_address.zip;
    shippingCountry: $input.shipping_address.country_code;
    
    // Line items - map array
    items: $input.line_items | map({
        itemId: $.id,
        sku: $.sku,
        name: $.title,
        vendor: $.vendor,
        quantity: $.quantity,
        unitPrice: $.price,
        weight: $.grams,
        productId: $.product_id,
        variantTitle: $.variant_title
    });
    
    // Financial fields
    subtotal: $input.subtotal_price;
    shippingCost: $input.total_shipping_price_set.shop_money.amount;
    tax: $input.total_tax;
    discounts: $input.total_discounts;
    total: $input.total_price;
    currency: $input.currency;
    
    // Status fields
    paymentStatus: $input.financial_status;
    fulfillmentStatus: $input.fulfillment_status;
    paymentMethod: $input.payment_gateway_names[0];
    
    // Other fields
    source: $input.source_name;
    browserIp: $input.browser_ip;
    tagsRaw: $input.tags;
    notes: $input.note;
    
    // Note attributes - map array
    noteAttributes: $input.note_attributes | map({
        name: $.name,
        value: $.value
    });
    
    processedAt: $input.processed_at;
    isConfirmed: $input.confirmed;
    isTest: $input.test;
    
    // Default values (matching JOLT defaults)
    itemCount: 0;
    totalQuantity: 0;
    totalWeight: 0;
    totalWeightKg: 0;
    premiumItemCount: 0;
    vendorCount: 0;
    finalTotal: 0;
    shippingStatus: "PENDING";
    shippingSpeed: "STANDARD";
    isPaid: false;
    isFulfilled: false;
}

