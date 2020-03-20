package fr.matthieuaudemard.yacot.services;

import fr.matthieuaudemard.yacot.models.LocationStat;
import fr.matthieuaudemard.yacot.models.Report;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingInt;

/**
 * Spring service accessing to a Github repository which provides location stats for Coronavirus
 */
@Service
public class CoronaVirusDataService {

    private static final String VIRUS_DATA_URL = "https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/csse_covid_19_time_series/time_series_19-covid-Confirmed.csv";

    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{1,2}/\\d{1,2}/\\d{2}");

    private List<LocationStat> stats = new ArrayList<>();

    /**
     * Parse a Date String to a LocalDate
     * the date shall be formatted as "M/d/u" i.e: 3 Jun 2008 as 6/3/08
     *
     * @param key the date in string
     * @return the LocalDate parsed
     */
    private static LocalDate parseDate(String key) {
        // Split the string into 3 parts delimited by a '/'
        String[] chunks = key.split("/");
        // Add 20 to the year chunk to format it as 2020 for example
        chunks[chunks.length - 1] = "20" + chunks[chunks.length - 1];
        // Join the 3 parts by '/' delimiter
        key = String.join("/", Arrays.asList(chunks));
        return LocalDate.parse(key, DateTimeFormatter.ofPattern("M/d/y"));
    }

    /**
     * Get virus data from Github repository
     * This Method is executed just once a day due to scheduled CRON task
     *
     * @throws IOException if the request to Github repository fails
     */
    @PostConstruct
    @Scheduled(cron = "0 0 1 * * *")
    public void fetchVirusData() throws IOException {
        List<LocationStat> newStats = new ArrayList<>();
        HttpGet request = new HttpGet(CoronaVirusDataService.VIRUS_DATA_URL);

        // Send get request to retrieve csv formatted string
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(request)) {
            client.execute(request);

            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                String csvString = EntityUtils.toString(entity);
                StringReader csvReader = new StringReader(csvString);
                Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(csvReader);

                // Parse each csv lines
                for (CSVRecord record : records) {
                    LocationStat stat = new LocationStat();
                    stat.setState(record.get("Province/State"));
                    stat.setCountry(record.get("Country/Region"));

                    // Stores the current record into a map
                    Map<String, String> csvMap = record.toMap();
                    List<Report> reports = new ArrayList<>();

                    for (Map.Entry<String, String> e : csvMap.entrySet()) {
                        // A date formatted key defines a Report entry.
                        // If it doesn't match, the key matches "Province/State" or "Country/Region" and should be
                        // ignored as it has already been treated
                        if (CoronaVirusDataService.DATE_PATTERN.matcher(e.getKey()).find()) {
                            LocalDate date = CoronaVirusDataService.parseDate(e.getKey());
                            Report report = new Report();
                            report.setDate(date);
                            try {
                                int totalCase = Integer.parseInt(e.getValue());
                                int lastTotalCase = reports.isEmpty() ? 0 : reports.get(reports.size() - 1).getTotalCase();
                                // Prevent the newCases to be negative
                                if (totalCase < lastTotalCase) {
                                    report.setTotalCase(lastTotalCase);
                                    report.setDailyNewCase(0);
                                } else {
                                    report.setTotalCase(totalCase);
                                    report.setDailyNewCase(totalCase - lastTotalCase);
                                }
                                reports.add(report);
                            } catch (NumberFormatException ignored) {
                                // If a NumberFormatException is raised, simply ignore the report.
                            }
                        }
                    }
                    // Sort the reports by chronological order
                    reports.sort(Comparator.comparing(Report::getDate));
                    stat.setReports(reports);
                    newStats.add(stat);
                }
                // Sort the list by country name alphabetically
                newStats.sort(Comparator.comparing(LocationStat::getCountry));
                stats = newStats;
            }
        }
    }

    /**
     * Extract data corresponding to the state
     *
     * @param state the state name
     * @return LocationStat
     */
    public LocationStat getCaseByState(String state) {
        return stats.stream()
                .filter(stat -> stat.getState().equals(state))
                .findFirst().orElse(null);
    }

    /**
     * Extract data corresponding to the country
     *
     * @param country the country name
     * @return List of all LocationStat linked to that country
     */
    private List<LocationStat> getLocationStatsByCountry(String country) {
        return stats.stream()
                .filter(stat -> stat.getCountry().equals(country))
                .collect(Collectors.toList());
    }

    /**
     * Compact all states LocationStat for 1 country into a single one with a null state value
     *
     * @param country the country name
     * @return LocationStat
     */
    public LocationStat getCaseByCountry(String country) {
        LocationStat stat = new LocationStat();
        List<LocationStat> countryLocationStatByStates = getLocationStatsByCountry(country);

        if (country.isEmpty()) {
            return null;
        }

        stat.setCountry(country);

        // Creation of a map of [report's date] -> [total cases], ordered by date of report.
        // The total cases amount is the result of the sum of all reports by date
        Map<LocalDate, Integer> totalCasesByDates = new LinkedHashMap<>();
        countryLocationStatByStates.stream()
                // Extraction of all reports
                .map(LocationStat::getReports)
                .flatMap(Collection::stream)
                // Grouping by report's date and summing all cases for each date collision
                .collect(
                        groupingBy(Report::getDate, summingInt(Report::getTotalCase))
                )
                .entrySet()
                .stream()
                // Sorting by report's date
                .sorted(Map.Entry.comparingByKey())
                .forEachOrdered(x ->
                        totalCasesByDates.put(x.getKey(), x.getValue())
                );


        // Building the report list
        List<Map.Entry<LocalDate, Integer>> entryList = new ArrayList<>(totalCasesByDates.entrySet());
        List<Report> reports = new ArrayList<>();
        for (int i = 0; i < entryList.size(); i++) {
            Report report = new Report();
            Map.Entry<LocalDate, Integer> entry = entryList.get(i);
            report.setDate(entry.getKey());
            report.setTotalCase(entry.getValue());
            // Get the daily new cases amount
            if (i == 0) {
                report.setDailyNewCase(entry.getValue());
            } else {
                report.setDailyNewCase(entry.getValue() - entryList.get(i - 1).getValue());
            }
            reports.add(report);
        }
        stat.setReports(reports);

        return stat;
    }

    /**
     * Get the LocationStats list
     *
     * @return list containing virus stats
     */
    public List<LocationStat> getStats() {
        return stats;
    }
}
