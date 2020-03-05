package fr.matthieuaudemard.yacot.controllers;

import fr.matthieuaudemard.yacot.models.LocationStat;
import fr.matthieuaudemard.yacot.services.CoronaVirusDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class HomeController {

    CoronaVirusDataService coronaVirusDataService;

    @Autowired
    public HomeController(CoronaVirusDataService coronaVirusDataService) {
        this.coronaVirusDataService = coronaVirusDataService;
    }

    /**
     * Get the total amount of new cases of a LocationStats list
     *
     * @param locationStats list of LocationStats
     * @return the total amount of new cases
     */
    private int getTotalNewCases(List<LocationStat> locationStats) {
        return locationStats.stream()
                .mapToInt(LocationStat::getLastIncrease)
                .sum();
    }

    /**
     * @param locationStats list of LocationStats
     * @return the total amount of
     */
    private int getTotalReportedCases(List<LocationStat> locationStats) {
        return locationStats.stream()
                .mapToInt(LocationStat::getCount)
                .sum();
    }

    @GetMapping("/")
    public String home(Model model) {
        List<LocationStat> locationStats = coronaVirusDataService.getStats();
        int totalReportedCases = getTotalReportedCases(locationStats);
        int totalNewCases = getTotalNewCases(locationStats);

        model.addAttribute("locationStats", locationStats);
        model.addAttribute("totalReportedCases", totalReportedCases);
        model.addAttribute("totalNewCases", totalNewCases);
        return "home";
    }

    @GetMapping("/state/{state}")
    public String state(@PathVariable(value = "state") String state, Model model) {
        LocationStat stat = coronaVirusDataService.getCaseByState(state);
        model.addAttribute("stat", stat);
        return "state";
    }

    @GetMapping("/country/{country}")
    public String country(@PathVariable(value = "country") String country, Model model) {
        LocationStat stat = coronaVirusDataService.getCaseByCountry(country);
        model.addAttribute("stat", stat);
        return "country";
    }
}
