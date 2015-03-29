package hoopsnake.geosource;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import ServerClientShared.FieldWithContent;
import ServerClientShared.GeotagFieldWithContent;
import ServerClientShared.GeotagFieldWithoutContent;
import ServerClientShared.ImageFieldWithContent;
import ServerClientShared.ImageFieldWithoutContent;
import ServerClientShared.Incident;
import ServerClientShared.StringFieldWithContent;
import ServerClientShared.StringFieldWithoutContent;
import hoopsnake.geosource.comm.TaskSendIncident;
import hoopsnake.geosource.data.AppField;
import hoopsnake.geosource.data.AppIncident;
import hoopsnake.geosource.data.AppIncidentWithWrapper;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

/**
 * @author wsv759
 * The activity that controls the app when the user is filling out a new incident.
 * This is called from MainActivity when the "add new incident" button is pressed.
 * This activity also launches the tasks to receive a new incident spec and send a new incident.
 */
public class IncidentActivity extends ActionBarActivity {
    private boolean clickable = true;
    private final ReentrantLock clickableLock = new ReentrantLock();

    private final AppGeotagWrapper appGeotagWrapper = new AppGeotagWrapper();

    private static final String LOG_TAG = "geosource";
    public static final String PARAM_STRING_CHANNEL_NAME = "channelName";
    public static final String PARAM_STRING_CHANNEL_OWNER = "channelOwner";
    public static final String PARAM_STRING_POSTER = "poster";

    public static final String SHAREDPREF_CUR_INCIDENT_EXISTS = "sharedpref_incident";
    private static final String FILENAME_CUR_INCIDENT = "cur_incident_object";
    private static final String DIRNAME_INCIDENTS_YET_TO_SEND = "incidents_yet_to_send";
    private static final String FILE_PREFIX_INCIDENT_YET_TO_SEND = "incident_yet_to_send_";

    /** The LinearLayout that displays all the fields of the incident. */
    private LinearLayout incidentDisplay;

    /** The incident to be created and edited by the user on this screen. */
    private AppIncident incident;

    public void setCurFieldIdx(int curFieldIdx) {
        this.curFieldIdx = curFieldIdx;
    }

    /**
     * The position of the currently-selected field in the incidentDisplay.
     * This is recorded so that different activities/fragments can be called whenever a field is clicked,
     * and the corresponding field can be remembered upon their return.
     */
    private int curFieldIdx = NO_CUR_FIELD_SELECTED;
    public static final int NO_CUR_FIELD_SELECTED = -1;

    /**
     * The set of all request codes that are used by this activity when starting new activities or fragments.
     */
    public enum RequestCode {
        FIELD_ACTION_REQUEST_CODE
    }

    /**
     * Initialize the display, and its underlying incident. There are only two legitimate cases for initializing the incident:
     *  1. There are no extras, but an incident is stored in the shared preferences.
     *      In this case we are resuming filling out an incident from before.
     *  2. There are extras, and no incident is stored in the shared preferences.
     *      In this case we send off for the incident spec, based on the channel name and channel owner provided by the extras.
     * @param savedInstanceState automatically handled by android.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incident);
        assertNull(incident);

        incidentDisplay = (LinearLayout) findViewById(R.id.incident_holder);

        Bundle extras = getIntent().getExtras();

        if ((extras == null || extrasAreEmpty(extras)) && curIncidentExistsInFileSystem(this))
            initializeAppIncidentFromPreexistingState();
        else if (extras != null)
            initializeAppIncidentFromServer(extras);
        else
            throw new RuntimeException("invalid onCreate scenario for IncidentActivity.");

//        // TODO If folder is not empty, and we are connected to the internet, send those files!
//        File savedIncidentsDir = getDir(DIRNAME_INCIDENTS_YET_TO_SEND, Context.MODE_PRIVATE);
//        File[] savedIncidentFiles = savedIncidentsDir.listFiles();
//
//        assertNotNull(savedIncidentFiles);
//        if (savedIncidentFiles.length > 0) {
//            File activityBaseFilesDir = getFilesDir();
//            activityBaseFilesDir.getAbsolutePath();
//            for (File incidentFileToSend : savedIncidentFiles) {
//                AppIncident incidentToSend = (AppIncident) FileIO.readObjectFromFile(this, incidentFileToSend.getAbsolutePath());
//            }
//        }
    }

    private void initializeAppIncidentFromPreexistingState()
    {
        retrieveIncidentState();

        assertNotNull(incident);
        assertNotNull(incident.toIncident());
        assertNotNull(incident.getFieldList());
        assertNotNull(incident.getChannelName());
        assertNotNull(incident.getChannelOwner());
        assertNotNull(incident.getIncidentAuthor());

        renderIncidentFromScratch();
    }

    private void initializeAppIncidentFromServer(Bundle extras)
    {
        //get a geotag as fast as possible.
        appGeotagWrapper.update(IncidentActivity.this);

        String channelName = extras.getString(PARAM_STRING_CHANNEL_NAME);
        String channelOwner = extras.getString(PARAM_STRING_CHANNEL_OWNER);
        String poster = extras.getString(PARAM_STRING_POSTER);
        assertNotNull(channelName);
        assertNotNull(channelOwner);
        assertNotNull(poster);

//        new TaskReceiveIncidentSpec(IncidentActivity.this).execute(channelName, channelOwner, poster);
        //TODO uncomment the above code once spec can be pulled properly, then remove up to "renderIncidentFromScratch()"
        ArrayList<FieldWithContent> l = new ArrayList<>();
        l.add(new StringFieldWithContent(new StringFieldWithoutContent("StringTitle", true)));
        l.add(new GeotagFieldWithContent(new GeotagFieldWithoutContent("GeotagTitle", true)));
        l.add(new ImageFieldWithContent(new ImageFieldWithoutContent("ImageTitle", true)));
        // etc.

        incident = new AppIncidentWithWrapper(new Incident(l, channelName, channelOwner, poster), IncidentActivity.this);
        renderIncidentFromScratch();
    }

    private boolean extrasAreEmpty(Bundle extras)
    {
        assertNotNull(extras);
        return !extras.containsKey(PARAM_STRING_CHANNEL_NAME);
    }

    public void setIncident(AppIncident incident) {
        this.incident = incident;
    }

    /**
     * @precond the current incident, and all its fields, and the incidentDisplay, are not null.
     * @postcond each field's custom view is added to the linear layout, replacing all the old
     * views in the linear layout (if they existed).
     *
     * IMPORTANT: All views are given a tag that is equal to their position in the linear layout.
     * They must preserve this tag, as IncidentActivity relies upon it.
     */
    public void renderIncidentFromScratch()
    {
        assertNotNull(incident);
        assertNotNull(incident.getFieldList());
        assertNotNull(incidentDisplay);

        //TODO grosssss... this accounts for refilling an incident from shared preferences, but man...
        if (!incident.getFieldList().get(Incident.POSITION_GEOTAG_FIELD).contentIsFilled())
            incident.setGeotag(appGeotagWrapper);

        //TODO this causes problems by removing the views defined in XML, and I don't think the incident ever needs to be re-rendered anyway.
//        incidentDisplay.removeAllViews();

        //Fill in the author, channel, and channel owner on the UI.
        TextView authorView = (TextView) incidentDisplay.getChildAt(1);
        authorView.setText(authorView.getText() + " " + incident.getIncidentAuthor());
        TextView channelNameView = (TextView) incidentDisplay.getChildAt(2);
        channelNameView.setText(channelNameView.getText() + " " + incident.getChannelName());
        TextView channelOwnerView = (TextView) incidentDisplay.getChildAt(3);
        channelOwnerView.setText(channelOwnerView.getText() + " " + incident.getChannelOwner());

        int i = 0;
        for (AppField field : incident.getFieldList())
        {
            assertNotNull(field);

            View v = field.getFieldViewRepresentation(RequestCode.FIELD_ACTION_REQUEST_CODE.ordinal());
            assertNotNull(v);

            incidentDisplay.addView(v);
            //All views are given a tag that is equal to their position in the linear layout.
            v.setTag(i);
            i++;
        }
    }

    /**
     * When an activity launched from here returns, respond accordingly.
     * @param requestCode the request code with which the activity was launched.
     * @param resultCode the result of the activity.
     * @param data any extra data associated with the result. This could be null.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        RequestCode requestCodeVal = RequestCode.values()[requestCode];
        switch(requestCodeVal)
        {
            case FIELD_ACTION_REQUEST_CODE:
                /**
                 * Delegate the response to the field whose selection launched the returning activity in the first place.
                 */
                assertFalse(curFieldIdx == NO_CUR_FIELD_SELECTED);
                AppField curField = incident.getFieldList().get(curFieldIdx);
                curField.onResultFromSelection(resultCode, data);
                break;
            default:
                throw new RuntimeException("invalid request code " + requestCode + ".");
        }
    }

    /**
     * Try to submit the incident to the server.
     * @param v the submit button.
     * @precond none.
     * @postcond the new incident is submitted to the server. The new incident may not end up going through.
     */
    public void onSubmitButtonClicked(View v)
    {
        if (incident != null && incident.isCompletelyFilledIn()) {
            //TODO uncomment this when actually using it.
            Toast.makeText(IncidentActivity.this, "Attempting to format and send your incident to server.", Toast.LENGTH_LONG).show();

            new TaskSendIncident(IncidentActivity.this).execute(incident);

            setIncident(null);
//            saveIncidentState();
            setResult(RESULT_OK);
            finish();
        }
        else
            Toast.makeText(IncidentActivity.this, "incident has not been completely filled in!",Toast.LENGTH_LONG).show();
    }

    /**
     *
     * @param v the cancel button.
     * @precond none.
     * @postcond Cancel: stop creating the current incident, and discard it forever.
     * (The media files created during the process will still be stored.)
     */
    public void onCancelButtonClicked(View v)
    {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Cancelling Incident Creation")
                .setMessage("Are you sure? This incident will be discarded forever. (Any media files you created will be preserved.)")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setIncident(null);
//                        saveIncidentState();
//                        Intent intent = IncidentActivity.this.getIntent();
//                        intent.putExtra("SOMETHING", false);
//                        IncidentActivity.this.setResult(RESULT_CANCELED, intent);
                        IncidentActivity.this.finish();
                    }

                })
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Test-and-set: atomically check if this activity can be clicked, and if it can, say that no other
     * thread can click it.
     * @return true if the activity is not busy and can be clicked, false otherwise.
     */
    private boolean canLaunch() {
        clickableLock.lock();
        if (clickable) {
            clickable = false;
            clickableLock.unlock();
            return true;
        }

        clickableLock.unlock();
        return false;
    }

    /**
     * When a thread is done using this activity, allow other threads to use it again.
     */
    private void doneLaunching() {
        clickableLock.lock();
        assertFalse(clickable);
        clickable = true;
        clickableLock.unlock();
    }

    /**
     *
     * @param taskThatLaunches a task that needs to be run by some other class, and uses this activity's code to launch an activity or fragment.
     * @precond taskThatLaunches is not null.
     * @postcond taskThatLaunches is run if this activity is not busy; otherwise it does not run.
     */
    private void doIfLaunchable(Runnable taskThatLaunches)
    {
        if (canLaunch())
        {
            taskThatLaunches.run();

            doneLaunching();
        }
    }

    /**
     *
     * @param v the view that needs to launch some activity or fragment on click.
     * @param onClickLaunchable a piece of runnable code that could launch an activity or fragment from this IncidentActivity.
     */
    public void makeViewLaunchable(final View v, final Runnable onClickLaunchable)
    {
        v.setClickable(true);

        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                doIfLaunchable(new Runnable() {
                    @Override
                    public void run() {
                        //Make sure the activity knows which view was clicked.
                        setCurFieldIdx((int) v.getTag());

                        onClickLaunchable.run();
                    }
                });
            }
        });
    }


    @Override
    protected void onPause() {
        super.onPause();

        saveIncidentState();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (incident == null)
            retrieveIncidentState();
    }

    /**
     * Save the current incident state, if the incident has not yet been submitted.
     * This serializes the whole incident into a file, so that it can
     * be deserialized the next time the incident activity is launched.
     * TODO ensure the precond actually holds.
     * @precond as long as incident != null, no file content fields are filled. Thus there will be no
     * attempt to serialize a massive file content object!
     * @postcond the incident is serialized, so that it can be reopened later. Or if the incident is null,
     * no serialization occurs and a new incident will be created next time.
     *
     * NOTE: the sharedPreferences key SHAREDPREF_CUR_INCIDENT_EXISTS is updated accordingly. Other activities
     * should ask for that key to see whether a cur incident exists.
     */
    private void saveIncidentState()
    {
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_sharedpref_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        if (incident == null) {
            deleteFile(FILENAME_CUR_INCIDENT);
            editor.putBoolean(SHAREDPREF_CUR_INCIDENT_EXISTS, false);
            editor.commit();
            return;
        }

        boolean fileWasWritten = FileIO.writeObjectToFile(IncidentActivity.this, (AppIncidentWithWrapper) incident, FILENAME_CUR_INCIDENT);
        if (fileWasWritten)
            editor.putBoolean(SHAREDPREF_CUR_INCIDENT_EXISTS, true);
        else
        {
            editor.putBoolean(SHAREDPREF_CUR_INCIDENT_EXISTS, false);
            String msg = "Current incident was lost.";
            Log.e(LOG_TAG, msg);
            Toast.makeText(this, msg + " Any created media files were saved.", Toast.LENGTH_LONG).show();
        }

        editor.commit();
    }

    /**
     * @precond incident == null. Otherwise this method should not be called as you already have an incident!
     * Retrieve the current incident state, by deserializing it from the file saved to by {@link #saveIncidentState()}.
     * If there is no current incident to restore, this method does nothing.
     */
    private void retrieveIncidentState()
    {
        assertNull(incident);

        if (curIncidentExistsInFileSystem(this)) {
            incident = (AppIncident) FileIO.readObjectFromFile(this, FILENAME_CUR_INCIDENT);
            for (AppField field : incident.getFieldList())
                field.setActivity(this);
        }
    }

    /**
     * @precond none.
     * @return true if there is a current incident serialized in the file system. False otherwise.
     */
    public static boolean curIncidentExistsInFileSystem(Context context)
    {
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.app_sharedpref_file_key), Context.MODE_PRIVATE);
        return sharedPref.getBoolean(SHAREDPREF_CUR_INCIDENT_EXISTS, false);
    }
}

