---
title: Dates & Times
parent: Data Types
nav_order: 2
description: "ISL date and time processing utilities. Parse, format, and manipulate dates with ISO 8601 and Unix timestamps in UTC timezone."
excerpt: "ISL date and time processing utilities. Parse, format, and manipulate dates with ISO 8601 and Unix timestamps in UTC timezone."
---

**Note:** All dates in ISL assume UTC as the only timezone.

The following methods are supported for working with dates:

- `@.Date.Now()` - current UTC timestamp
- `| to.string( format )` - convert a date to a string with a specified format
- `| date.parse( format , { locale: 'en_AU' })` - parse a string to date using a specified format. Null is returned is the date can't be parsed. The options parameter is optional and the default locale is `en_US`.
- `| date.parse( [ format1, format2 ], [{ locale: 'en_AU' }] )` - try parse a string to date using any of the specified formats.
- `| date.add ( value, unit )` - add/subtract a `value` of `units` from a date
- `| date.part( part )` - return a part the date. part=`year|month|day|dayOfWeek|dayOfYear|hour|minute|second`
- `| date.fromEpochMillis` - convert an epoch milliseconds number to a a date
- `| date.fromEpochSeconds` - convert an epoch milliseconds number to a a date
- `| date.toTimezone( timezone )` - convert a date to a specific timezone. E.g. `@.Date.Now() | date.toTimezone('America/Los_Angeles')`.
- `| date.toUtc` - convert a date to UTC timezone.
- `| date.toLocal` - convert a date to the system's local timezone.
- `| to.number` - convert the date to epoch seconds
- `| to.epochmillis` - convert the date to epoch milliseconds

## Format Specifiers
The [standard Java format specifiers](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html) can be used in all date operations.
To include verbatim text wrap the text in `'` (apostrophes) e.g. `dd'T'HH`.

You can see [various examples of date & time parsing in the examples section](../examples/dates.md).

## @.Date.Now()
`@.Date.Now()` provides the current UTC system timestamp. By default any conversion to a string will be 
done using the default ISO format `yyyy-MM-ddTHH:mm:ss.fffZ` e.g. `2021-12-01T00:57:39.910Z`.

- `$date = @.Date.Now() | to.string` will output in the standard ISO format of `yyyy-MM-ddTHH:mm:ss.fffZ` > `2021-12-01T00:57:39.910Z`
- You can also specify a custom formatting: `$date = @.Date.Now() | to.string( "yyMMdd" )` will output `211201`
- You can include text literals in the date formatting using `'` (apostrophes): `$date = @.Date.Now() | to.string( "yyMMdd'T'HHmm" )` will output `211201T2201`

## Parsing Dates
- `$stringValue | date.parse( format )` - parse any string with the specified formats. [See specifiers](#format-specifiers).
  `"2021-12-03" | date.parse("yyyy-MM-dd")` > `2021-12-03`

- `$stringValue | date.parse( format )` - parse any string with the specified formats. [See specifiers](#format-specifiers).
  `"2021-12-03" | date.parse( ["yyyyMMdd", "MMddyyyy", "yyyy-MM-dd"] )` > `2021-12-03`

## Date Operations
Dates can be manipulated by adding (or subtracting) new values to them:
`$value = $date | date.add( value, unit )` - add a specific `value` of `units` to the specified date.

`$value = r: "2021-12-03" | date.parse("yyyy-MM-dd") | date.add( 1, 'DAYS' ) | to.string("yy-MM-dd")`
results in `21-12-04`

Available Units (case sensitive!)

- `MILLIS`
- `SECONDS`
- `MINUTES`
- `HOURS`
- `DAYS`
- `WEEKS`
- `MONTHS`
- `YEARS`

### Comparing Dates
The simplest way to compare dates is to compare their numeric equivalent
Date comparisons work in any `if` or `switch` statements.

By default the date comparisons are done on the seconds version of the date.
```isl
$d1 = "2021-12-03" | date.parse("yyyy-MM-dd");
$d2 = "2021-12-04" | date.parse("yyyy-MM-dd");
if ( $d1 < $d2 )
	// ...
endif
```

If you need to compare dates with precision of milliseconds you will need to convert the milliseconds first:
```isl
$d1 = "2021-12-03" | date.parse("yyyy-MM-dd");
$d2 = "2021-12-04" | date.parse("yyyy-MM-dd");
if ( $d1 | to.epochmillis < $d2 | to.epochmillis )
	// ...
endif
```

