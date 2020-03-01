package fr.matthieuaudemard.yacot.controllers;

import fr.matthieuaudemard.yacot.models.LocationStats;
import fr.matthieuaudemard.yacot.services.CoronaVirusDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    CoronaVirusDataService coronaVirusDataService;

    @Autowired
    public HomeController(CoronaVirusDataService coronaVirusDataService) {
        this.coronaVirusDataService = coronaVirusDataService;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<LocationStats> locationStats = coronaVirusDataService.getStats();
        int totalReportedCases = locationStats.stream()
                .mapToInt(LocationStats::getLatestTotalCases)
                .sum();
        int totalNewCases = locationStats.stream()
                .mapToInt(LocationStats::getDiffFromPreviousDay)
                .sum();
        model.addAttribute("locationStats", locationStats);
        model.addAttribute("totalReportedCases", totalReportedCases);
        model.addAttribute("totalNewCases", totalNewCases);
        return "home";
    }
}
