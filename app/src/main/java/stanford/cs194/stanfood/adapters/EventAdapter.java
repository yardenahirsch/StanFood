package stanford.cs194.stanfood.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import stanford.cs194.stanfood.R;
import stanford.cs194.stanfood.helpers.ViewGroupUtils;
import stanford.cs194.stanfood.models.Event;


public class EventAdapter extends ArrayAdapter {

    private ArrayList<Event> events;
    private Context context;
    private ListView eventListView;
    private ViewGroup bottomSheetView;

    public EventAdapter(
            Context context,
            ArrayList<Event> events,
            ListView eventListView,
            ViewGroup bottomSheetView
    ) {
        super(context, R.layout.list_view, events);
        this.context = context;
        this.events = events;
        this.eventListView = eventListView;
        this.bottomSheetView = bottomSheetView;
    }

    @NonNull
    public View getView(int position, View view, @NonNull ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        View rowView = inflater.inflate(R.layout.list_view, null, true);

        // gets references to objects in the list_view.xml file
        TextView eventLocation = bottomSheetView.findViewById(R.id.bottom_sheet_header);
        TextView eventName = rowView.findViewById(R.id.eventName);
        TextView eventTimeStart = rowView.findViewById(R.id.eventTimeStart);
        TextView eventDescription = rowView.findViewById(R.id.eventDescription);

        Event event = events.get(position);
        String name = event.getName();
        String locationName = event.getLocationName();
        long time = event.getTimeStart();
        long duration = event.getDuration();
        String description = event.getDescription();

        // sets the values of the objects to the value from the current event
        // TODO: Temporary null check until we clear out data since some events don't have explicit name fields
        if(name != null && !name.equals("")) eventName.setText(name);
        else  eventName.setText("N/A");

        if(locationName != null) eventLocation.setText(locationName);
        else eventLocation.setText("N/A");

        if(time != 0) eventTimeStart.setText(getEventTimeRange(time, duration));
        else eventTimeStart.setText("N/A");

        if(!description.equals("")) eventDescription.setText(description);
        else eventDescription.setText("N/A");

        rowView.setOnClickListener(new View.OnClickListener(){
            /**
             * When list item is clicked on, display the event information.
             * @param listItemView
             */
            @Override
            public void onClick(View listItemView) {
                LayoutInflater viewInflater = LayoutInflater.from(context);
                // we want to somehow get the database id, so that we can pull more details from the db
                String clickedEventName = ((TextView)listItemView.findViewById(R.id.eventName)).getText().toString();
                String clickedTimeStart = ((TextView)listItemView.findViewById(R.id.eventTimeStart)).getText().toString();
                String clickedEventDescription = ((TextView)listItemView.findViewById(R.id.eventDescription)).getText().toString();

                View infoView = viewInflater.inflate(R.layout.event_info, null, true);
                TextView infoEventName = bottomSheetView.findViewById(R.id.bottom_sheet_header);
                infoEventName.setText(clickedEventName);
                TextView infoEventDescription = infoView.findViewById(R.id.infoEventDescription);
                String descriptionHeader = infoEventDescription.getText().toString();
                infoEventDescription.setText(descriptionHeader + clickedEventDescription);

                ViewGroup bottomSheetContents = bottomSheetView.findViewById(R.id.bottom_sheet_contents);
                ViewGroupUtils viewGroupUtils = new ViewGroupUtils();
                viewGroupUtils.replaceViewById(infoView, bottomSheetContents, 1);
            }
        });

        return rowView;
    }

    /**
     * Given the start time and duration, returns the time range.
     *
     * E.g. given [Mon Jan 15, 4:30PM] and duration [90 min] in milliseconds,
     * return the string "Mon Jan 15, 4:30PM - 6:00PM"
     */
    private String getEventTimeRange(long startTimeInMillis, long durationInMillis) {
        // set start time
        Calendar startTime = Calendar.getInstance();
        startTime.setTimeInMillis(startTimeInMillis);
        // set end time
        Calendar endTime = Calendar.getInstance();
        endTime.setTimeInMillis(startTimeInMillis + durationInMillis);

        SimpleDateFormat startFormat = new SimpleDateFormat("E MMM dd, hh:mma");
        SimpleDateFormat endFormat = new SimpleDateFormat("hh:mma");
        String startTimeStr = startFormat.format(startTime.getTime());
        String endTimeStr = endFormat.format(endTime.getTime());
        return startTimeStr + " - " + endTimeStr;
    }

}
