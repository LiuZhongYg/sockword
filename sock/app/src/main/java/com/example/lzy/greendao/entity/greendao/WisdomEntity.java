package com.example.lzy.greendao.entity.greendao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT. Enable "keep" sections if you want to edit. 
/**
 * Entity mapped to table "WISDOM_ENTITY".
 */
public class WisdomEntity {

    private Long id;
    private String english;
    private String china;

    public WisdomEntity() {
    }

    public WisdomEntity(Long id) {
        this.id = id;
    }

    public WisdomEntity(Long id, String english, String china) {
        this.id = id;
        this.english = english;
        this.china = china;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEnglish() {
        return english;
    }

    public void setEnglish(String english) {
        this.english = english;
    }

    public String getChina() {
        return china;
    }

    public void setChina(String china) {
        this.china = china;
    }

}
