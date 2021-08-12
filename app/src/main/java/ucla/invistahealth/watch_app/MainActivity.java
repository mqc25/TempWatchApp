package ucla.invistahealth.watch_app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

import ucla.invistahealth.watch_app.databinding.ActivityMainBinding;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}