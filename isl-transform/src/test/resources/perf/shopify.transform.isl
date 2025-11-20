fun run( $input ){
    return @.This.Transform( $input );
}

fun transform( $o ){

    $order: {
            id: {
                externalId: $o.id | to.string
            },
            number: $o.name | subStringAfter( "#" ),
            date: {
                timeStamp: $o.processed_at
            },
            status: $o | getOrderStatus,
            note: $o.note,
            taxTreatment: if ( $o.taxes_included == true) "inclusive" else "exclusive",
            customer: @.This.transformCustomer( $o.customer, $o.billing_address ),
            $lines: @.This.transformLines( $o.line_items, $o.currency, $o.discount_applications, $o.taxes_included, $o.refunds ),
            $shipLines: @.This.transformShippingLines( $o.shipping_lines, $o.currency, $o.discount_applications, $o.taxes_included ),
            lineItems: $lines.lines | pushItems( $shipLines ),
            total: $o.total_price_set | getMoney | subMoney( $lines.editedTotal ),
            totalTaxes: $o.total_tax_set | getMoney | subMoney( $lines.editedTax ),
            totalDiscounts: if( $o.financial_status contains "refunded" ) $o.total_discounts_set | getMoney else $o.current_total_discounts_set | getMoney,
            }

    return $order

}


fun transformCustomer( $remoteCustomer, $orderBilling ){
    $customer: {
        id: {
            externalId: $remoteCustomer.id | to.string
        },
        name: {
            businessName: if ($orderBilling.company) $orderBilling.company else $remoteCustomer.default_address.company,
            individualName: {
                firstName: $remoteCustomer.first_name,
                lastName: $remoteCustomer.last_name
            }
        },
        emails: [
            {
                email: $remoteCustomer.email
                // verifiedType ?
                // usage ?
            }
        ],
        phones: [
            {
                phoneNumber: if( $remoteCustomer.phone) $remoteCustomer.phone else $remoteCustomer.default_address.phone
            }
        ]
    }
    return $customer
}

fun transformLines( $remoteLines, $currency, $orderDiscounts, $taxIncluded, $refunds ) {

    // extract edited lines to filter inside foreach loop
    $editedLines: $refunds | getEdited

    foreach $li in $remoteLines

        $line: {
            lineType: if ( $li.product_id ) "product" else "service",
            externalLineItemId: $li.id | to.string,
            index: $liIndex,
            quantity: $li.quantity,
            amount: $li.price_set | getMoney( $li.quantity ),
            unitPrice: if ($taxIncluded == true) $li.price_set | getMoney,
            unitPriceBeforeTax: if ($taxIncluded == false) $li.price_set | getMoney,
            productId: if ( $li.product_id ) {
                externalId: $li.product_id | to.string
            },
            variantId: if ( $li.variant_id ){
                externalId: $li.variant_id | to.string
            },
            sku: $li.sku,
            name: if ($li.name) $li.name else $li.title,
            description: if ($li.name) $li.name else $li.title,
            //
            options: $li.properties | getOptions,
            //
            // Discounts
            discountTotal: if( $li.discount_allocations ){
                settlementMoney: {
                    currency: $li.discount_allocations[0].amount_set.shop_money.currency_code,
                    amount: $li.discount_allocations | reduce( {{ $acc + $it.amount_set.shop_money.amount }} ) | to.string
                },
                presentmentMoney: {
                    currency: $li.discount_allocations[0].amount_set.presentment_money.currency_code,
                    amount: $li.discount_allocations | reduce( {{ $acc + $it.amount_set.presentment_money.amount }} ) | to.string
                }
            },
            discounts: foreach $disc in $li.discount_allocations
            {
                $appliedDisc: $orderDiscounts | at($disc.discount_application_index),
                name: $appliedDisc.code,
                "type":   switch($appliedDisc.value_type)
                            "fixed_amount" -> "amount";
                            "percentage" -> "percentage";
                        endswitch,
                value: $appliedDisc.value,
                description: $appliedDisc.description,
                applicationType:    switch ($appliedDisc.type)
                                        "manual" -> "manual";
                                        "discount_code" -> "code";
                                        "script" -> "auto";
                                    endswitch,
            } endfor,
            //
            // Taxes
            taxes: foreach $tax in $li.tax_lines {
                code: $tax.title,
                name: $tax.title,
                value: {{ $tax.rate * 100}},
                "type": "percentage"
            } endfor,
            //
            taxTotal: if( $li.tax_lines ) {
                settlementMoney: {
                    currency: $li.tax_lines[0].price_set.shop_money.currency_code,
                    amount: $li.tax_lines | reduce( {{ $acc + $it.price_set.shop_money.amount }} ) | to.string
                },
                presentmentMoney: {
                    currency: $li.tax_lines[0].price_set.presentment_money.currency_code,
                    amount: $li.tax_lines | reduce( {{ $acc + $it.price_set.presentment_money.amount }} ) | to.string
                }
            },
            //
            // Fulfillment
            fulfillmentStatus:  switch ($li.fulfillment_status)
                                    "fulfilled" -> "complete";
                                    "partial" -> "partial";
                                    else -> null;
                                endswitch,

            $items: @.This.getItems( $li.product_id, $li.variant_id ),
            // product/variant available?
            productNotAvailable: if( !$items.product.id ) true else false,
            variantNotAvailable: if( !$items.variant.id ) true else false,

        }

        $realLine: $line | handleEdited( $editedLines, $li )

        $sumEditedTax: $sumEditedTax | addMoney( $realLine.editedTax )
        $sumEditedTotal: $sumEditedTotal | addMoney( $realLine.editedTotal )
        if( !$taxIncluded )
            $sumEditedTotal: $sumEditedTotal | addMoney( $realLine.editedTax )
        endif

        $lines: if( $realLine.line.quantity > 0 ) $lines | push( $realLine.line ) else $lines

    endfor

    return {
        lines: $lines,
        editedTax: $sumEditedTax,
        editedTotal: $sumEditedTotal
    }
}

// Shipping Lines
fun transformShippingLines( $shippingLines, $currency, $orderDiscounts, $taxIncluded ) {

    $lines: foreach $ship in $shippingLines {
        lineType: "shipping",
        sku: $ship.code,
        name: $ship.title,
        description: $ship.title,
        amount: $ship.price_set | getMoney,
        quantity: 1,
        unitPrice: if ($taxIncluded == true) $ship.price_set | getMoney,
        unitPriceBeforeTax: if ($taxIncluded == false) $ship.price_set | getMoney,
        //
        taxes: foreach $tax in $ship.tax_lines {
            code: $tax.title,
            name: $tax.title,
            amount: $tax.price_set | getMoney,
            value: {{ $tax.rate * 100}},
            "type": "percentage"
        } endfor,
        taxTotal: if( $ship.tax_lines ) {
                settlementMoney: {
                    currency: $ship.tax_lines[0].price_set.shop_money.currency_code,
                    amount: $ship.tax_lines | reduce( {{ $acc + $it.price_set.shop_money.amount }} ) | to.string
                },
                presentmentMoney: {
                    currency: $ship.tax_lines[0].price_set.presentment_money.currency_code,
                    amount: $ship.tax_lines | reduce( {{ $acc + $it.price_set.presentment_money.amount }} ) | to.string
                }
            }
    } endfor

    return $lines
}

fun getItems( $productId, $variantId ) {

    if( $productId )
        //$product: @.Product.findExisting( $productId )
        $product: { id : $productId }

        if ($product.id )
            // $variant: @.Variant.findExisting( $variantId, $product )
            $variant: { id: $variantId }
        endif
    endif

    return { product: $product, variant: $variant }
}
modifier getEdited( $refunds ) {

    $editedRefunds: $refunds | filter( !$fit.transactions and !$fit.order_adjustments )

    foreach $er in $editedRefunds
        $editedLines: $editedLines | pushItems( $er.refund_line_items )
    endfor

    return $editedLines
}

// Adjusts the line items and returns the new line and edited total and tax to subtract from order total
modifier handleEdited( $localLine, $editedLines, $remoteLine )
{
    $editedLineDetails: $editedLines | filter( $fit.line_item_id == $localLine.externalLineItemId )

    // if there are multiple edits, need to loop through multiple "refunds"
    foreach $ed in $editedLineDetails
        $localLine.quantity: {{ $localLine.quantity - $ed.quantity }}
        $localLine.taxTotal: $localLine.taxTotal | subMoney( $ed.total_tax_set )

        // keep sum of edited total/tax to remove from order level totals
        $editedTotal: $editedTotal | addMoney( $ed.subtotal_set )
        $editedTax: $editedTax | addMoney( $ed.total_tax_set )
    endfor

    $localLine.amount: $remoteLine.price_set | getMoney( $localLine.quantity )

    return {
        line: $localLine,
        editedTotal: $editedTotal,
        editedTax: $editedTax
    }
}

// subtract remote money object from CDM money object
modifier subMoney( $localMoney, $remoteMoney ) {

    $result: {
        settlementMoney: {
            currency: if( $localMoney.settlementMoney.currency ) $localMoney.settlementMoney.currency else $remoteMoney.shop_money.currency_code,
            amount: {{ $localMoney.settlementMoney.amount - $remoteMoney.shop_money.amount }} | to.string
        },
        presentmentMoney: {
            currency: if( $localMoney.presentmentMoney.currency ) $localMoney.presentmentMoney.currency else $remoteMoney.presentment_money.currency_code,
            amount: {{ $localMoney.presentmentMoney.amount - $remoteMoney.presentment_money.amount }} | to.string
        }
    }
    return $result;

}

modifier addMoney( $money, $moreMoney ) {

    if( !$money )
        $result: $moreMoney
    else
        $result:{
            shop_money: {
                currency_code: if( $money.shop_money.currency_code ) $money.shop_money.currency_code else $moreMoney.shop_money.currency_code,
                amount: {{ $money.shop_money.amount + $moreMoney.shop_money.amount }} | to.string
            },
            presentment_money: {
                currency_code: if( $money.presentment_money.currency_code ) $money.presentment_money.currency_code else $moreMoney.presentment_money.currency_code,
                amount: {{ $money.presentment_money.amount + $moreMoney.presentment_money.amount }} | to.string
            }
        }
    endif

    return $result;
}

// Convert remote money object to CDM money object
modifier getMoney( $amountSet, $quantity ) {

    $quantity: if (!$quantity) 1 else $quantity

    $money:{
        settlementMoney: {
            currency: $amountSet.shop_money.currency_code,
            amount: {{ $amountSet.shop_money.amount * $quantity }} | to.string
        },
        presentmentMoney: {
            currency: $amountSet.presentment_money.currency_code,
            amount: {{ $amountSet.presentment_money.amount * $quantity }} | to.string
        }
    }
    return $money;
}

modifier getOptions ( $properties ) {

    $options: []
    foreach $prop in $properties
        // properties that start with _ are hidden properties and can be any format. Ignore them for now.
        $option: if ( $prop.value.input_type != "HIDDEN" )
        {
            name: $prop.name,
            value: $prop.value
        }

        $options: if ( $option ) $options | push( $option ) else $options
    endfor


    return $options
}

modifier getPaymentStatus( $remoteStatus ) {

    return switch( $remoteStatus )
                "pending"            -> "unpaid";
                "authorized"         -> "unpaid";
                "partially_paid"     -> "partial";
                "paid"               -> "full";
                "partially_refunded" -> "full"; // We are making assumptions here that only FULLY PAID orders can be issued with a refund
                "refunded"           -> "full";
                "voided"             -> "other";
            endswitch
}

modifier getRefundStatus( $remoteStatus ) {

    return switch( $remoteStatus )
                "refunded" -> "refunded";
                "partially_refunded" -> "partially_refunded";
            endswitch
}
modifier getOrderStatus( $order ) {

    if( $order.cancelled_at )
        return "cancelled"
    endif

    //return "created"

    if( $order.closed_at)
        return "closed"
    endif

    return "open"

}

