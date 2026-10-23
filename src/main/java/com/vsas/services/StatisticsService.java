package com.vsas.services;

import com.vsas.models.Scroll;
import com.vsas.models.User;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class StatisticsService {
    private final ScrollService scrollService;
    private final AuthService authService;

    public StatisticsService() {
        this.scrollService = new ScrollService();
        this.authService = new AuthService(Path.of(System.getProperty("scrollvault.dataDir", "data"), "users.json"));
    }

    public StatisticsService(ScrollService scrollService, AuthService authService) {
        this.scrollService = scrollService;
        this.authService = authService;
    }

    // Overall statistics
    public int getTotalScrolls() {
        return scrollService.list().size();
    }

    public int getTotalUsers() throws IOException {
        return authService.getAllUsers().size();
    }

    public int getTotalDownloads() {
        return scrollService.list().stream()
                .mapToInt(Scroll::getDownloadCount)
                .sum();
    }

    public int getTotalViews() {
        return scrollService.list().stream()
                .mapToInt(Scroll::getViewCount)
                .sum();
    }

    // Most popular scrolls
    public List<Scroll> getMostDownloadedScrolls(int limit) {
        return scrollService.list().stream()
                .sorted(Comparator.comparingInt(Scroll::getDownloadCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<Scroll> getMostViewedScrolls(int limit) {
        return scrollService.list().stream()
                .sorted(Comparator.comparingInt(Scroll::getViewCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    // User statistics
    public Map<String, Integer> getScrollCountByUser() {
        Map<String, Integer> userScrollCounts = new HashMap<>();
        for (Scroll scroll : scrollService.list()) {
            String uploader = scroll.getUploaderID();
            if (uploader != null && !uploader.isEmpty()) {
                userScrollCounts.put(uploader, userScrollCounts.getOrDefault(uploader, 0) + 1);
            }
        }
        return userScrollCounts;
    }

    public String getTopContributor() {
        Map<String, Integer> counts = getScrollCountByUser();
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
    }

    // Type statistics
    public Map<String, Integer> getScrollCountByType() {
        Map<String, Integer> typeCounts = new HashMap<>();
        for (Scroll scroll : scrollService.list()) {
            String type = scroll.getType();
            typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);
        }
        return typeCounts;
    }

    // Engagement rate
    public double getAverageDownloadsPerScroll() {
        List<Scroll> scrolls = scrollService.list();
        if (scrolls.isEmpty()) return 0.0;
        return (double) getTotalDownloads() / scrolls.size();
    }

    public double getAverageViewsPerScroll() {
        List<Scroll> scrolls = scrollService.list();
        if (scrolls.isEmpty()) return 0.0;
        return (double) getTotalViews() / scrolls.size();
    }

    // Recent uploads
    public List<Scroll> getRecentUploads(int limit) {
        return scrollService.list().stream()
                .sorted(Comparator.comparing(Scroll::getUploadDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .collect(Collectors.toList());
    }
}
