package com.example.android.waitlist;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.android.waitlist.data.WaitlistContract;

import java.util.Calendar;
import java.util.Date;


public class GuestListAdapter extends RecyclerView.Adapter<GuestListAdapter.GuestViewHolder> {

    // Holds on to the cursor to display the waitlist
    private Cursor mCursor;
    private Context mContext;

    private long timeWhenBooked;
    public String mobileNumber; // guest's mobile number
    private static final long TOO_LONG = 1000 * 60; // 1 minutes in milliseconds


    /**
     * Constructor using the context and the db cursor
     * @param context the calling context/activity
     * @param cursor the db cursor with waitlist data to display
     */
    GuestListAdapter(Context context, Cursor cursor) {
        this.mContext = context;
        this.mCursor = cursor;
    }

    @Override
    public GuestViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Get the RecyclerView item layout
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.guest_list_item, parent, false);
        return new GuestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(GuestViewHolder holder, int position) {
        // Move the mCursor to the position of the item to be displayed
        if (!mCursor.moveToPosition(position))
            return; // bail if returned null

        // Update the view holder with the information needed to display
        String name = mCursor.getString(mCursor.getColumnIndex(WaitlistContract.WaitlistEntry.COLUMN_GUEST_NAME));
        int partySize = mCursor.getInt(mCursor.getColumnIndex(WaitlistContract.WaitlistEntry.COLUMN_PARTY_SIZE));

        // mobile number and booked time are not displayed, but we need them here so that we can
        // keep track of time waiting and the number for messaging
        mobileNumber = mCursor.getString(mCursor.getColumnIndex(WaitlistContract.WaitlistEntry.COLUMN_MOBILE_NUMBER));
        timeWhenBooked = mCursor.getLong(mCursor.getColumnIndex(WaitlistContract.WaitlistEntry.COLUMN_TIMESTAMP));


        // COMPLETED (6) Retrieve the id from the cursor and
        long id = mCursor.getLong(mCursor.getColumnIndex(WaitlistContract.WaitlistEntry._ID));

        // Display the guest name
        holder.nameTextView.setText(name);
        // Display the party count
        holder.partySizeTextView.setText(String.valueOf(partySize));
        // COMPLETED (7) Set the tag of the itemview in the holder to the id
        holder.itemView.setTag(id);

        // Check how long the guest has been waiting
        checkWaitingTime(holder);

        // get the id of the guest in the second position in the list and send them a text

    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    /**
     * Swaps the Cursor currently held in the adapter with a new one
     * and triggers a UI refresh
     *
     * @param newCursor the new cursor that will replace the existing one
     */
    void swapCursor(Cursor newCursor) {
        // Always close the previous mCursor first
        if (mCursor != null) mCursor.close();
        mCursor = newCursor;
        if (newCursor != null) {
            // Force the RecyclerView to refresh
            this.notifyDataSetChanged();
        }
    }

    /**
     * Inner class to hold the views needed to display a single item in the recycler-view
     */
    class GuestViewHolder extends RecyclerView.ViewHolder {

        // Will display the guest name
        TextView nameTextView;
        // Will display the party size number
        TextView partySizeTextView;
        // Layout for single guest
        LinearLayout singleGuest;



        /**
         * Constructor for our ViewHolder. Within this constructor, we get a reference to our
         * TextViews
         *
         * @param itemView The View that you inflated in
         *                 {@link GuestListAdapter#onCreateViewHolder(ViewGroup, int)}
         */
        GuestViewHolder(View itemView) {
            super(itemView);
            nameTextView = (TextView) itemView.findViewById(R.id.name_text_view);
            partySizeTextView = (TextView) itemView.findViewById(R.id.party_size_text_view);
            singleGuest = (LinearLayout) itemView.findViewById(R.id.single_guest);

        }

    }

    private void checkWaitingTime(GuestViewHolder holder) {

        long currentTime = getTime();
        long bookedTime = timeWhenBooked;
        long timeSinceBooked = currentTime - bookedTime;
        long mediumWaitingTime = TOO_LONG / 2;

        if (timeSinceBooked < TOO_LONG && timeSinceBooked < mediumWaitingTime){
            // the guest was just added, or was added a short time ago. Don't worry about it.
            holder.singleGuest.setBackgroundColor(ContextCompat.getColor(mContext, R.color.warningColourLow));
        } else if (timeSinceBooked >= mediumWaitingTime && timeSinceBooked < TOO_LONG){
            // the guest has been waiting for awhile, but it hasn't been too long. They do need to be seated soon though
            holder.singleGuest.setBackgroundColor(ContextCompat.getColor(mContext, R.color.warningColourMedium));
        }else if (timeSinceBooked >= TOO_LONG && timeSinceBooked > mediumWaitingTime){
            // the guest has been waiting a very long time. Be honest, you forgot about them, didn't you?
            holder.singleGuest.setBackgroundColor(ContextCompat.getColor(mContext, R.color.warningColourHigh));
        }
    }

    private long getTime() {

        // Create a new calendar

        Date time = Calendar.getInstance().getTime();

        return time.getTime();
    }
}