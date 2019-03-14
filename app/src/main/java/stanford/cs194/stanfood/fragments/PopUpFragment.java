package stanford.cs194.stanfood.fragments;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import stanford.cs194.stanfood.R;
import stanford.cs194.stanfood.database.Database;
import stanford.cs194.stanfood.models.Food;

public class PopUpFragment extends DialogFragment {

    private String name;
    private String location;
    private String time;
    private String description;
    private String eventId;
    private ViewGroup bottomSheetContents;
    private Display screen;
    private Database db;
    private String url;

    public static PopUpFragment newInstance(String name, String location,
                                            String time, String description,
                                            String eventId,
                                            ViewGroup bottomSheetContents,
                                            Display screen,
                                            Database db) {
        PopUpFragment p = new PopUpFragment();
        // initiate popup variables.
        p.name = name;
        p.location = location;
        p.time = time;
        p.description = description;
        p.eventId = eventId;
        p.bottomSheetContents = bottomSheetContents;
        p.screen = screen;
        p.db = db;
        p.url = null;

        return p;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View popupView = inflater.inflate(R.layout.event_popup, container, false);

        TextView infoLocationName = popupView.findViewById(R.id.infoLocationName);
        TextView infoEventTime = popupView.findViewById(R.id.infoEventTime);
        TextView infoEventDescription = popupView.findViewById(R.id.infoEventDescription);

        String locationText = infoLocationName.getText().toString() + location;
        infoLocationName.setText(locationText);
        String timeText = infoEventTime.getText().toString() + time;
        infoEventTime.setText(timeText);
        infoEventDescription.setText(description);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        loadFoodImages(eventId,popupView);

        popupView.setFocusable(true);
        // dismiss the popup window when touched
//        popupView.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                dismiss();
//                return true;
//            }
//        });

        return popupView;
    }

    /**
     * @param eventId
     * TODO: Retrieves images from Storage and loads into Picasso adapter
     */
    private void loadFoodImages(final String eventId, final View popupView){
        final ImageView eventImage = popupView.findViewById(R.id.infoEventImage);
        db.dbRef.child("food").orderByChild("eventId").equalTo(eventId).
                addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            Food food = ds.getValue(Food.class);
                            url = food.getImagePath();
                            if(url!=null) break;
                        }
                        Point size = new Point();
                        screen.getSize(size);

                        eventImage.requestLayout();
                        eventImage.getLayoutParams().height = size.x;
                        eventImage.getLayoutParams().width = size.x-15;

                        Transformation t = new Transformation() {
                            @Override
                            public Bitmap transform(Bitmap source) {
                                //whatever algorithm here to compute size
                                float ratio = (float) source.getHeight() / (float) source.getWidth();
                                float heightFloat = ((float) eventImage.getLayoutParams().width) * ratio;

                                final android.view.ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) eventImage.getLayoutParams();

                                layoutParams.height = (int) heightFloat;
                                layoutParams.width = (int) eventImage.getLayoutParams().width;
                                eventImage.setLayoutParams(layoutParams);
                                eventImage.setImageBitmap(source);
                                return source;
                            }

                            @Override
                            public String key() {
                                return "transformation";
                            }
                        };

                        if(url==null){
                            Drawable drawable = getContext().getResources().getDrawable(R.drawable.no_picture);
                            eventImage.setImageDrawable(drawable);
                        }
                        else {
                            Picasso.get()
                                    .load(url)
                                    .transform(t)
                                    .error(R.drawable.no_picture)
                                    .into(eventImage);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.d("ERROR", databaseError.toString());
                    }
                });
    }
}
