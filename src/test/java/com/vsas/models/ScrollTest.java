package com.vsas.models;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ScrollTest {

    @Test
    void getters_return_values() {
        LocalDate d = LocalDate.of(2024, 1, 2);
        Scroll s = new Scroll("S-007", "T", "TEXT", "/p", d);
        assertEquals("S-007", s.getId());
        assertEquals("T", s.getTitle());
        assertEquals("TEXT", s.getType());
        assertEquals("/p", s.getResourcePath());
        assertEquals(d, s.getUploadDate());
    }

    @Test
    void ctor_without_date_sets_today() {
        LocalDate today = LocalDate.now();
        Scroll s = new Scroll("S-100", "Today", "TEXT", "/x");
        assertEquals("S-100", s.getId());
        assertEquals(today, s.getUploadDate());
    }

    @Test
    void noarg_constructor_fields_can_be_set_via_reflection() throws Exception {
        Scroll s = new Scroll();
        set(s, "id", "S-200");
        set(s, "title", "Reflect");
        set(s, "type", "IMAGE");
        set(s, "resourcePath", "/icons/hero-bg.png");
        set(s, "uploadDate", LocalDate.of(2025, 10, 1));
        assertEquals("S-200", s.getId());
        assertEquals("Reflect", s.getTitle());
        assertEquals("IMAGE", s.getType());
        assertEquals("/icons/hero-bg.png", s.getResourcePath());
        assertEquals(LocalDate.of(2025, 10, 1), s.getUploadDate());
    }

    @Test
    void resourcePath_and_type_can_be_text_values() throws Exception {
        Scroll s = new Scroll();
        set(s, "type", "TEXT");
        set(s, "resourcePath", "/scrolls/scroll1.txt");
        assertEquals("TEXT", s.getType());
        assertEquals("/scrolls/scroll1.txt", s.getResourcePath());
    }

    @Test
    void constructor_with_uploaderID_sets_all_fields() {
        Scroll s = new Scroll("S-001", "user123", "Test Scroll", "PDF", "/scrolls/test.pdf");
        assertEquals("S-001", s.getId());
        assertEquals("S-001", s.getID()); // Test alias
        assertEquals("user123", s.getUploaderID());
        assertEquals("Test Scroll", s.getTitle());
        assertEquals("PDF", s.getType());
        assertEquals("/scrolls/test.pdf", s.getResourcePath());
        assertNotNull(s.getUploadTimestamp());
        assertNotNull(s.getUploadDate());
    }

    @Test
    void getID_returns_same_as_getId() {
        Scroll s = new Scroll("S-999", "Title", "TEXT", "/path");
        assertEquals(s.getId(), s.getID());
    }

    @Test
    void getUploaderID_returns_uploader() {
        Scroll s = new Scroll("S-001", "uploader456", "Title", "TEXT", "/path");
        assertEquals("uploader456", s.getUploaderID());
    }

    @Test
    void getUploadTimestamp_returns_timestamp() {
        Scroll s = new Scroll("S-001", "user123", "Title", "TEXT", "/path");
        assertNotNull(s.getUploadTimestamp());
        assertTrue(s.getUploadTimestamp().getTime() > 0);
    }

    @Test
    void getUploadDateString_formats_date_correctly() {
        LocalDate date = LocalDate.of(2025, 11, 2);
        Scroll s = new Scroll("S-001", "Title", "TEXT", "/path", date);
        String dateString = s.getUploadDateString();
        assertEquals("02/11/2025", dateString);
    }

    @Test
    void getUploadDateString_handles_null_uploadDate() throws Exception {
        Scroll s = new Scroll();
        set(s, "uploadDate", null);
        set(s, "uploadTimestamp", new java.util.Date());
        String dateString = s.getUploadDateString();
        assertNotNull(dateString); // Should fallback to DateFormat
    }

    @Test
    void getUploadDateString_returns_null_when_both_dates_null() throws Exception {
        Scroll s = new Scroll();
        set(s, "uploadDate", null);
        set(s, "uploadTimestamp", null);
        assertNull(s.getUploadDateString());
    }

    @Test
    void setTitle_updates_title() {
        Scroll s = new Scroll("S-001", "Original Title", "TEXT", "/path");
        String newTitle = s.setTitle("Updated Title");
        assertEquals("Updated Title", newTitle);
        assertEquals("Updated Title", s.getTitle());
    }

    @Test
    void downloadCount_starts_at_zero() {
        Scroll s = new Scroll("S-001", "Title", "TEXT", "/path");
        assertEquals(0, s.getDownloadCount());
    }

    @Test
    void setDownloadCount_updates_count() {
        Scroll s = new Scroll("S-001", "Title", "TEXT", "/path");
        s.setDownloadCount(5);
        assertEquals(5, s.getDownloadCount());
    }

    @Test
    void incrementDownloadCount_increases_by_one() {
        Scroll s = new Scroll("S-001", "Title", "TEXT", "/path");
        assertEquals(0, s.getDownloadCount());
        s.incrementDownloadCount();
        assertEquals(1, s.getDownloadCount());
        s.incrementDownloadCount();
        assertEquals(2, s.getDownloadCount());
    }

    @Test
    void viewCount_starts_at_zero() {
        Scroll s = new Scroll("S-001", "Title", "TEXT", "/path");
        assertEquals(0, s.getViewCount());
    }

    @Test
    void setViewCount_updates_count() {
        Scroll s = new Scroll("S-001", "Title", "TEXT", "/path");
        s.setViewCount(10);
        assertEquals(10, s.getViewCount());
    }

    @Test
    void incrementViewCount_increases_by_one() {
        Scroll s = new Scroll("S-001", "Title", "TEXT", "/path");
        assertEquals(0, s.getViewCount());
        s.incrementViewCount();
        assertEquals(1, s.getViewCount());
        s.incrementViewCount();
        assertEquals(2, s.getViewCount());
    }

    @Test
    void statistics_can_be_incremented_multiple_times() {
        Scroll s = new Scroll("S-001", "Title", "TEXT", "/path");

        // Increment downloads
        for (int i = 0; i < 5; i++) {
            s.incrementDownloadCount();
        }
        assertEquals(5, s.getDownloadCount());

        // Increment views
        for (int i = 0; i < 10; i++) {
            s.incrementViewCount();
        }
        assertEquals(10, s.getViewCount());
    }

    @Test
    void constructor_with_date_converts_to_timestamp() {
        LocalDate date = LocalDate.of(2025, 1, 15);
        Scroll s = new Scroll("S-001", "Title", "TEXT", "/path", date);
        assertNotNull(s.getUploadTimestamp());

        // Verify the timestamp corresponds to the date
        LocalDate timestampDate = s.getUploadTimestamp().toInstant()
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate();
        assertEquals(date, timestampDate);
    }

    @Test
    void constructor_with_uploaderID_sets_current_date() {
        LocalDate today = LocalDate.now();
        Scroll s = new Scroll("S-001", "user123", "Title", "TEXT", "/path");
        assertEquals(today, s.getUploadDate());
    }

    @Test
    void all_constructors_create_valid_scroll() {
        // Constructor 1: Basic (id, title, type, path)
        Scroll s1 = new Scroll("S-001", "Title1", "TEXT", "/path1");
        assertNotNull(s1.getId());
        assertNotNull(s1.getTitle());
        assertNotNull(s1.getType());
        assertNotNull(s1.getResourcePath());
        assertNotNull(s1.getUploadDate());

        // Constructor 2: With date (id, title, type, path, date)
        Scroll s2 = new Scroll("S-002", "Title2", "PDF", "/path2", LocalDate.now());
        assertNotNull(s2.getId());
        assertNotNull(s2.getUploadDate());

        // Constructor 3: With uploaderID (id, uploaderID, title, type, path)
        Scroll s3 = new Scroll("S-003", "uploader", "Title3", "IMAGE", "/path3");
        assertNotNull(s3.getId());
        assertNotNull(s3.getUploaderID());
        assertNotNull(s3.getUploadTimestamp());

        // Constructor 4: No-arg
        Scroll s4 = new Scroll();
        assertNotNull(s4);
    }

    @Test
    void scroll_with_different_types() {
        Scroll textScroll = new Scroll("S-001", "Text", "TEXT", "/text.txt");
        Scroll pdfScroll = new Scroll("S-002", "PDF", "PDF", "/doc.pdf");
        Scroll imageScroll = new Scroll("S-003", "Image", "IMAGE", "/img.png");

        assertEquals("TEXT", textScroll.getType());
        assertEquals("PDF", pdfScroll.getType());
        assertEquals("IMAGE", imageScroll.getType());
    }

    @Test
    void uploadDateString_formats_single_digit_day_and_month() {
        LocalDate date = LocalDate.of(2025, 1, 5); // January 5
        Scroll s = new Scroll("S-001", "Title", "TEXT", "/path", date);
        String dateString = s.getUploadDateString();
        assertEquals("05/01/2025", dateString);
    }

    @Test
    void uploadDateString_formats_double_digit_day_and_month() {
        LocalDate date = LocalDate.of(2025, 12, 25); // December 25
        Scroll s = new Scroll("S-001", "Title", "TEXT", "/path", date);
        String dateString = s.getUploadDateString();
        assertEquals("25/12/2025", dateString);
    }

    @Test
    void statistics_independent_for_each_scroll() {
        Scroll s1 = new Scroll("S-001", "Scroll1", "TEXT", "/path1");
        Scroll s2 = new Scroll("S-002", "Scroll2", "TEXT", "/path2");

        s1.incrementDownloadCount();
        s1.incrementDownloadCount();
        s2.incrementDownloadCount();

        assertEquals(2, s1.getDownloadCount());
        assertEquals(1, s2.getDownloadCount());

        s1.incrementViewCount();
        s2.incrementViewCount();
        s2.incrementViewCount();
        s2.incrementViewCount();

        assertEquals(1, s1.getViewCount());
        assertEquals(3, s2.getViewCount());
    }

    private static void set(Object target, String field, Object value) throws Exception {
        var f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }
}
