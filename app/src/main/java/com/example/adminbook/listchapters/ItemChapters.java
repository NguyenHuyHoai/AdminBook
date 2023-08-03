package com.example.adminbook.listchapters;

import com.google.firebase.Timestamp;

public class ItemChapters {
    private String chaptersId;
    private String booksId;
    private String chaptersName;
    private String chaptersContent;
    private Timestamp chaptersTime;
    public ItemChapters() {
    }

    public ItemChapters(String chaptersId, String booksId, String chaptersName,
                        String chaptersContent, Timestamp chaptersTime) {
        this.chaptersId = chaptersId;
        this.booksId = booksId;
        this.chaptersName = chaptersName;
        this.chaptersContent = chaptersContent;
        this.chaptersTime = chaptersTime;
    }

    public String getChaptersId() {
        return chaptersId;
    }

    public void setChaptersId(String chaptersId) {
        this.chaptersId = chaptersId;
    }

    public String getBooksId() {
        return booksId;
    }

    public void setBooksId(String booksId) {
        this.booksId = booksId;
    }

    public String getChaptersContent() {
        return chaptersContent;
    }

    public void setChaptersContent(String chaptersContent) {
        this.chaptersContent = chaptersContent;
    }

    public Timestamp getChaptersTime() {
        return chaptersTime;
    }

    public void setChaptersTime(Timestamp chaptersTime) {
        this.chaptersTime = chaptersTime;
    }

    public String getChaptersName() {
        return chaptersName;
    }

    public void setChaptersName(String chaptersName) {
        this.chaptersName = chaptersName;
    }
}
