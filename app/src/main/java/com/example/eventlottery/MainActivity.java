package com.example.eventlottery;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.eventlottery.fragments.CreateEventFragment;

import com.example.eventlottery.fragments.BrowseFragment;

public class MainActivity extends AppCompatActivity {
    //private TextView navBrowse;
   // private TextView navMyEvents;
   // private TextView navScan;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

       // navBrowse = findViewById(R.id.nav_browse);
      //  navMyEvents = findViewById(R.id.nav_my_events);
       // navScan = findViewById(R.id.nav_scan);


        // Load BrowseFragment by default
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new BrowseFragment())
                    .commit();
        }
    }

    //private void setupNavigation() {
    //    navBrowse.setOnClickListener(v-> {
    //        loadFragment(new BrowseFragment());
   //         highlightNavButton(navBrowse);
   //     });

    //    navMyEvents.setOnClickListener(v-> {

   //     });
  //  }


}

