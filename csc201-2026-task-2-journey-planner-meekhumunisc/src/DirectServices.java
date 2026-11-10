import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * CSC201 Task 2 -- Task 2: Direct Services.
 *
 * A direct service from u to v exists when one service calls at u and later
 * calls at v: the passenger rides between them without transferring. See the
 * task sheet for the full definition before you start -- a single service
 * stopping at 30 stations creates a direct service for every ordered pair
 * among those stations, not just the adjacent ones.
 */
public class DirectServices {

    static final String CONNECTIONS = "dataset/connections.csv";
    static final String STATIONS = "dataset/stations.csv";

    private final Map<String, String> stationNames = new HashMap<>();
    /** from -> to -> aggregated direct-service stats. */
    private final Map<String, Map<String, PairStats>> pairs = new HashMap<>();

    public DirectServices() {
        loadStations();
        enumerateDirectPairs();
    }

    /** Task 2 -- direct services between one ordered pair of stations. */
    public void getDirectServices(String fromStationId, String toStationId) {
        if (!stationNames.containsKey(fromStationId)) {
            System.out.printf("Unknown station '%s'.\n", fromStationId);
            return;
        }
        if (!stationNames.containsKey(toStationId)) {
            System.out.printf("Unknown station '%s'.\n", toStationId);
            return;
        }
        if (fromStationId.equals(toStationId)) {
            System.out.printf("No direct service from %s to %s.\n",
                    label(fromStationId), label(toStationId));
            return;
        }

        Map<String, PairStats> fromMap = pairs.get(fromStationId);
        PairStats stats = fromMap == null ? null : fromMap.get(toStationId);
        if (stats == null) {
            System.out.printf("No direct service from %s to %s.\n",
                    label(fromStationId), label(toStationId));
            return;
        }

        StringBuilder routeList = new StringBuilder();
        boolean first = true;
        for (String route : stats.routes) {
            if (!first) routeList.append('/');
            routeList.append(route);
            first = false;
        }

        System.out.printf("Direct service %s -> %s: %d service(s) on route(s) %s, min %ds, max %ds.\n",
                label(fromStationId), label(toStationId), stats.count,
                routeList.toString(), stats.minTime, stats.maxTime);
    }

    /**
     * Runs the commands in the command file that belong to this task:
     *     java -cp bin DirectServices             (reads tests/commands.txt)
     *     java -cp bin DirectServices other.txt
     */
    public static void main(String[] args) {
        CommandRunner.run(CommandRunner.fileFrom(args), "DirectServices");
    }

    private String label(String stationId) {
        return stationNames.get(stationId) + " (" + stationId + ")";
    }

    private void loadStations() {
        try (BufferedReader in = new BufferedReader(new FileReader(STATIONS))) {
            String line = in.readLine();
            while ((line = in.readLine()) != null) {
                if (line.isEmpty()) continue;
                int comma = line.indexOf(',');
                int second = line.indexOf(',', comma + 1);
                String id = line.substring(0, comma);
                String name = second < 0 ? line.substring(comma + 1) : line.substring(comma + 1, second);
                stationNames.put(id, name);
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot read " + STATIONS, e);
        }
    }

    /**
     * Rebuilds each service as an ordered list of stops, then records every
     * ordered pair (i, j) with i before j. Adjacent connections are only the
     * edges of that path; a passenger staying on the train can travel any
     * later stop as well.
     */
    private void enumerateDirectPairs() {
        Map<Integer, List<Connection>> byService = new HashMap<>();
        try (BufferedReader in = new BufferedReader(new FileReader(CONNECTIONS))) {
            String line = in.readLine();
            while ((line = in.readLine()) != null) {
                if (line.isEmpty()) continue;
                String[] p = split6(line);
                Connection c = new Connection(p[0], p[1], Integer.parseInt(p[2]),
                        Integer.parseInt(p[3]), p[4], Integer.parseInt(p[5]));
                byService.computeIfAbsent(c.service, k -> new ArrayList<>()).add(c);
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot read " + CONNECTIONS, e);
        }

        for (List<Connection> legs : byService.values()) {
            Collections.sort(legs, Comparator.comparingInt(a -> a.departure));
            List<Stop> stops = buildStops(legs);
            if (stops.size() < 2) continue;

            Set<String> seen = new HashSet<>();
            String route = legs.get(0).route;
            int n = stops.size();
            for (int i = 0; i < n - 1; i++) {
                Stop from = stops.get(i);
                for (int j = i + 1; j < n; j++) {
                    Stop to = stops.get(j);
                    if (from.station.equals(to.station)) continue;
                    String key = from.station + '\0' + to.station;
                    if (!seen.add(key)) continue;
                    int time = to.arrival - from.departure;
                    addPair(from.station, to.station, route, time);
                }
            }
        }
    }

    /** Walk the timed connections of one service into boarding/alighting stops. */
    private static List<Stop> buildStops(List<Connection> legs) {
        List<Stop> stops = new ArrayList<>(legs.size() + 1);
        Connection first = legs.get(0);
        stops.add(new Stop(first.from, first.departure, first.departure));
        for (int i = 0; i < legs.size(); i++) {
            Connection c = legs.get(i);
            int departHere = (i + 1 < legs.size()) ? legs.get(i + 1).departure : c.arrival;
            stops.add(new Stop(c.to, departHere, c.arrival));
        }
        return stops;
    }

    private void addPair(String from, String to, String route, int time) {
        Map<String, PairStats> inner = pairs.get(from);
        if (inner == null) {
            inner = new HashMap<>();
            pairs.put(from, inner);
        }
        PairStats stats = inner.get(to);
        if (stats == null) {
            stats = new PairStats();
            inner.put(to, stats);
        }
        stats.count++;
        stats.routes.add(route);
        if (time < stats.minTime) stats.minTime = time;
        if (time > stats.maxTime) stats.maxTime = time;
    }

    private static String[] split6(String line) {
        String[] p = new String[6];
        int start = 0;
        for (int i = 0; i < 5; i++) {
            int comma = line.indexOf(',', start);
            p[i] = line.substring(start, comma);
            start = comma + 1;
        }
        p[5] = line.substring(start);
        return p;
    }

    private static final class Connection {
        final String from, to, route;
        final int departure, arrival, service;

        Connection(String from, String to, int departure, int arrival, String route, int service) {
            this.from = from;
            this.to = to;
            this.departure = departure;
            this.arrival = arrival;
            this.route = route;
            this.service = service;
        }
    }

    private static final class Stop {
        final String station;
        final int departure;
        final int arrival;

        Stop(String station, int departure, int arrival) {
            this.station = station;
            this.departure = departure;
            this.arrival = arrival;
        }
    }

    private static final class PairStats {
        int count;
        int minTime = Integer.MAX_VALUE;
        int maxTime = Integer.MIN_VALUE;
        final TreeSet<String> routes = new TreeSet<>();
    }
}
