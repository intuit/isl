# Python simple transformation - minimal overhead test
customer = input_data.get("customer")

transformation_result = {
    "orderId": input_data.get("id"),
    "orderName": input_data.get("name"),
    "amount": input_data.get("total"),
    "customerName": customer.get("firstName") if customer else None
}

