package fr.matthieuaudemard.yacot.services;

import fr.matthieuaudemard.yacot.models.LocationStats;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Spring service accessing to a Github repository which provides location stats for Coronavirus
 */
@Service
public class CoronaVirusDataService {

    private static String VIRUS_DATA_URL = "https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/csse_covid_19_time_series/time_series_19-covid-Confirmed.csv";

    private List<LocationStats> stats = new ArrayList<>();

    /**
     * Get virus data from Github repository
     * This Method is executed just once a day due to scheduled CRON task
     *
     * @throws IOException if request to Github repository fails
     */
    @PostConstruct
    @Scheduled(cron = "0 0 1 * * *")
    public void fetchVirusData() throws IOException {
        List<LocationStats> newStats = new ArrayList<>();
        HttpGet request = new HttpGet(VIRUS_DATA_URL);

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(request)) {
            client.execute(request);

            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                String csvString = EntityUtils.toString(entity);
                StringReader csvReader = new StringReader(csvString);
                Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(csvReader);

                for (CSVRecord record : records) {
                    LocationStats stat = new LocationStats();
                    stat.setCounty(record.get("Province/State"));
                    stat.setState(record.get("Country/Region"));
                    int latestCases = Integer.parseInt(record.get(record.size() - 1));
                    int previousDayCases = Integer.parseInt(record.get(record.size() - 2));
                    stat.setLatestTotalCases(latestCases);
                    stat.setDiffFromPreviousDay(latestCases - previousDayCases);
                    newStats.add(stat);
                }

                stats = newStats;
            }
        }
    }

    /**
     * Get the LocationStats list
     *
     * @return list containing virus stats
     */
    public List<LocationStats> getStats() {
        return stats;
    }
}
