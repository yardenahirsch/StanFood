package stanford.cs194.stanfood.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Objects;

import stanford.cs194.stanfood.App;
import stanford.cs194.stanfood.R;
import stanford.cs194.stanfood.database.CreateList;
import stanford.cs194.stanfood.database.Database;
import stanford.cs194.stanfood.fragments.BottomSheet;
import stanford.cs194.stanfood.fragments.NavigationDrawer;
import stanford.cs194.stanfood.helpers.Notification;
import stanford.cs194.stanfood.models.Pin;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, OnMarkerClickListener, GoogleMap.OnMapClickListener, GoogleMap.OnCameraMoveStartedListener {

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private BottomSheet bottomSheet;
    private NavigationDrawer drawerLayout;
    private HashMap<LatLng,String> eventStorage;
    private HashMap<LatLng,Marker> markerStorage;

    private FusedLocationProviderClient mFusedLocationClient;
    private float distanceRange = 10000;
    private Database db;
    private Notification notif;
    private SharedPreferences prefs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        db = new Database();

        notif = new Notification(App.getContext());

        eventStorage = new HashMap<>();
        markerStorage = new HashMap<>();
        // Get the transparent toolbar to insert the navigation menu icon

        // Get SharedPreferences for current logged in status.
        prefs = getSharedPreferences("loginStatus", MODE_PRIVATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setAuthenticationMenuOptions();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //adds location marker
        enableMyLocation();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            LatLng current = new LatLng(location.getLatitude(),location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current,16));
                            populatePins(location);
                        }
                    }
                });

        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnCameraMoveStartedListener(this);

        // Get the bottom sheet view
        View bottomSheetView = findViewById(R.id.bottom_sheet);
        bottomSheet = new BottomSheet(bottomSheetView, bottomSheetView.getContext(), mMap);
        bottomSheet.moveListener();
        // Set padding to show Google logo in correct position
        mMap.setPadding(0, 0, 0, (int)bottomSheet.getPeekHeight());

        setupNavigationMenu();
    }

    /**
     * Expand bottom info window when a pin is clicked.
     *
     * @param marker - the pin that is clicked
     * @return - true to indicate the action was successful
     */
    @Override
    public boolean onMarkerClick(Marker marker) {
        LatLng location = marker.getPosition();

        bottomSheet.expand();
        mMap.setPadding(0, 0, 0, (int)bottomSheet.getExpandedHeight());
        mMap.animateCamera(CameraUpdateFactory.newLatLng(location),500,null);

        ListView eventList = findViewById(R.id.eventList);

        CreateList initRows = new CreateList(App.getContext(), db, marker, eventStorage, eventList);
        initRows.createEventList();
        return true;
    }

    /**
     * Listen for when map is clicked and hide bottom sheet if it is expanded.
     * @param latLng - location of click
     */
    @Override
    public void onMapClick(LatLng latLng) {
        if (!bottomSheet.isCollapsed()) {
            bottomSheet.collapse();
        }
    }

    /**
     * Listen for when camera starts moving and collapse bottom sheet.
     * Only want to collapse bottom sheet when user drags the map. Ignore marker clicks.
     * @param i - reason the camera motion started
     */
    @Override
    public void onCameraMoveStarted(int i) {
        if (bottomSheet.isExpanded()) {
            if (i == REASON_GESTURE) {
                mMap.setPadding(0, 0, 0, (int)bottomSheet.getPeekHeight());
                bottomSheet.collapse();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // Check if location permissions are granted and if so enable the
        // location data layer.
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0
                        && grantResults[0]
                        == PackageManager.PERMISSION_GRANTED) {
                    enableMyLocation();
                    break;
                }
        }
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this, new String[]
                            {Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        }
    }

    public void populatePins(final Location cur){
        db.dbRef.child("pins").addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot ds : dataSnapshot.getChildren()){
                            if(ds.hasChildren()){
                                Pin curPin = ds.getValue(Pin.class);
                                LatLng coordinate = curPin.getLocationCoordinate();
                                Location loc = new Location(LocationManager.GPS_PROVIDER);
                                loc.setLatitude(coordinate.latitude);
                                loc.setLongitude(coordinate.longitude);
                                if(cur.distanceTo(loc) < distanceRange &&
                                        !eventStorage.containsKey(coordinate)){
                                    Marker m = mMap.addMarker(new MarkerOptions().position(coordinate));
                                    markerStorage.put(coordinate, m);
                                    eventStorage.put(coordinate, ds.getKey());
                                }
                                if(curPin.getNumEvents() == 0){
                                    Marker m = markerStorage.get(coordinate);
                                    if(m != null) {
                                        m.remove();
                                        markerStorage.remove(coordinate);
                                        eventStorage.remove(coordinate);
                                    }
                                }
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.d("ERROR",databaseError.toString());
                    }
                }
        );
    }

    /**
     * Creates the drawer layout and adds listeners.
     */
    private void setupNavigationMenu() {
        DrawerLayout mDrawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout = new NavigationDrawer(mDrawerLayout);
        final NavigationView navigationView = findViewById(R.id.nav_view);
        drawerLayout.addMenuIconListener();
        drawerLayout.addNavigationListener(loginSignupRunnable(), logOutRunnable(), createEventRunnable(), navigationView);
        setAuthenticationMenuOptions();
        moveCompassPosition();
        createNavigationMenuListener();
    }

    /**
     * Adds a listener so that when the hamburger menu icon is clicked,
     * the navigation menu opens.
     */
    private void createNavigationMenuListener() {
        View menu_view = findViewById(R.id.hamburger_menu);
        menu_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer();
            }
        });
    }

    /**
     * Starts the intent for users to log in or sign up. Returns a Runnable.
     */
    private Runnable loginSignupRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MapsActivity.this, LoginActivity.class);
                intent.putExtra("createEvent", false);
                startActivity(intent);
            }
        };
    }

    /**
     * Starts the intent for users to log out. Returns a Runnable.
     */
    private Runnable logOutRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                AuthUI.getInstance()
                        .signOut(App.getContext())
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            public void onComplete(@NonNull Task<Void> task) {
                                Log.d("Authentication", "User successfully logged out");
                                Context context = getApplicationContext();
                                String text = "Log-Out successful!";
                                Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                                final int BOTTOM_SHEET_PEEK_HEIGHT = (int)context.getResources().getDimension(R.dimen.bottom_sheet_peek_height);
                                toast.setGravity(Gravity.BOTTOM, 0, BOTTOM_SHEET_PEEK_HEIGHT);
                                toast.show();


                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putBoolean("isLoggedIn", false);
                                editor.commit();
                                setAuthenticationMenuOptions();
                            }
                        });
            }
        };
    }

    /**
     * Starts the intent for users to create an event. Returns a Runnable.
     * If the user is logged in, goes straight to creating an event.
     * If the user is not logged in, starts the LoginActivity with the intention
     * to create an event immediately afterwards if the login succeeds.
     */
    private Runnable createEventRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
                Intent intent;
                if (isLoggedIn) {
                    Intent intent = new Intent(MapsActivity.this, CreateEventActivity.class);
                    intent.putExtra("userId", auth.getCurrentUser().getUid());
                    startActivityForResult(intent, CREATE_EVENT);
                    intent = new Intent(MapsActivity.this, CreateEventActivity.class);
                } else {
                    intent = new Intent(MapsActivity.this, LoginActivity.class);
                    intent.putExtra("createEvent", true);
                }
                startActivity(intent);
            }
        };
    }

    /**
     * Checks if user is logged in and displays the corresponding authentication option in menu
     * - User is logged in -> display "Log Out"
     * - User is not logged in -> display "Log In or Sign Up"
     */
    private void setAuthenticationMenuOptions() {
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        final Menu menu = ((NavigationView)findViewById(R.id.nav_view)).getMenu();
        menu.findItem(R.id.login_signup).setVisible(!isLoggedIn);
        menu.findItem(R.id.logout).setVisible(isLoggedIn);
    }

    /**
     * Moves the compass position down, so that the hamburger menu does not cover it.
     */
    private void moveCompassPosition() {
        View compassButton = mapFragment.getView().findViewWithTag("GoogleMapCompass");
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) compassButton.getLayoutParams();
        rlp.addRule(RelativeLayout.ALIGN_PARENT_START, 0);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        rlp.rightMargin = rlp.leftMargin;
        rlp.bottomMargin = 25;
    }
}
