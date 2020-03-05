package fr.matthieuaudemard.yacot.models;

import java.time.LocalDate;

public class Report {

    /**
     * Date of the report
     */
    private LocalDate date;

    /**
     * Amount of all cases
     */
    private Integer totalCase = 0;

    /**
     * Amount of new cases since last report
     */
    private Integer dailyNewCase = 0;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getTotalCase() {
        return totalCase;
    }

    public void setTotalCase(Integer totalCase) {
        this.totalCase = totalCase;
    }

    public Integer getDailyNewCase() {
        return dailyNewCase;
    }

    public void setDailyNewCase(Integer dailyNewCase) {
        this.dailyNewCase = dailyNewCase;
    }
}
