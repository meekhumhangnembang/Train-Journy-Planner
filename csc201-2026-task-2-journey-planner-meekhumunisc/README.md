# CSC201 DSA Task 2 — Subway Journey Planner

Starter repository. Everything you need is already here; you should not need to download anything.



### Data — `dataset/`

A frozen snapshot of one weekday of the New York City Subway timetable. Do not download the feed yourself and do not edit these files; everyone must work from the identical snapshot or no two results will agree.

Paths are relative to the repository root, so run your program from the root and open `dataset/connections.csv` exactly as written in the starter classes.

### Your code — `src/`

| File | Contains |
|---|---|
| `src/StationActivity.java` | Task 1: `getStationRank`, `getBusiestHour` |
| `src/DirectServices.java` | Task 2: `getDirectServices` |
| `src/JourneyPlanner.java` | Task 3: `getStationsAtHops`, `getItineraryM1`, `getItineraryM2`, `getItinerary` |
| `src/Subway.java` | Master class |
| `src/CommandRunner.java` | Harness. Reads the command file and calls your methods. Do not edit |

Fill in the three task classes. You may add auxiliary classes for sorting, graph algorithms and utilities; invoke them from the task class they belong to. Leave `CommandRunner.java` alone: marking runs your code through it, so changing it will not help you and may stop your submission running at all.

Two helpers are worth writing early, because every task needs them:

* `label(stationId)` returns the station name followed by its id in brackets, as in `Times Sq-42 St (127)`.
* `hhmmss(t)` formats seconds after midnight as `HH:MM:SS`, letting the hour run past 23. An arrival of `88410` prints as `24:33:30`, not `00:33:30`.

### Commands — `tests/commands.txt`

The program does not hardcode which queries to run. It reads them from a command file: one call per line, method name then arguments, separated by spaces. Blank lines and lines starting with `#` are ignored.

```
getStationRank 20
getBusiestHour 127
getDirectServices 629 635
getStationsAtHops 127 3
getItinerary 420 405 28800
```

Station ids are written without quotes. The seven commands available are `getStationRank`, `getBusiestHour`, `getDirectServices`, `getStationsAtHops`, `getItinerary`, `getItineraryM1` and `getItineraryM2`.

The `tests/commands.txt` in this repository holds the sample queries from the task sheet. Add your own while you work. **Marking will replace this file with a different set of commands**, so do not rely on the ones shipped here, and do not change how the program reads it.

### Tests — `tests/`

| File | What it is |
|---|---|
| `tests/commands.txt` | The sample queries from the task sheet; the default command file |
| `tests/expected_samples.txt` | What a correct program prints for the commands in `tests/commands.txt` |
| `tests/queries.csv` | 1,000 `src,dst,depart_after` rows for the Task 3.2 comparison |

For Task 3.2, the earliest arrival is fixed by the timetable and there is only one right answer. However, more than one journey often reaches the destination at the earliest possible time, and the task sheet accepts any journey that does. So your `Leg` lines may differ from the expected ones.

## Submitting

Commit and push regularly; the history is evidence of progress as well as your final submission. Put the repository link in your report, which goes to Cadmus.
