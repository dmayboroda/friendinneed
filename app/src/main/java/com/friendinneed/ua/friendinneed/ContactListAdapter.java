package com.friendinneed.ua.friendinneed;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.friendinneed.ua.friendinneed.model.Contact;

import java.io.InputStream;
import java.util.List;

/**
 * Created by mymac on 9/24/16.
 */

public class ContactListAdapter extends ArrayAdapter<Contact> {

    public ContactListAdapter(Context context, int resource, List<Contact> objects) {
        super(context, resource, objects);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = View.inflate(getContext(), R.layout.contact_list_item, null);
            viewHolder = new ViewHolder();
            viewHolder.contactNameTextView = (TextView) convertView.findViewById(R.id.contact_name_item);
            viewHolder.contactPhoneTextView = (TextView) convertView.findViewById(R.id.contact_number_item);
            viewHolder.contactImageImageView = (ImageView) convertView.findViewById(R.id.contact_image_item);
            convertView.setTag(viewHolder);
        }

        viewHolder = (ViewHolder) convertView.getTag();
        viewHolder.contactNameTextView.setText(getItem(position).getContactName());
        viewHolder.contactPhoneTextView.setText(getItem(position).getContactNumber());

        InputStream input = ContactsContract.Contacts
                .openContactPhotoInputStream(getContext().getContentResolver(), getItem(position).getImageUri());
        Bitmap contactPhotoImage;
        if (input == null) {
            contactPhotoImage = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.default_contact_photo);
        } else {
            contactPhotoImage = BitmapFactory.decodeStream(input);
        }
            viewHolder.contactImageImageView.setImageBitmap(Utils.getCroppedBitmap(contactPhotoImage));

        return convertView;
    }

    private class ViewHolder{
        TextView contactNameTextView;
        TextView contactPhoneTextView;
        ImageView contactImageImageView;
    }


}
