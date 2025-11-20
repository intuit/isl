---
title: Date Examples
parent: Examples
nav_order: 1
---

The [standard Java format specifiers](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html) can be used in all date operations.

## Parsing

- `2019-03-05T09:56:55.728933+00:00` >

  `$value | date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSSSSxxx")`

  - `s` - second-of-minute
  - `S` - fraction-of-second
  - `xxx` - zone-offset

- `4/10/2021 03:12:40 AM` > `$value | date.parse("M/d/yyyy hh:mm:ss a")` > `2021-04-10T03:12:40.000Z`

- `10/4/2021 3:12:40 am` > `$value | date.parse("d/M/yyyy h:mm:ss a", { locale: "en_AU"})` > `2021-04-10T03:12:40.000Z`

- `20211203T201201` > `$value | date.parse("yyyyMMdd'T'HHmmss")`

  - `'T'` - `T` as verbatim text

- Parsing with optional parts `[...]`

  - `"2019-03-05" | date.parse("yyyy-MM-dd['T'HH:mm:ss[.SSS]['Z']]")` > `2019-03-05T00:00:00.000Z`
  - `"2019-03-05T23:12:15" | date.parse("yyyy-MM-dd['T'HH:mm:ss[.SSS]['Z']]")` > `2019-03-05T23:12:15.000Z`
  - `"2019-03-05T23:12:15.225" | date.parse("yyyy-MM-dd['T'HH:mm:ss[.SSS]['Z']]")` > `2019-03-05T23:12:15.225Z`
  - `"2019-03-05T23:12:15.225Z" | date.parse("yyyy-MM-dd['T'HH:mm:ss[.SSS]['Z']]")` > `2019-03-05T23:12:15.225Z`

- Parsing Zone Offsets - based on [Java's `DateTimeFormatter`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/format/DateTimeFormatter.html)

  - `X` (capital `X`) - parse normal offsets up to 4 chars long `Z; -08; -0830;`. You can make that optional using `[X]`
  - `XXX` - parse offsets with `:` in them `-08:30; -08:30:15`
  - `XXXX` - parse long offsets `-083015`
  - `XXXXX` - parse long offsets with `:` in them `-08:30:15`

## Rendering

- `@.Date.Now() | to.string("'Year' yyyy 'Month' MM 'Day' dd")` >

  `Year 2022 Month 09 Day 26`
