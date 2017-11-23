package com.example.android.waitlist;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.android.waitlist.data.WaitlistContract;
import com.example.android.waitlist.data.WaitlistDbHelper;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private GuestListAdapter mAdapter;
    private SQLiteDatabase mDb;
    private EditText mNewGuestNameEditText;
    private EditText mNewPartySizeEditText;
    private EditText mNewNumberEditText;
    private final static String LOG_TAG = MainActivity.class.getSimpleName();

    // Twilio SMS strings
    private static final String ACCOUNT_SID = "[Twilio SID]";
    private static final String AUTH_TOKEN = "[Twilio Auth Token]";

    long timeWhenBooked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set local attributes to corresponding views
        final RecyclerView waitlistRecyclerView = (RecyclerView) this.findViewById(R.id.all_guests_list_view);
        mNewGuestNameEditText = (EditText) this.findViewById(R.id.person_name_edit_text);
        mNewPartySizeEditText = (EditText) this.findViewById(R.id.party_count_edit_text);
        mNewNumberEditText = (EditText) this.findViewById(R.id.person_mobile_number_edit_text);

        // Set layout for the RecyclerView, because it's a list we are using the linear layout
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        waitlistRecyclerView.setLayoutManager(linearLayoutManager);

        // Create a DB helper (this will create the DB if run for the first time)
        final WaitlistDbHelper dbHelper = new WaitlistDbHelper(this);

        // Keep a reference to the mDb until paused or killed. Get a writable database
        // because you will be adding restaurant customers
        mDb = dbHelper.getWritableDatabase();

        // Get all guest info from the database and save in a cursor
        final Cursor cursor = getAllGuests();

        // Create an adapter for that cursor to display the data
        mAdapter = new GuestListAdapter(this, cursor);

        // Link the adapter to the RecyclerView
        waitlistRecyclerView.setAdapter(mAdapter);

        // set a custom ScrollListener to the RecyclerView
        waitlistRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                // get the second item
                if (linearLayoutManager.findFirstCompletelyVisibleItemPosition() == 1){

                    String toNumber = WaitlistContract.WaitlistEntry.COLUMN_MOBILE_NUMBER;
                    String fromNumber = "[Twilio number]";
                    String textMessage = "You are now second in the waiting list. " +
                            "Your host will be with you soon.";
                    // initialise Twilio
                    Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
                    // Create a message with two phone numbers.
                    // The first number is the "To", the second number is the "From"
                    try {Message.creator(
                            new PhoneNumber(toNumber),
                            new PhoneNumber(fromNumber),
                            textMessage
                    ).create();
                        Toast.makeText(MainActivity.this, textMessage, Toast.LENGTH_SHORT).show();
                    }catch (Exception e){
                        Log.e(LOG_TAG, "Unable to access Twilio services. Message not sent");
                    }

                }
            }
        });

        // COMPLETED (3) Create a new ItemTouchHelper with a SimpleCallback that handles both LEFT and RIGHT swipe directions
        // Create an item touch helper to handle swiping items off the list
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            // COMPLETED (4) Override onMove and simply return false inside
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                //do nothing, we only care about swiping
                return false;
            }

            // COMPLETED (5) Override onSwiped
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                // COMPLETED (8) Inside, get the viewHolder's itemView's tag and store in a long variable id
                //get the id of the item being swiped
                long id = (long) viewHolder.itemView.getTag();
                // COMPLETED (9) call removeGuest and pass through that id
                //remove from DB
                removeGuest(id);
                // COMPLETED (10) call swapCursor on mAdapter passing in getAllGuests() as the argument
                //update the list
                mAdapter.swapCursor(getAllGuests());
            }

            //COMPLETED (11) attach the ItemTouchHelper to the waitlistRecyclerView
        }).attachToRecyclerView(waitlistRecyclerView);
    }

    /**
     * This method is called when user clicks on the Add to waitlist button
     *
     * @param view The calling view (button)
     */
    public void addToWaitlist(View view) {
        if (mNewGuestNameEditText.getText().length() == 0 ||
                mNewPartySizeEditText.getText().length() == 0) {
            return;
        }
        //default party size to 1
        int partySize = 1;
        try {
            //mNewPartyCountEditText inputType="number", so this should always work
            partySize = Integer.parseInt(mNewPartySizeEditText.getText().toString());
        } catch (NumberFormatException ex) {
            Log.e(LOG_TAG, "Failed to parse party size text to number: " + ex.getMessage());
        }

        timeWhenBooked = getTime();
        // Add guest info to mDb
        addNewGuest(mNewGuestNameEditText.getText().toString(), partySize, timeWhenBooked, mNewNumberEditText.getText().toString());

        // Update the cursor in the adapter to trigger UI to display the new list
        mAdapter.swapCursor(getAllGuests());

        //clear UI text fields
        mNewPartySizeEditText.clearFocus();
        mNewGuestNameEditText.getText().clear();
        mNewPartySizeEditText.getText().clear();
        mNewNumberEditText.getText().clear();
    }


    /**
     * Query the mDb and get all guests from the waitlist table
     *
     * @return Cursor containing the list of guests
     */
    private Cursor getAllGuests() {

        return mDb.query(
                WaitlistContract.WaitlistEntry.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                WaitlistContract.WaitlistEntry.COLUMN_TIMESTAMP
        );
    }

    /**
     * Adds a new guest to the mDb including the party count and the current timestamp
     *
     * @param name           Guest's name
     * @param partySize      Number in party
     * @param timeWhenBooked Time when party arrived
     * @param mobileNumber   Guest's mobile number
     * @return id of new record added
     */
    private long addNewGuest(String name, int partySize, long timeWhenBooked, String mobileNumber) {
        ContentValues cv = new ContentValues();

        cv.put(WaitlistContract.WaitlistEntry.COLUMN_GUEST_NAME, name);
        cv.put(WaitlistContract.WaitlistEntry.COLUMN_PARTY_SIZE, partySize);

        // puts the time stamp of when the entry was created into the database
        cv.put(WaitlistContract.WaitlistEntry.COLUMN_TIMESTAMP, timeWhenBooked);

        cv.put(WaitlistContract.WaitlistEntry.COLUMN_MOBILE_NUMBER, mobileNumber);

        return mDb.insert(WaitlistContract.WaitlistEntry.TABLE_NAME, null, cv);
    }


    // COMPLETED (1) Create a new function called removeGuest that takes long id as input and returns a boolean

    /**
     * Removes the record with the specified id
     *
     * @param id the DB id to be removed
     * @return True: if removed successfully, False: if failed
     */
    private boolean removeGuest(long id) {
        // COMPLETED (2) Inside, call mDb.delete to pass in the TABLE_NAME and the condition that WaitlistEntry._ID equals id
        return mDb.delete(WaitlistContract.WaitlistEntry.TABLE_NAME,
                WaitlistContract.WaitlistEntry._ID + "=" + id, null) > 0;
    }

    public long getTime() {

        // Create a new calendar

        Date time = Calendar.getInstance().getTime();

        return time.getTime();
    }

}