package com.rubengees.proxerme.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
public class News implements Parcelable {

    private int id;
    private long time;
    private String description;
    private long imageId;
    private String subject;
    private int hits;
    private int threadId;
    private int authorId;
    private String author;
    private int posts;
    private int categoryId;
    private String categoryTitle;

    public News(int id, long time, @NonNull String description, long imageId, @NonNull String subject,
                int hits, int threadId, int authorId, @NonNull String author, int posts,
                int categoryId, @NonNull String categoryTitle) {
        this.id = id;
        this.time = time;
        this.description = description;
        this.imageId = imageId;
        this.subject = subject;
        this.hits = hits;
        this.threadId = threadId;
        this.authorId = authorId;
        this.author = author;
        this.posts = posts;
        this.categoryId = categoryId;
        this.categoryTitle = categoryTitle;
    }

    public int getId() {
        return id;
    }

    public long getTime() {
        return time;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    public long getImageId() {
        return imageId;
    }

    @NonNull
    public String getSubject() {
        return subject;
    }

    public int getHits() {
        return hits;
    }

    public int getThreadId() {
        return threadId;
    }

    public int getAuthorId() {
        return authorId;
    }

    @NonNull
    public String getAuthor() {
        return author;
    }

    public int getPosts() {
        return posts;
    }

    public int getCategoryId() {
        return categoryId;
    }

    @NonNull
    public String getCategoryTitle() {
        return categoryTitle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        News news = (News) o;

        if (id != news.id) return false;
        if (time != news.time) return false;
        if (imageId != news.imageId) return false;
        if (hits != news.hits) return false;
        if (threadId != news.threadId) return false;
        if (authorId != news.authorId) return false;
        if (posts != news.posts) return false;
        if (categoryId != news.categoryId) return false;
        if (!description.equals(news.description)) return false;
        if (!subject.equals(news.subject)) return false;
        if (!author.equals(news.author)) return false;
        return categoryTitle.equals(news.categoryTitle);

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (int) (time ^ (time >>> 32));
        result = 31 * result + description.hashCode();
        result = 31 * result + (int) (imageId ^ (imageId >>> 32));
        result = 31 * result + subject.hashCode();
        result = 31 * result + hits;
        result = 31 * result + threadId;
        result = 31 * result + authorId;
        result = 31 * result + author.hashCode();
        result = 31 * result + posts;
        result = 31 * result + categoryId;
        result = 31 * result + categoryTitle.hashCode();
        return result;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeLong(this.time);
        dest.writeString(this.description);
        dest.writeLong(this.imageId);
        dest.writeString(this.subject);
        dest.writeInt(this.hits);
        dest.writeInt(this.threadId);
        dest.writeInt(this.authorId);
        dest.writeString(this.author);
        dest.writeInt(this.posts);
        dest.writeInt(this.categoryId);
        dest.writeString(this.categoryTitle);
    }

    protected News(Parcel in) {
        this.id = in.readInt();
        this.time = in.readLong();
        this.description = in.readString();
        this.imageId = in.readLong();
        this.subject = in.readString();
        this.hits = in.readInt();
        this.threadId = in.readInt();
        this.authorId = in.readInt();
        this.author = in.readString();
        this.posts = in.readInt();
        this.categoryId = in.readInt();
        this.categoryTitle = in.readString();
    }

    public static final Parcelable.Creator<News> CREATOR = new Parcelable.Creator<News>() {
        public News createFromParcel(Parcel source) {
            return new News(source);
        }

        public News[] newArray(int size) {
            return new News[size];
        }
    };
}
