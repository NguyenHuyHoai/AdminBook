package com.example.adminbook.listbooks;

import com.google.firebase.Timestamp;

import java.util.List;

public class ItemBooks {
    private String booksId;
    private String title;
    private String author;
    private String description;
    private List<String> genres;
    private float rating;
    private int ratingsCount;
    private int viewsCount;
    private String imageBook;
    private Timestamp creationTimestamp;
    private List<String> chapter;

    public ItemBooks() {
    }
    public ItemBooks(String booksId, String title, String author, String description,
                     List<String> genres, float rating, int ratingsCount, int viewsCount,
                     String imageBook, Timestamp creationTimestamp, List<String> chapter) {
        this.booksId = booksId;
        this.title = title;
        this.author = author;
        this.description = description;
        this.genres = genres;
        this.rating = rating;
        this.ratingsCount = ratingsCount;
        this.viewsCount = viewsCount;
        this.imageBook = imageBook;
        this.creationTimestamp = creationTimestamp;
        this.chapter = chapter;
    }

    public String getBooksId() {
        return booksId;
    }

    public void setBooksId(String booksId) {
        this.booksId = booksId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public int getRatingsCount() {
        return ratingsCount;
    }

    public void setRatingsCount(int ratingsCount) {
        this.ratingsCount = ratingsCount;
    }

    public int getViewsCount() {
        return viewsCount;
    }

    public void setViewsCount(int viewsCount) {
        this.viewsCount = viewsCount;
    }

    public String getImageBook() {
        return imageBook;
    }

    public void setImageBook(String imageBook) {
        this.imageBook = imageBook;
    }

    public Timestamp getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(Timestamp creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public List<String> getChapter() {
        return chapter;
    }

    public void setChapter(List<String> chapter) {
        this.chapter = chapter;
    }
}
