package stanford.cs194.stanfood.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import stanford.cs194.stanfood.R;
import stanford.cs194.stanfood.authentication.Authentication;

public class LoginActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 123; // Arbitrary request code value for signing in

    private Authentication auth = new Authentication();
    private List<AuthUI.IdpConfig> providers = Arrays.asList(
            new AuthUI.IdpConfig.EmailBuilder().build(),
            new AuthUI.IdpConfig.GoogleBuilder().build());
    private boolean createEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Gets whether this activity was started with the intent of creating an event afterwards.
        createEvent = getIntent().getBooleanExtra("createEvent", false);

        // Starts activity to log the user in
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        final int BOTTOM_SHEET_PEEK_HEIGHT = (int)context.getResources().getDimension(R.dimen.bottom_sheet_peek_height);

        IdpResponse response = IdpResponse.fromResultIntent(data);

        if (resultCode == RESULT_OK) {
            // Successfully signed in
            Log.d("Authentication", "User successfully logged in as: "
                    + auth.getCurrentUser().getDisplayName());
            auth.addCurrentUserToDatabase();
            setLoggedInState();
            String text = "Log-In successful!";
            Toast toast = Toast.makeText(context, text, duration);
            toast.setGravity(Gravity.BOTTOM, 0, BOTTOM_SHEET_PEEK_HEIGHT);
            toast.show();

            // If LoginActivity started to create an event, goto CreateEventActivity
            if (createEvent) {
                Intent eventIntent = new Intent(this, CreateEventActivity.class);
                startActivity(eventIntent);
            }
        } else {
            // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
            // ...
            if (response == null) {
                Log.d("Authentication", "Sign in flow cancelled by user");
            } else {
                Log.e("Authentication", "Log in failed with error: "
                        + Objects.requireNonNull(response.getError()).getErrorCode());
                String text = "Log-In failed.";
                Toast toast = Toast.makeText(context, text, duration);
                toast.setGravity(Gravity.BOTTOM, 0, BOTTOM_SHEET_PEEK_HEIGHT);
                toast.show();
            }
        }
        finish();
    }

    /*
     * Saves whether the user is logged in or not in preferences
     */
    @SuppressLint("ApplySharedPref")
    private void setLoggedInState() {
        boolean isLoggedIn = auth.getCurrentUser() != null;
        SharedPreferences prefs = getSharedPreferences("loginStatus", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isLoggedIn", isLoggedIn);
        editor.commit();
    }
}
