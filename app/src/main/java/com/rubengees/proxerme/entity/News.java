/*
 *   Copyright 2015 Ruben Gees
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.rubengees.proxerme.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

/**
 * A bean holding all relevant info of a single news.
 *
 * @author Ruben Gees
 */
public class News implements Parcelable {

    public static final Parcelable.Creator<News> CREATOR = new Parcelable.Creator<News>() {
        public News createFromParcel(Parcel source) {
            return new News(source);
        }

        public News[] newArray(int size) {
            return new News[size];
        }
    };
    private String id;
    private long time;
    private String description;
    private String imageId;
    private String subject;
    private int hits;
    private String threadId;
    private String authorId;
    private String author;
    private int posts;
    private String categoryId;
    private String categoryTitle;

    public News(@NonNull String id, long time, @NonNull String description, @NonNull String imageId,
                @NonNull String subject, int hits, @NonNull String threadId,
                @NonNull String authorId, @NonNull String author, int posts,
                @NonNull String categoryId, @NonNull String categoryTitle) {
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

    protected News(Parcel in) {
        this.id = in.readString();
        this.time = in.readLong();
        this.description = in.readString();
        this.imageId = in.readString();
        this.subject = in.readString();
        this.hits = in.readInt();
        this.threadId = in.readString();
        this.authorId = in.readString();
        this.author = in.readString();
        this.posts = in.readInt();
        this.categoryId = in.readString();
        this.categoryTitle = in.readString();
    }

    public String getId() {
        return id;
    }

    public long getTime() {
        return time;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    public String getImageId() {
        return imageId;
    }

    @NonNull
    public String getSubject() {
        return subject;
    }

    public int getHits() {
        return hits;
    }

    public String getThreadId() {
        return threadId;
    }

    public String getAuthorId() {
        return authorId;
    }

    @NonNull
    public String getAuthor() {
        return author;
    }

    public int getPosts() {
        return posts;
    }

    public String getCategoryId() {
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

        if (time != news.time) return false;
        if (hits != news.hits) return false;
        if (posts != news.posts) return false;
        if (!id.equals(news.id)) return false;
        if (!description.equals(news.description)) return false;
        if (!imageId.equals(news.imageId)) return false;
        if (!subject.equals(news.subject)) return false;
        if (!threadId.equals(news.threadId)) return false;
        if (!authorId.equals(news.authorId)) return false;
        if (!author.equals(news.author)) return false;
        if (!categoryId.equals(news.categoryId)) return false;
        return categoryTitle.equals(news.categoryTitle);

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (int) (time ^ (time >>> 32));
        result = 31 * result + description.hashCode();
        result = 31 * result + imageId.hashCode();
        result = 31 * result + subject.hashCode();
        result = 31 * result + hits;
        result = 31 * result + threadId.hashCode();
        result = 31 * result + authorId.hashCode();
        result = 31 * result + author.hashCode();
        result = 31 * result + posts;
        result = 31 * result + categoryId.hashCode();
        result = 31 * result + categoryTitle.hashCode();
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeLong(this.time);
        dest.writeString(this.description);
        dest.writeString(this.imageId);
        dest.writeString(this.subject);
        dest.writeInt(this.hits);
        dest.writeString(this.threadId);
        dest.writeString(this.authorId);
        dest.writeString(this.author);
        dest.writeInt(this.posts);
        dest.writeString(this.categoryId);
        dest.writeString(this.categoryTitle);
    }
}
