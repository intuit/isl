---
title: Pagination
parent: Advanced Topics
nav_order: 1
description: "ISL pagination strategies for processing data page-by-page. Supports page-based and cursor-based pagination for API calls."
excerpt: "ISL pagination strategies for processing data page-by-page. Supports page-based and cursor-based pagination for API calls."
---

ISL pagination can be executed on demand, allowing the developer to process data in a page by page basis
instead of processing a complete batch of all records from api response.

This allows processing of smaller batches of data, allows easier handling of custom pagination algorithms and 
allows the ISL to deliver entities to the runtime for further processing in individual pages.

The ISL can also work with the host to allow _watermarking_ - understand how much data was already processed and be able to re-start
from a specific point in time in case of an error during processing of a page of data.


## Page by Page Pagination
```isl
fun run(){
    // $Page will be the iterator
    @.Pagination.Page( $Page, { startIndex: 0, pageSize: 100  } ){
        $page = @.Api.Call( { 
            Url: `/api/orders`,
            Query: {
                page: $Page.current,
                size: $Page.pageSize
            }
        } );

        // Transform all Records
        $orders: foreach $o in $page.body.items	// assume list of orders is in the body.items
            # convert each payload into an order
            @.This.Transform( $o )
        endfor

        // TODO: forward for processing - pass the $Page if you want to store the page detail once processed and be able to continue from that point in time

        // set the $page.hasMorePages to true if the array was not empty and you want to continue processing
        $page.hasMorePages = $page.body.items | isNotEmpty;	// use isNotEmpty to check an array is empty or not
    }
}
```

## Date Range based Pagination
Pagination(s) can also be embedded into each other, for example eBay requires a Pagination.Date pagination that embeds a Pagination.Page 
to paginate inside the date range.

```isl
fun run(){
    // $Date - date period pagination cursor: 1d -> 1 day
    @.Pagination.Date( $date, { startDate: $startDate, duration: 'P1D' }){
        
        // re-paginate based on pages & Date Rate
        @.Pagination.Page( $Page, { startIndex: 0, pageSize: 100  } ){
            
            $page = @.Api.Call( { 
                Url: `/api/orders`,
                Query: {
                    // date range
                    fromDate: $Date.StartDate,
                    toDate: $Date.EndDate,
                    
                    // page range inside the date
                    page: $Page.current,
                    size: $Page.pageSize
                }
            } );

            // ... 
        }
    }
}
```


## Pagination Strategies
There are various pagination strategies that can be triggered: Page, List, Cursor, Date.

### Page 
Paginate with a normal page index/size/offset. The pagination will stop once the `hasMorePages` is not set to `true`.

`@.Pagination.Page( $Page, { startIndex: 0, pageSize: 100 } )`

The `$Page` iterator will provide a set of properties:

- `$Page.page` - current index
- `$Page.startIndex` - the original starting index
- `$Page.pageSize`
- `$Page.fromOffset` - calculated from offset from Index & Size
- `$Page.toOffset` - calculated to offset from Index & Size
- `$Page.hasMorePages` - flag that needs to be set to `true` to force the pagination loop to continue.
  When processing arrays you can simply set `$Page.hasMorePages = $array | isNotEmpty;` to continue the pagination.

```isl
@.Pagination.Page( $page, { startIndex: 1, pageSize: 3 } ){
	// add the page index to the array
	$result = $result | push( $page.page );

	// finish after 5 pages
	$page.hasMorePages = if( $page.page < 5 ) true else false;
}
result: $result;
```
Result:
```json
{ "result": [1, 2, 3, 4, 5] }
```


### Cursor
Paginate using a custom cursor. The developer is responsible for capturing the `next` cursor from wherever they want (headers, of field inside the body).
This gives the developer the power to chose how they approach and select the cursor details from.

Pagination will exit once the `$Cursor.Next` remains null at the end of the loop.

`@.Pagination.Cursor( $Cursor )`

The `$Cursor` iterator will provide a set of properties:

- `$Cursor.current` - current cursor token/detail.
- `$Cursor.next` - set the next token/detail in this property to continue the pagination.


### Date
Date pagination provides pagination for date ranges from a start to and optional end date.

`@.Pagination.Date( $Date, { startDate: , endDate:, duration: "P1D" } )`

[Durations respects the IOS 8601 format](https://en.wikipedia.org/wiki/ISO_8601#Durations).

Only durations up to `nnD` (days) are supported.
Durations can be negative too. For example, use `-P1D` or `-P2D` to go back 1 or 2 days respectively


The `$Date` iterator will provide a set of properties:
- `$Date.startDate` - first Date in the current date range.
- `$Date.endDate` - end Date of the current range.
- `$Date.page` - the zero based index of the current page

```isl
// increment every day
@.Pagination.Date( $date, { startDate: "2021-12-03" | date.parse, endDate: "2021-12-06" | date.parse, duration: "P1D" } ){
	$start = $start | push ( $date.startDate | to.string("dd-MM-yy") );
	$end   = $end   | push ( $date.endDate | to.string("dd-MM-yy") );
}
result: {
	start: $start,
	end: $end
}
```
Result:
```json
{
	"result": {
		"start": ["03-12-21", "04-12-21", "05-12-21"],
		"end": ["04-12-21", "05-12-21", "06-12-21"]
	}
}
```
