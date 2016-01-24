package me.rijul.dozeonproximity;


import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.TwoStatePreference;
import android.support.v7.widget.Toolbar;

public class MainActivity extends AppCompatPreferenceActivity {

    @SuppressWarnings("deprecation")
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        addPreferencesFromResource(R.xml.preferences);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

       findPreference("service_status").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
           @Override
           public boolean onPreferenceClick(Preference preference) {
               if ( ((TwoStatePreference) preference).isChecked())
                   MainActivity.this.startService(new Intent(MainActivity.this, SensorService.class));
               else
                   MainActivity.this.stopService(new Intent(MainActivity.this, SensorService.class));
               return true;
           }
       });

    }

}