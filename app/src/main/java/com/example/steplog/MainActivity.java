package com.example.steplog;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;
    Fragment homeFragment = new HomeFragment();
    Fragment timerFragment = new TimerFragment();
    Fragment userFragment = new UserFragment();
    Fragment calendarFragment = new CalendarFragment();
    Fragment settingsFragment = new SettingsFragment();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            return insets;
        });

        bottomNavigationView = findViewById(R.id.bottom_nav_menu);
        setCurrentFragment(homeFragment);

        bottomNavigationView.setOnItemSelectedListener(item ->{
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home)
            {
                setCurrentFragment(homeFragment);
            }
            else if (itemId == R.id.nav_timer)
            {
                setCurrentFragment(timerFragment);
            }
            else if (itemId == R.id.nav_user)
            {
                setCurrentFragment(userFragment);
            }
            else if (itemId == R.id.nav_calendar)
            {
                setCurrentFragment(calendarFragment);
            }
            else if (itemId == R.id.nav_settings)
            {
                setCurrentFragment(settingsFragment);
            }
            return true;
        });
    }

    private void setCurrentFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.flFragment, fragment)
                .commit();
    }

}
