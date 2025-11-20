// Data transformation example
fun run($data) {
    // Filter and transform items
    activeItems: foreach $item in $data.items | filter($item.active) {
        id: $item.id,
        name: $item.name | upperCase,
        total: {{ $item.price * $item.quantity }},
        formattedPrice: "$" + $item.price
    }
    endfor,
    
    // Calculate totals
    totalItems: $data.items | length,
    activeCount: $data.items | filter($item.active) | length,
    grandTotal: $data.items | reduce({{ $acc + $it.price * $it.quantity }}),
    
    // Add metadata
    processedAt: @.Date.Now(),
    status: "success"
}


