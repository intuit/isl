"""
Python transformation - Shopify order to internal format
Matches the simple field mapping capabilities of JOLT/ISL/MVEL
"""

def transform_shopify_order(*args, **kwargs):
    """
    Transform Shopify order JSON to internal format
    
    Args:
        input_data: Parsed JSON object (dict) from Java
        
    Returns:
        Transformed dictionary
    """
    # Extract input_data from variable arguments (GraalVM may pass extra args)
    if len(args) > 0:
        input_data = args[0]
    elif 'input_data' in kwargs:
        input_data = kwargs['input_data']
    else:
        raise ValueError(f"No input provided. Args count: {len(args)}, Kwargs: {list(kwargs.keys())}")
    
    # Helper function to map line items
    def map_line_items(items):
        result = []
        for item in items:
            result.append({
                "itemId": item.get("id"),
                "sku": item.get("sku"),
                "name": item.get("title"),  # Using title instead of name
                "vendor": item.get("vendor"),
                "quantity": item.get("quantity"),
                "unitPrice": item.get("price"),
                "weight": item.get("grams"),
                "productId": item.get("product_id"),
                "variantTitle": item.get("variant_title")
            })
        return result
    
    # Helper function to map note attributes
    def map_note_attributes(attrs):
        result = []
        for attr in attrs:
            result.append({
                "name": attr.get("name"),
                "value": attr.get("value")
            })
        return result
    
    # Get customer and shipping address safely
    customer = input_data.get("customer", {})
    default_address = customer.get("default_address", {})
    shipping_address = input_data.get("shipping_address", {})
    
    # Build the transformed result
    return {
        # Order information
        "orderId": input_data.get("id"),
        "orderNumber": input_data.get("order_number"),
        "orderName": input_data.get("name"),
        
        # Customer fields
        "customerId": customer.get("id"),
        "customerFirstName": customer.get("first_name"),
        "customerLastName": customer.get("last_name"),
        "customerEmail": customer.get("email"),
        "customerPhone": customer.get("phone"),
        "customerOrders": customer.get("orders_count"),
        "customerLifetimeValue": customer.get("total_spent"),
        
        # Customer address
        "customerStreet": default_address.get("address1"),
        "customerCity": default_address.get("city"),
        "customerState": default_address.get("province_code"),
        "customerZip": default_address.get("zip"),
        "customerCountry": default_address.get("country_code"),
        
        # Shipping address
        "shippingStreet": shipping_address.get("address1"),
        "shippingCity": shipping_address.get("city"),
        "shippingState": shipping_address.get("province_code"),
        "shippingZip": shipping_address.get("zip"),
        "shippingCountry": shipping_address.get("country_code"),
        
        # Line items
        "items": map_line_items(input_data.get("line_items", [])),
        
        # Financial fields
        "subtotal": input_data.get("subtotal_price"),
        "shippingCost": input_data.get("total_shipping_price_set", {}).get("shop_money", {}).get("amount"),
        "tax": input_data.get("total_tax"),
        "discounts": input_data.get("total_discounts"),
        "total": input_data.get("total_price"),
        "currency": input_data.get("currency"),
        
        # Status fields
        "paymentStatus": input_data.get("financial_status"),
        "fulfillmentStatus": input_data.get("fulfillment_status"),
        "paymentMethod": input_data.get("payment_gateway_names", [None])[0],
        
        # Other fields
        "source": input_data.get("source_name"),
        "browserIp": input_data.get("browser_ip"),
        "tagsRaw": input_data.get("tags"),
        "notes": input_data.get("note"),
        
        # Note attributes
        "noteAttributes": map_note_attributes(input_data.get("note_attributes", [])),
        
        "processedAt": input_data.get("processed_at"),
        "isConfirmed": input_data.get("confirmed"),
        "isTest": input_data.get("test"),
        
        # Default values (matching JOLT/ISL/MVEL defaults)
        "itemCount": 0,
        "totalQuantity": 0,
        "totalWeight": 0,
        "totalWeightKg": 0,
        "premiumItemCount": 0,
        "vendorCount": 0,
        "finalTotal": 0,
        "shippingStatus": "PENDING",
        "shippingSpeed": "STANDARD",
        "isPaid": False,
        "isFulfilled": False
    }


