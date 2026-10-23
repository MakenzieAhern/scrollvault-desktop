package com.vsas.models;

import java.text.DateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class Scroll {
    // canonical fields used across the app
    private String id;                 // scroll ID
    private String title;              // title
    private String type;               // PDF | IMAGE | TEXT
    private String resourcePath;       // classpath/resource path
    private LocalDate uploadDate;      // upload date (for filtering/sorting/UI)

    // additional fields from the provided API
    private String uploaderID;         // who uploaded
    private Date uploadTimestamp;      // timestamp (legacy/alternate representation)

    // statistics fields
    private int downloadCount = 0;     // number of times downloaded
    private int viewCount = 0;         // number of times viewed/previewed

    public Scroll() {}

    // Existing constructor (kept for compatibility)
    public Scroll(String id, String title, String type, String resourcePath) {
        this(id, title, type, resourcePath, LocalDate.now());
    }

    // Existing constructor with LocalDate (used by ScrollService)
    public Scroll(String id, String title, String type, String resourcePath, LocalDate uploadDate) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.resourcePath = resourcePath;
        this.uploadDate = uploadDate;
        this.uploadTimestamp = Date.from(uploadDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    // New constructor from provided API (includes uploaderID, sets current time/date)
    public Scroll(String scrollID,
                  String uploaderID,
                  String title,
                  String type,
                  String resourcePath) {
        this.id = scrollID;
        this.uploaderID = uploaderID;
        this.title = title;
        this.type = type;
        this.resourcePath = resourcePath;
        this.uploadTimestamp = new Date(System.currentTimeMillis());
        this.uploadDate = Instant.ofEpochMilli(this.uploadTimestamp.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    // Canonical getters used by controllers/services
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getType() { return type; }
    public String getResourcePath() { return resourcePath; }
    public LocalDate getUploadDate() {
        if (uploadDate != null) return uploadDate;
        return uploadTimestamp == null ? null : uploadTimestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public void setResourcePath(String resourcePath) { this.resourcePath = resourcePath; }

    // Added for editing support used by controllers
    public String setTitle(String title) { this.title = title; return this.title; }

    // Aliases and additional accessors from the provided API
    public String getID() { return id; }
    public String getUploaderID() { return uploaderID; }
    public Date getUploadTimestamp() { return uploadTimestamp; }
    public String getUploadDateString() {
        if (uploadDate != null) {
            // Format as DD/MM/YYYY
            return String.format("%02d/%02d/%d",
                uploadDate.getDayOfMonth(),
                uploadDate.getMonthValue(),
                uploadDate.getYear());
        }
        if (uploadTimestamp == null) return null;
        // Fallback to date-only format if uploadDate is null
        return DateFormat.getDateInstance().format(uploadTimestamp);
    }

    // Statistics getters and setters
    public int getDownloadCount() { return downloadCount; }
    public void setDownloadCount(int count) { this.downloadCount = count; }
    public void incrementDownloadCount() { this.downloadCount++; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int count) { this.viewCount = count; }
    public void incrementViewCount() { this.viewCount++; }
}
