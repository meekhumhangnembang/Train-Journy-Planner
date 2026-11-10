# NYC Subway timetable - frozen dataset

Derived from the MTA **regular** static GTFS feed, `Weekday` service day. Source: <https://rrgtfsfeeds.s3.amazonaws.com/gtfs_subway.zip> (feed_version `20260826-X-long-term-supplement-trip-ids`, valid 2026-05-26 to 2026-10-31). MTA data feeds are free to use; MTA logos, maps and symbols are not. Do not reproduce the official map or coloured route bullets.

- connections.csv - 230,227 rows. 

**Times past midnight are not wrapped.** Late services continue past 86400 (24:00:00); the latest departure is 100800 (28:00:00). Subtracting 86400 would corrupt the ordering - treat time as a plain increasing integer.

**Staying on the same `service` is not a transfer.** Changing to a different `service` requires the minimum transfer time (MTA publishes 180 seconds).

- stations.csv - 444 rows

Station names are **not unique**. For example, six different stations are called `86 St`. Always key on `station_id`.

35 nodes have `platforms > 1`: these are interchanges whose lines GTFS records as separate stations. They are merged here because the raw feed otherwise splits the network into six disconnected pieces.
