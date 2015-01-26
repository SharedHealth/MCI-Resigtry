package org.sharedhealth.mci.web.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

public class FeedEntry {

    @JsonProperty
    private String id;
    @JsonProperty
    private String publishedDate;
    @JsonProperty
    private String title;
    @JsonProperty
    private String link;
    @JsonProperty
    private String[] categories;
    @JsonProperty
    private Object content;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(String publishedDate) {
        this.publishedDate = publishedDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String[] getCategories() {
        return categories;
    }

    public void setCategories(String[] categories) {
        this.categories = categories;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("FeedEntry{");
        sb.append("\nid='").append(id).append('\'');
        sb.append(",\n publishedDate='").append(publishedDate).append('\'');
        sb.append(",\n title='").append(title).append('\'');
        sb.append(",\n link='").append(link).append('\'');
        sb.append(",\n categories=").append(Arrays.toString(categories));
        sb.append(",\n content=").append(content);
        sb.append('}');
        return sb.toString();
    }
}