package ucla.invistahealth.watch_app.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import ucla.invistahealth.watch_app.R;

public class WatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = WatchFaceService.class.getSimpleName();
    private static final long NORMAL_UPDATE_RATE_MS = 500;
    private static final long MUTE_UPDATE_RATE_MS = TimeUnit.MINUTES.toMillis(1);
    private static final Typeface BOLD_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine implements
            DataClient.OnDataChangedListener {

        static final String COLON_STRING = ":";

        /** Alpha value for drawing time when in mute mode. */
        static final int MUTE_ALPHA = 100;

        /** Alpha value for drawing time when not in mute mode. */
        static final int NORMAL_ALPHA = 255;
        static final int MSG_UPDATE_TIME = 0;
        /** How often {@link #mUpdateTimeHandler} ticks in milliseconds. */
        long mInteractiveUpdateRateMs = NORMAL_UPDATE_RATE_MS;

        /** Handler to update the time periodically in interactive mode. */
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        if (Log.isLoggable(TAG, Log.VERBOSE)) {
                            Log.v(TAG, "updating time");
                        }
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs =
                                    mInteractiveUpdateRateMs - (timeMs % mInteractiveUpdateRateMs);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        boolean mTimeZoneReceiverRegistered = false;

        Paint mBackgroundPaint;
        Paint mDatePaint;
        Paint mHourPaint;
        Paint mMinutePaint;
        Paint mSecondPaint;
        Paint mAmPmPaint;
        Paint mColonPaint;
        float mColonWidth;
        boolean mMute;

        Calendar mCalendar;
        Date mDate;
        SimpleDateFormat mDayOfWeekFormat;
        java.text.DateFormat mDateFormat;

        boolean mShouldDrawColons;
        float mXOffset;
        float mYOffset;
        float mLineHeight;
        String mAmString;
        String mPmString;
        int mInteractiveBackgroundColor = Color.parseColor("black");
        int mInteractiveHourDigitsColor = Color.parseColor("white");
        int mInteractiveMinuteDigitsColor = Color.parseColor("white");
        int mInteractiveSecondDigitsColor = Color.parseColor("white");

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        Engine(){
            //  Ask for a hardware accelerated canvas.
            super(true);
        }

        private void initFormats() {
            mDayOfWeekFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
            mDayOfWeekFormat.setCalendar(mCalendar);
            mDateFormat = DateFormat.getDateFormat(WatchFaceService.this);
            mDateFormat.setCalendar(mCalendar);
        }
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }
        /**
         * Handles time zone and locale changes.
         */
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                initFormats();
                invalidate();
            }
        };
        private void registerTimeZoneReceiver() {
            if (mTimeZoneReceiverRegistered) {
                return;
            }
            mTimeZoneReceiverRegistered = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_LOCALE_CHANGED);
            WatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterTimeZoneReceiver() {
            if (!mTimeZoneReceiverRegistered) {
                return;
            }
            mTimeZoneReceiverRegistered = false;
            WatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private Paint createTextPaint(int defaultInteractiveColor) {
            return createTextPaint(defaultInteractiveColor, NORMAL_TYPEFACE);
        }

        private Paint createTextPaint(int defaultInteractiveColor, Typeface typeface) {
            Paint paint = new Paint();
            paint.setColor(defaultInteractiveColor);
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            Log.d(TAG, "onCreate");
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(WatchFaceService.this)
                    .build());
            Resources resources = WatchFaceService.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);
            mLineHeight = resources.getDimension(R.dimen.digital_line_height);
            mAmString = resources.getString(R.string.digital_am);
            mPmString = resources.getString(R.string.digital_pm);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(mInteractiveBackgroundColor);
            mDatePaint = createTextPaint(
                    ContextCompat.getColor(getApplicationContext(), R.color.digital_date));
            mHourPaint = createTextPaint(mInteractiveHourDigitsColor, BOLD_TYPEFACE);
            mMinutePaint = createTextPaint(mInteractiveMinuteDigitsColor);
            mSecondPaint = createTextPaint(mInteractiveSecondDigitsColor);
            mAmPmPaint = createTextPaint(
                    ContextCompat.getColor(getApplicationContext(), R.color.digital_am_pm));
            mColonPaint = createTextPaint(
                    ContextCompat.getColor(getApplicationContext(), R.color.digital_colons));

            mCalendar = Calendar.getInstance();
            mDate = new Date();
            initFormats();
        }


        @Override
        public void onVisibilityChanged(boolean visible) {
            Log.d(TAG, "onVisibilityChanged: " + visible);
            super.onVisibilityChanged(visible);

            if (visible) {
                // Registers for color changes to the watch face which can be set either on the
                // watch via ({@code DigitalWatchFaceWearableConfigActivity.java}) or on the phone
                // via the Wear OS companion app using
                // ({@code DigitalWatchFaceCompanionConfigActivity}).
                Wearable.getDataClient(getApplicationContext()).addListener(this);

                registerTimeZoneReceiver();

                // Update time zone and date formats, in case they changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                initFormats();

//                initColorPreferencesFromDataLayer();

            } else {
                unregisterTimeZoneReceiver();

                // Unregisters for color changes to the watch face.
                Wearable.getDataClient(getApplicationContext()).removeListener(this);
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {

        }
    }
}
