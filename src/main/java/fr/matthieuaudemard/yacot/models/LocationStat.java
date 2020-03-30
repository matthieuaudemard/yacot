package fr.matthieuaudemard.yacot.models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LocationStat {

    private String state;

    private String country;

    private List<Report> reports = new ArrayList<>();

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public int getCount() {
        return !reports.isEmpty() ?
                reports.stream()
                        .mapToInt(Report::getDailyNewCase)
                        .sum()
                : 0;
    }

    public int getLastIncrease() {
        // Search the last report in the list by skipping (size - 1) elements
        Optional<Report> lastReport = reports.stream()
                .skip(reports.size() - (long) 1)
                .findFirst();
        return lastReport.isPresent() ? lastReport.get().getDailyNewCase() : 0;
    }

    public LocalDate getLatestReportDate() {
        return reports.stream()
                .map(Report::getDate)
                .max(LocalDate::compareTo)
                .orElse(null);
    }

    public List<Report> getReports() {
        return reports;
    }

    public void setReports(List<Report> reports) {
        this.reports = reports;
    }
}
