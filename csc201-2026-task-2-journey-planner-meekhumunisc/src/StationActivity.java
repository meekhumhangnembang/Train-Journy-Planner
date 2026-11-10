import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * CSC201 Task 2 -- Task 1: Station Activity.
 *
 * Implement both subtasks in this class. You may add auxiliary classes,
 * but they must be invoked from here.
 *
 * Reminders from the task sheet:
 *  - Stations are printed as name followed by id in brackets, e.g.
 *    "Times Sq-42 St (127)". Write yourself a label(stationId) helper.
 *  - Any method handed a station id that is not in stations.csv prints
 *      System.out.printf("Unknown station '%s'.\n", stationId);
 *    and does nothing else.
 *  - Ranked lists print at most the first ten items; the count in the
 *    template line is still the full tier size.
 */
public class StationActivity {

    static final String CONNECTIONS = "dataset/connections.csv";
    static final String STATIONS = "dataset/stations.csv";
    static final int MAX_HOUR = 28;

    private final Map<String, String> stationNames = new HashMap<>();
    private final Map<String, Integer> departureCounts = new HashMap<>();
    private final Map<String, int[]> hourBuckets = new HashMap<>();

    /** Distinct departure counts, busiest first (index 0 is rank 1). */
    private final List<Integer> ranksByCount = new ArrayList<>();
    /** Stations sharing a departure count, already sorted by station_id. */
    private final Map<Integer, List<String>> stationsByCount = new HashMap<>();

    public StationActivity() {
        loadStations();
        countDepartures();
        buildRanks();
    }

    /** Task 1.1 -- report the k-th busiest tier of stations by departures. */
    public void getStationRank(int k) {
        if (k < 1 || k > ranksByCount.size()) {
            System.out.printf("Rank %d does not exist.\n", k);
            return;
        }

        int count = ranksByCount.get(k - 1);
        List<String> ids = stationsByCount.get(count);
        List<String> stations = new ArrayList<>();
        int shown = Math.min(10, ids.size());
        for (int i = 0; i < shown; i++) {
            stations.add(label(ids.get(i)));
        }

        System.out.printf("Rank %d: %d station(s) with %d departure(s).\n",
                k, ids.size(), count);
        System.out.println("Station(s) include: " + stations.toString());
    }

    /** Task 1.2 -- report the busiest hour bucket at one station. */
    public void getBusiestHour(String stationId) {
        if (!stationNames.containsKey(stationId)) {
            System.out.printf("Unknown station '%s'.\n", stationId);
            return;
        }

        int[] buckets = hourBuckets.get(stationId);
        int hour = 0;
        int best = buckets[0];
        for (int h = 1; h <= MAX_HOUR; h++) {
            if (buckets[h] > best) {
                best = buckets[h];
                hour = h;
            }
        }

        System.out.printf("Busiest hour at %s is %02d:00-%02d:00 with %d departure(s).\n",
                label(stationId), hour, hour + 1, best);
    }

    /**
     * Runs the commands in the command file that belong to this task:
     *     java -cp bin StationActivity            (reads tests/commands.txt)
     *     java -cp bin StationActivity other.txt
     */
    public static void main(String[] args) {
        CommandRunner.run(CommandRunner.fileFrom(args), "StationActivity");
    }

    private String label(String stationId) {
        return stationNames.get(stationId) + " (" + stationId + ")";
    }

    private void loadStations() {
        try (BufferedReader in = new BufferedReader(new FileReader(STATIONS))) {
            String line = in.readLine(); // header
            while ((line = in.readLine()) != null) {
                if (line.isEmpty()) continue;
                int comma = line.indexOf(',');
                if (comma < 0) continue;
                String id = line.substring(0, comma);
                int second = line.indexOf(',', comma + 1);
                String name = second < 0 ? line.substring(comma + 1) : line.substring(comma + 1, second);
                stationNames.put(id, name);
                departureCounts.put(id, 0);
                hourBuckets.put(id, new int[MAX_HOUR + 1]);
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot read " + STATIONS, e);
        }
    }

    private void countDepartures() {
        try (BufferedReader in = new BufferedReader(new FileReader(CONNECTIONS))) {
            String line = in.readLine(); // header
            while ((line = in.readLine()) != null) {
                if (line.isEmpty()) continue;
                int first = line.indexOf(',');
                int second = line.indexOf(',', first + 1);
                int third = line.indexOf(',', second + 1);
                String from = line.substring(0, first);
                Integer dep = departureCounts.get(from);
                if (dep == null) continue; // unknown station id
                departureCounts.put(from, dep + 1);

                int departure = Integer.parseInt(line.substring(second + 1, third));
                int hour = departure / 3600;
                if (hour >= 0 && hour <= MAX_HOUR) {
                    hourBuckets.get(from)[hour]++;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot read " + CONNECTIONS, e);
        }
    }

    /**
     * Groups stations by departure count, then extracts the distinct counts
     * in descending order with a max-heap. That finds the k-th busiest tier
     * without sorting every station: only 444 counts are heaped, and a rank
     * query then looks up the already-grouped list for that count.
     */
    private void buildRanks() {
        for (Map.Entry<String, Integer> e : departureCounts.entrySet()) {
            stationsByCount.computeIfAbsent(e.getValue(), c -> new ArrayList<>()).add(e.getKey());
        }
        for (List<String> ids : stationsByCount.values()) {
            Collections.sort(ids);
        }

        PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
        maxHeap.addAll(stationsByCount.keySet());
        while (!maxHeap.isEmpty()) {
            ranksByCount.add(maxHeap.poll());
        }
    }
}
