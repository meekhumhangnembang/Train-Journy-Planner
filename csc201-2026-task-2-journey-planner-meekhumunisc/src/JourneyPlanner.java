import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * CSC201 Task 2 -- Task 3: Earliest Arrival Journey Planning.
 *
 * Task 3.2 must be implemented twice, by two genuinely different algorithms,
 * as getItineraryM1 and getItineraryM2. Both print in exactly the same
 * templates. Each announces itself first with its own "[getItineraryM1]" or
 * "[getItineraryM2]" line, so the two can be told apart in a transcript.
 * getItinerary calls both in turn, so a single run exercises each of them;
 * the test cases call it.
 *
 * Times are seconds after midnight of the service day and are NOT wrapped at
 * 24:00 -- an arrival of 88410 prints as 24:33:30. Write yourself a
 * hhmmss(t) helper alongside label(stationId).
 */
public class JourneyPlanner {

    static final String CONNECTIONS = "dataset/connections.csv";
    static final String STATIONS = "dataset/stations.csv";
    static final String QUERIES = "tests/queries.csv";
    static final int TRANSFER_SECONDS = 180;
    static final int INF = Integer.MAX_VALUE / 4;

    private final Map<String, Integer> stationIndex = new HashMap<String, Integer>();
    private final String[] stationIds;
    private final String[] stationNames;

    private final int[] from;
    private final int[] to;
    private final int[] dep;
    private final int[] arr;
    private final int[] serviceIdx;
    private final int[] serviceId;
    private final String[] route;

    private final int serviceCount;
    private final List<Integer>[] adj;

    private final int[][] svcStops;
    private final int[][] svcDep;
    private final int[][] svcArr;
    private final int[] svcId;
    private final String[] svcRoute;
    private final List<Boarding>[] boardingsAt;

    @SuppressWarnings("unchecked")
    public JourneyPlanner() {
        List<String> ids = new ArrayList<String>();
        List<String> names = new ArrayList<String>();
        loadStations(ids, names);
        stationIds = ids.toArray(new String[0]);
        stationNames = names.toArray(new String[0]);
        int n = stationIds.length;

        List<RawConn> raw = loadConnections();
        int m = raw.size();
        from = new int[m];
        to = new int[m];
        dep = new int[m];
        arr = new int[m];
        serviceIdx = new int[m];
        serviceId = new int[m];
        route = new String[m];

        adj = new List[n];
        boardingsAt = new List[n];
        for (int i = 0; i < n; i++) {
            adj[i] = new ArrayList<Integer>();
            boardingsAt[i] = new ArrayList<Boarding>();
        }

        Map<Integer, Integer> serviceMap = new HashMap<Integer, Integer>();
        Map<Integer, List<RawConn>> byService = new HashMap<Integer, List<RawConn>>();
        boolean[][] edge = new boolean[n][n];
        for (int i = 0; i < m; i++) {
            RawConn c = raw.get(i);
            from[i] = c.from;
            to[i] = c.to;
            dep[i] = c.dep;
            arr[i] = c.arr;
            serviceId[i] = c.service;
            route[i] = c.route;
            Integer mapped = serviceMap.get(Integer.valueOf(c.service));
            if (mapped == null) {
                mapped = Integer.valueOf(serviceMap.size());
                serviceMap.put(Integer.valueOf(c.service), mapped);
            }
            serviceIdx[i] = mapped.intValue();
            List<RawConn> list = byService.get(Integer.valueOf(c.service));
            if (list == null) {
                list = new ArrayList<RawConn>();
                byService.put(Integer.valueOf(c.service), list);
            }
            list.add(c);
            if (!edge[c.from][c.to]) {
                edge[c.from][c.to] = true;
                adj[c.from].add(Integer.valueOf(c.to));
            }
        }

        serviceCount = serviceMap.size();
        svcStops = new int[serviceCount][];
        svcDep = new int[serviceCount][];
        svcArr = new int[serviceCount][];
        svcId = new int[serviceCount];
        svcRoute = new String[serviceCount];

        for (Map.Entry<Integer, List<RawConn>> e : byService.entrySet()) {
            int sid = e.getKey().intValue();
            int idx = serviceMap.get(Integer.valueOf(sid)).intValue();
            List<RawConn> legs = e.getValue();
            Collections.sort(legs, new Comparator<RawConn>() {
                public int compare(RawConn a, RawConn b) {
                    return a.dep - b.dep;
                }
            });
            int k = legs.size();
            int[] stops = new int[k + 1];
            int[] departures = new int[k + 1];
            int[] arrivals = new int[k + 1];
            stops[0] = legs.get(0).from;
            departures[0] = legs.get(0).dep;
            arrivals[0] = legs.get(0).dep;
            for (int i = 0; i < k; i++) {
                RawConn c = legs.get(i);
                stops[i + 1] = c.to;
                arrivals[i + 1] = c.arr;
                departures[i + 1] = (i + 1 < k) ? legs.get(i + 1).dep : c.arr;
            }
            svcStops[idx] = stops;
            svcDep[idx] = departures;
            svcArr[idx] = arrivals;
            svcId[idx] = sid;
            svcRoute[idx] = legs.get(0).route;
            for (int i = 0; i < k; i++) {
                boardingsAt[stops[i]].add(new Boarding(idx, i, departures[i]));
            }
        }
        for (int s = 0; s < n; s++) {
            Collections.sort(boardingsAt[s], new Comparator<Boarding>() {
                public int compare(Boarding x, Boarding y) {
                    return x.departure - y.departure;
                }
            });
        }
    }

    /** Task 3.1 -- stations exactly `hops` edges away, times ignored. */
    public void getStationsAtHops(String stationId, int hops) {
        Integer s = stationIndex.get(stationId);
        if (s == null) {
            System.out.printf("Unknown station '%s'.\n", stationId);
            return;
        }
        System.out.printf("Total number of stations exactly %d hop(s) from %s: %d\n",
                hops, label(stationId), countAtHops(s.intValue(), hops));
    }

    /** Task 3.2, algorithm 1 of 2: Connection Scan. */
    public void getItineraryM1(String fromStationId, String toStationId, int departAfter) {
        System.out.println("[getItineraryM1]"); // DO NOT remove this statement
        printItinerary(fromStationId, toStationId, departAfter,
                searchCsa(fromStationId, toStationId, departAfter));
    }

    /** Task 3.2, algorithm 2 of 2: trip-based Dijkstra. */
    public void getItineraryM2(String fromStationId, String toStationId, int departAfter) {
        System.out.println("[getItineraryM2]"); // DO NOT remove this statement
        printItinerary(fromStationId, toStationId, departAfter,
                searchDijkstra(fromStationId, toStationId, departAfter));
    }

    /**
     * The method the test cases call. It runs both versions one after the
     * other, so a single run exercises both: M1 prints its itinerary first,
     * then M2 prints its own. The two must report the same arrival time.
     */
    public void getItinerary(String fromStationId, String toStationId, int departAfter) {
        getItineraryM1(fromStationId, toStationId, departAfter);
        getItineraryM2(fromStationId, toStationId, departAfter);
    }

    /**
     * Runs the commands in the command file that belong to this task:
     *     java -cp bin JourneyPlanner             (reads tests/commands.txt)
     *     java -cp bin JourneyPlanner other.txt
     */
    public static void main(String[] args) {
        CommandRunner.run(CommandRunner.fileFrom(args), "JourneyPlanner");
    }

    /**
     * Runs both itinerary algorithms over tests/queries.csv: checks that
     * earliest arrivals agree, and records elapsed time for the report.
     *     java -cp bin BatchCompare
     */
    void runBatchComparison() {
        List<String[]> queries = loadQueries();
        int agree = 0;
        int disagree = 0;
        int reachable = 0;
        int differentJourneys = 0;

        long t1 = System.nanoTime();
        Result[] m1 = new Result[queries.size()];
        for (int i = 0; i < queries.size(); i++) {
            String[] q = queries.get(i);
            m1[i] = searchCsa(q[0], q[1], Integer.parseInt(q[2]));
        }
        long m1Ms = (System.nanoTime() - t1) / 1000000L;

        long t2 = System.nanoTime();
        Result[] m2 = new Result[queries.size()];
        for (int i = 0; i < queries.size(); i++) {
            String[] q = queries.get(i);
            m2[i] = searchDijkstra(q[0], q[1], Integer.parseInt(q[2]));
        }
        long m2Ms = (System.nanoTime() - t2) / 1000000L;

        for (int i = 0; i < queries.size(); i++) {
            int a1 = m1[i] == null ? -1 : m1[i].arrival;
            int a2 = m2[i] == null ? -1 : m2[i].arrival;
            if (a1 == a2) {
                agree++;
                if (a1 >= 0) {
                    reachable++;
                    if (!sameJourney(m1[i], m2[i])) differentJourneys++;
                }
            } else {
                disagree++;
                String[] q = queries.get(i);
                System.out.println("Disagree " + q[0] + " -> " + q[1] + " @" + q[2]
                        + " M1=" + a1 + " M2=" + a2);
            }
        }

        System.out.println("Queries: " + queries.size());
        System.out.println("Arrival times agree: " + agree);
        System.out.println("Arrival times disagree: " + disagree);
        System.out.println("Reachable queries: " + reachable);
        System.out.println("Same arrival, different journey: " + differentJourneys);
        System.out.println("M1 Connection Scan batch: " + m1Ms + " ms");
        System.out.println("M2 trip Dijkstra batch: " + m2Ms + " ms");
    }

    private static boolean sameJourney(Result a, Result b) {
        if (a.legs.size() != b.legs.size()) return false;
        for (int i = 0; i < a.legs.size(); i++) {
            Leg x = a.legs.get(i);
            Leg y = b.legs.get(i);
            if (x.service != y.service || x.depart != y.depart || x.arrive != y.arrive
                    || !x.from.equals(y.from) || !x.to.equals(y.to)) {
                return false;
            }
        }
        return true;
    }

    private List<String[]> loadQueries() {
        List<String[]> queries = new ArrayList<String[]>();
        try {
            BufferedReader in = new BufferedReader(new FileReader(QUERIES));
            try {
                String line = in.readLine();
                while ((line = in.readLine()) != null) {
                    if (line.length() == 0) continue;
                    String[] p = line.split(",");
                    queries.add(p);
                }
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot read " + QUERIES, e);
        }
        return queries;
    }

    private void printItinerary(String fromStationId, String toStationId, int departAfter, Result result) {
        if (!stationIndex.containsKey(fromStationId)) {
            System.out.printf("Unknown station '%s'.\n", fromStationId);
            return;
        }
        if (!stationIndex.containsKey(toStationId)) {
            System.out.printf("Unknown station '%s'.\n", toStationId);
            return;
        }
        if (fromStationId.equals(toStationId)) {
            System.out.printf("Already at %s at %s; no journey required.\n",
                    label(fromStationId), hhmmss(departAfter));
            return;
        }
        if (result == null || result.legs.isEmpty()) {
            System.out.printf("No journey from %s to %s departing at or after %s.\n",
                    label(fromStationId), label(toStationId), hhmmss(departAfter));
            return;
        }
        List<Leg> legs = result.legs;
        System.out.printf("Itinerary from %s to %s departing %s: %d leg(s), %d transfer(s), arriving %s.\n",
                label(fromStationId), label(toStationId), hhmmss(departAfter),
                Integer.valueOf(legs.size()), Integer.valueOf(legs.size() - 1), hhmmss(result.arrival));
        for (int i = 0; i < legs.size(); i++) {
            Leg leg = legs.get(i);
            System.out.printf("  Leg %d: route %s service %d depart %s %s arrive %s %s\n",
                    Integer.valueOf(i + 1), leg.route, Integer.valueOf(leg.service),
                    label(leg.from), hhmmss(leg.depart),
                    label(leg.to), hhmmss(leg.arrive));
        }
    }

    private Result searchCsa(String fromStationId, String toStationId, int departAfter) {
        Integer originObj = stationIndex.get(fromStationId);
        Integer destObj = stationIndex.get(toStationId);
        if (originObj == null || destObj == null || fromStationId.equals(toStationId)) {
            return null;
        }
        int origin = originObj.intValue();
        int dest = destObj.intValue();
        int n = stationIds.length;
        int m = from.length;

        int[] best = new int[n];
        Arrays.fill(best, INF);
        best[origin] = departAfter;
        boolean[] arrived = new boolean[n];
        boolean[] onTrip = new boolean[serviceCount];
        int[] tripBoard = new int[serviceCount];
        int[] tripBoardDep = new int[serviceCount];
        ParentLeg[] parent = new ParentLeg[n];

        for (int i = 0; i < m; i++) {
            int u = from[i];
            int svc = serviceIdx[i];
            boolean staying = onTrip[svc];
            int ready = arrived[u] ? best[u] + TRANSFER_SECONDS : best[u];
            boolean boarding = best[u] < INF && ready <= dep[i];
            if (!staying && !boarding) continue;
            if (!staying) {
                onTrip[svc] = true;
                tripBoard[svc] = u;
                tripBoardDep[svc] = dep[i];
            }
            int v = to[i];
            if (arr[i] < best[v]) {
                best[v] = arr[i];
                arrived[v] = true;
                parent[v] = new ParentLeg(tripBoard[svc], tripBoardDep[svc], arr[i],
                        route[i], serviceId[i]);
            }
        }
        if (best[dest] >= INF) return null;
        List<Leg> legs = walkLegs(origin, dest, parent);
        if (legs.isEmpty()) return null;
        return new Result(best[dest], legs);
    }

    private Result searchDijkstra(String fromStationId, String toStationId, int departAfter) {
        Integer originObj = stationIndex.get(fromStationId);
        Integer destObj = stationIndex.get(toStationId);
        if (originObj == null || destObj == null || fromStationId.equals(toStationId)) {
            return null;
        }
        int origin = originObj.intValue();
        int dest = destObj.intValue();
        int n = stationIds.length;

        int[] best = new int[n];
        Arrays.fill(best, INF);
        best[origin] = departAfter;
        boolean[] arrived = new boolean[n];
        ParentLeg[] parent = new ParentLeg[n];

        PriorityQueue<int[]> pq = new PriorityQueue<int[]>(new Comparator<int[]>() {
            public int compare(int[] a, int[] b) {
                if (a[0] != b[0]) return a[0] - b[0];
                return a[1] - b[1];
            }
        });
        pq.add(new int[] { departAfter, origin });

        while (!pq.isEmpty()) {
            int[] cur = pq.poll();
            int time = cur[0];
            int s = cur[1];
            if (time != best[s]) continue;
            if (s == dest) break;

            int ready = arrived[s] ? best[s] + TRANSFER_SECONDS : best[s];
            List<Boarding> options = boardingsAt[s];
            int start = firstBoarding(options, ready);
            for (int i = start; i < options.size(); i++) {
                Boarding b = options.get(i);
                int[] stops = svcStops[b.service];
                int[] arrivals = svcArr[b.service];
                for (int j = b.index + 1; j < stops.length; j++) {
                    int v = stops[j];
                    int at = arrivals[j];
                    if (at < best[v]) {
                        best[v] = at;
                        arrived[v] = true;
                        parent[v] = new ParentLeg(s, b.departure, at, svcRoute[b.service], svcId[b.service]);
                        pq.add(new int[] { at, v });
                    }
                }
            }
        }
        if (best[dest] >= INF) return null;
        List<Leg> legs = walkLegs(origin, dest, parent);
        if (legs.isEmpty()) return null;
        return new Result(best[dest], legs);
    }

    private List<Leg> walkLegs(int origin, int dest, ParentLeg[] parent) {
        List<ParentLeg> rev = new ArrayList<ParentLeg>();
        int s = dest;
        int guard = 0;
        while (s != origin) {
            ParentLeg p = parent[s];
            if (p == null || guard++ > stationIds.length) return new ArrayList<Leg>();
            rev.add(p);
            s = p.from;
        }
        Collections.reverse(rev);
        List<Leg> legs = new ArrayList<Leg>();
        for (int i = 0; i < rev.size(); i++) {
            ParentLeg p = rev.get(i);
            legs.add(new Leg(stationIds[p.from], stationIds[destFor(i, dest, rev)],
                    p.depart, p.arrive, p.route, p.service));
        }
        return legs;
    }

    private static int destFor(int i, int dest, List<ParentLeg> legs) {
        if (i + 1 < legs.size()) return legs.get(i + 1).from;
        return dest;
    }

    private int countAtHops(int start, int hops) {
        if (hops < 0) return 0;
        int n = stationIds.length;
        int[] dist = new int[n];
        Arrays.fill(dist, -1);
        ArrayDeque<Integer> q = new ArrayDeque<Integer>();
        dist[start] = 0;
        q.add(Integer.valueOf(start));
        int count = (hops == 0) ? 1 : 0;
        while (!q.isEmpty()) {
            int u = q.poll().intValue();
            if (dist[u] >= hops) continue;
            List<Integer> nbrs = adj[u];
            for (int i = 0; i < nbrs.size(); i++) {
                int v = nbrs.get(i).intValue();
                if (dist[v] == -1) {
                    dist[v] = dist[u] + 1;
                    if (dist[v] == hops) count++;
                    q.add(Integer.valueOf(v));
                }
            }
        }
        return count;
    }

    private static int firstBoarding(List<Boarding> options, int ready) {
        int lo = 0, hi = options.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (options.get(mid).departure < ready) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    private String label(String stationId) {
        int i = stationIndex.get(stationId).intValue();
        return stationNames[i] + " (" + stationId + ")";
    }

    private static String hhmmss(int t) {
        int h = t / 3600;
        int m = (t % 3600) / 60;
        int s = t % 60;
        return String.format("%02d:%02d:%02d", Integer.valueOf(h), Integer.valueOf(m), Integer.valueOf(s));
    }

    private void loadStations(List<String> ids, List<String> names) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(STATIONS));
            try {
                String line = in.readLine();
                while ((line = in.readLine()) != null) {
                    if (line.length() == 0) continue;
                    int comma = line.indexOf(',');
                    int second = line.indexOf(',', comma + 1);
                    String id = line.substring(0, comma);
                    String name = second < 0 ? line.substring(comma + 1) : line.substring(comma + 1, second);
                    stationIndex.put(id, Integer.valueOf(ids.size()));
                    ids.add(id);
                    names.add(name);
                }
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot read " + STATIONS, e);
        }
    }

    private List<RawConn> loadConnections() {
        List<RawConn> raw = new ArrayList<RawConn>(230227);
        try {
            BufferedReader in = new BufferedReader(new FileReader(CONNECTIONS));
            try {
                String line = in.readLine();
                while ((line = in.readLine()) != null) {
                    if (line.length() == 0) continue;
                    int c1 = line.indexOf(',');
                    int c2 = line.indexOf(',', c1 + 1);
                    int c3 = line.indexOf(',', c2 + 1);
                    int c4 = line.indexOf(',', c3 + 1);
                    int c5 = line.indexOf(',', c4 + 1);
                    Integer fi = stationIndex.get(line.substring(0, c1));
                    Integer ti = stationIndex.get(line.substring(c1 + 1, c2));
                    if (fi == null || ti == null) continue;
                    raw.add(new RawConn(
                            fi.intValue(), ti.intValue(),
                            Integer.parseInt(line.substring(c2 + 1, c3)),
                            Integer.parseInt(line.substring(c3 + 1, c4)),
                            line.substring(c4 + 1, c5),
                            Integer.parseInt(line.substring(c5 + 1))));
                }
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot read " + CONNECTIONS, e);
        }
        Collections.sort(raw, new Comparator<RawConn>() {
            public int compare(RawConn a, RawConn b) {
                if (a.dep != b.dep) return a.dep - b.dep;
                if (a.arr != b.arr) return a.arr - b.arr;
                return a.service - b.service;
            }
        });
        return raw;
    }

    private static final class RawConn {
        final int from, to, dep, arr, service;
        final String route;
        RawConn(int from, int to, int dep, int arr, String route, int service) {
            this.from = from;
            this.to = to;
            this.dep = dep;
            this.arr = arr;
            this.route = route;
            this.service = service;
        }
    }

    private static final class Boarding {
        final int service;
        final int index;
        final int departure;
        Boarding(int service, int index, int departure) {
            this.service = service;
            this.index = index;
            this.departure = departure;
        }
    }

    private static final class ParentLeg {
        final int from, depart, arrive, service;
        final String route;
        ParentLeg(int from, int depart, int arrive, String route, int service) {
            this.from = from;
            this.depart = depart;
            this.arrive = arrive;
            this.route = route;
            this.service = service;
        }
    }

    private static final class Leg {
        final String from, to, route;
        final int depart, arrive, service;
        Leg(String from, String to, int depart, int arrive, String route, int service) {
            this.from = from;
            this.to = to;
            this.depart = depart;
            this.arrive = arrive;
            this.route = route;
            this.service = service;
        }
    }

    private static final class Result {
        final int arrival;
        final List<Leg> legs;
        Result(int arrival, List<Leg> legs) {
            this.arrival = arrival;
            this.legs = legs;
        }
    }
}
