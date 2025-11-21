# Python transformation - Shopify order to internal format
# This version uses inline execution rather than function calls
# The input data is expected to be in the global variable 'input_data'

# NOTE: When accessing Java Maps from Python, use single-arg .get() or [] accessor
# Java's Map.get() only takes 1 argument, unlike Python's dict.get(key, default)

# Extract nested objects once for cleaner access
customer = input_data.get("customer")
customer_address = customer.get("default_address") if customer else None
shipping_address = input_data.get("shipping_address")
total_shipping_price_set = input_data.get("total_shipping_price_set")
shop_money = total_shipping_price_set.get("shop_money") if total_shipping_price_set else None
payment_gateways = input_data.get("payment_gateway_names")

# Helper to map line items
line_items_result = []
line_items = input_data.get("line_items")
if line_items:
    for item in line_items:
        line_items_result.append({
            "itemId": item.get("id"),
            "sku": item.get("sku"),
            "name": item.get("title"),
            "vendor": item.get("vendor"),
            "quantity": item.get("quantity"),
            "unitPrice": item.get("price"),
            "weight": item.get("grams"),
            "productId": item.get("product_id"),
            "variantTitle": item.get("variant_title")
        })

# Helper to map note attributes
note_attrs_result = []
note_attrs = input_data.get("note_attributes")
if note_attrs:
    for attr in note_attrs:
        note_attrs_result.append({
            "name": attr.get("name"),
            "value": attr.get("value")
        })

# Main transformation result
transformation_result = {
    "orderId": input_data.get("id"),
    "orderNumber": input_data.get("order_number"),
    "orderName": input_data.get("name"),
    
    # Customer fields
    "customerId": customer.get("id") if customer else None,
    "customerFirstName": customer.get("first_name") if customer else None,
    "customerLastName": customer.get("last_name") if customer else None,
    "customerEmail": customer.get("email") if customer else None,
    "customerPhone": customer.get("phone") if customer else None,
    "customerOrders": customer.get("orders_count") if customer else None,
    "customerLifetimeValue": customer.get("total_spent") if customer else None,
    
    # Customer address
    "customerStreet": customer_address.get("address1") if customer_address else None,
    "customerCity": customer_address.get("city") if customer_address else None,
    "customerState": customer_address.get("province_code") if customer_address else None,
    "customerZip": customer_address.get("zip") if customer_address else None,
    "customerCountry": customer_address.get("country_code") if customer_address else None,
    
    # Shipping address
    "shippingStreet": shipping_address.get("address1") if shipping_address else None,
    "shippingCity": shipping_address.get("city") if shipping_address else None,
    "shippingState": shipping_address.get("province_code") if shipping_address else None,
    "shippingZip": shipping_address.get("zip") if shipping_address else None,
    "shippingCountry": shipping_address.get("country_code") if shipping_address else None,
    
    # Line items
    "items": line_items_result,
    
    # Financial fields
    "subtotal": input_data.get("subtotal_price"),
    "shippingCost": shop_money.get("amount") if shop_money else None,
    "tax": input_data.get("total_tax"),
    "discounts": input_data.get("total_discounts"),
    "total": input_data.get("total_price"),
    "currency": input_data.get("currency"),
    
    # Status fields
    "paymentStatus": input_data.get("financial_status"),
    "fulfillmentStatus": input_data.get("fulfillment_status"),
    "paymentMethod": payment_gateways[0] if payment_gateways and len(payment_gateways) > 0 else None,
    
    # Other fields
    "source": input_data.get("source_name"),
    "browserIp": input_data.get("browser_ip"),
    "tagsRaw": input_data.get("tags"),
    "notes": input_data.get("note"),
    
    # Note attributes
    "noteAttributes": note_attrs_result,
    
    "processedAt": input_data.get("processed_at"),
    "isConfirmed": input_data.get("confirmed"),
    "isTest": input_data.get("test"),
    
    # Default values (matching JOLT defaults)
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
