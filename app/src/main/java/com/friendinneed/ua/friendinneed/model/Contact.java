package com.friendinneed.ua.friendinneed.model;

import android.graphics.Bitmap;
import android.net.Uri;

/**
 * Created by mymac on 9/24/16.
 */
public class Contact {

    String name;
    String number;
    String imageUri;

    public Contact(String name, String number, Uri imageUri){
        this.name = name;
        this.number = number;
        this.imageUri = imageUri.toString();
    }

    public String getContactName() {
        return name;
    }

    public void setContactName(String name) {
        this.name = name;
    }

    public String getContactNumber() {
        return number;
    }

    public void setContactNumber(String number) {
        this.number = number;
    }

    public Uri getImageUri() {
        return Uri.parse(imageUri);
    }


}
