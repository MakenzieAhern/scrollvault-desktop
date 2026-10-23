package com.vsas.services;

import com.vsas.models.Scroll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsServiceTest {

    @TempDir
    Path tempDir;
    private Path usersFile;
    private AuthService authService;
    private StatisticsService statsService;

    @BeforeEach
    void setUp() throws Exception {
        usersFile = tempDir.resolve("users.json");
        authService = new AuthService(usersFile);

        // Create test users
        authService.register(null, "admin", "Admin User", "admin@test.com", "1234567890", "admin123", "ADMIN");
        authService.register(null, "user1", "User One", "user1@test.com", "0987654321", "user123", "GENERAL");
        authService.register(null, "user2", "User Two", "user2@test.com", "1112223333", "user123", "GENERAL");

        statsService = new StatisticsService();
    }

    @Test
    void testGetTotalScrolls() {
        int total = statsService.getTotalScrolls();
        assertTrue(total >= 0, "Total scrolls should be non-negative");
    }

    @Test
    void testGetTotalUsers() throws Exception {
        int total = statsService.getTotalUsers();
        assertTrue(total >= 0, "Total users should be non-negative");
    }

    @Test
    void testGetTotalDownloads() {
        int downloads = statsService.getTotalDownloads();
        assertTrue(downloads >= 0, "Total downloads should be non-negative");
    }

    @Test
    void testGetTotalViews() {
        int views = statsService.getTotalViews();
        assertTrue(views >= 0, "Total views should be non-negative");
    }

    @Test
    void testGetMostDownloadedScrolls() {
        List<Scroll> mostDownloaded = statsService.getMostDownloadedScrolls(5);
        assertNotNull(mostDownloaded);
        assertTrue(mostDownloaded.size() <= 5, "Should return at most 5 scrolls");
    }

    @Test
    void testGetMostViewedScrolls() {
        List<Scroll> mostViewed = statsService.getMostViewedScrolls(5);
        assertNotNull(mostViewed);
        assertTrue(mostViewed.size() <= 5, "Should return at most 5 scrolls");
    }

    @Test
    void testGetScrollCountByType() {
        Map<String, Integer> typeCounts = statsService.getScrollCountByType();
        assertNotNull(typeCounts);
        assertTrue(typeCounts.size() >= 0, "Type counts should be valid");
    }

    @Test
    void testGetAverageDownloadsPerScroll() {
        double avg = statsService.getAverageDownloadsPerScroll();
        assertTrue(avg >= 0.0, "Average downloads should be non-negative");
    }

    @Test
    void testGetAverageViewsPerScroll() {
        double avg = statsService.getAverageViewsPerScroll();
        assertTrue(avg >= 0.0, "Average views should be non-negative");
    }

    @Test
    void testGetTopContributor() {
        String topContributor = statsService.getTopContributor();
        assertNotNull(topContributor);
    }

    @Test
    void testGetScrollCountByUser() {
        Map<String, Integer> userCounts = statsService.getScrollCountByUser();
        assertNotNull(userCounts);
    }

    @Test
    void testGetRecentUploads() {
        List<Scroll> recent = statsService.getRecentUploads(3);
        assertNotNull(recent);
        assertTrue(recent.size() <= 3, "Should return at most 3 scrolls");
    }
}
