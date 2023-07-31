package com.example.adminbook.listgenres;

public class ItemGenres {
    private String genresId;
    private String genresName;
    public ItemGenres() {
    }

    public ItemGenres(String genresId, String genresName) {
        this.genresId = genresId;
        this.genresName = genresName;
    }

    public String getGenresId() {
        return genresId;
    }

    public void setGenresId(String genresId) {
        this.genresId = genresId;
    }

    public String getGenresName() {
        return genresName;
    }

    public void setGenresName(String genresName) {
        this.genresName = genresName;
    }
}
