/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.fragment;

import android.app.*;
import android.content.*;
import android.hardware.*;
import android.os.*;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.*;
import net.java.sip.communicator.util.*;
import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.service.osgi.*;

import java.util.*;

/**
 * This fragment when added to parent <tt>Activity</tt> will listen for
 * proximity sensor updates and turn the screen on and off when NEAR/FAR
 * distance is detected.
 *
 * @author Pawel Domas
 */
public class ProximitySensorFragment
    extends Fragment
    implements SensorEventListener
{

    /**
     * The logger
     */
    private static final Logger logger
            = Logger.getLogger(ProximitySensorFragment.class);

    /**
     * Proximity sensor managed used by this fragment.
     */
    private Sensor proximitySensor;

    /**
     * Unreliable sensor status flag.
     */
    private boolean sensorDisabled;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume()
    {
        super.onResume();

        SensorManager manager = JitsiApplication.getSensorManager();

        // Skips if the sensor has been already attached
        if(proximitySensor != null)
        {
            // Re-registers the listener as it might have been
            // unregistered in onPause()
            manager.registerListener( this, proximitySensor,
                                      SensorManager.SENSOR_DELAY_UI );
            return;
        }

        List<Sensor> sensors = manager.getSensorList(Sensor.TYPE_ALL);
        logger.trace("Device has "+sensors.size()+" sensors");
        for(Sensor s : sensors)
        {
            logger.trace("Sensor "+s.getName()+" type: "+s.getType());
        }

        this.proximitySensor
                = manager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if(proximitySensor == null)
        {
            return;
        }

        logger.info("Using proximity sensor: "+proximitySensor.getName());
        sensorDisabled = false;
        manager.registerListener( this, proximitySensor,
                                  SensorManager.SENSOR_DELAY_UI );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause()
    {
        super.onPause();

        JitsiApplication.getSensorManager().unregisterListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if(proximitySensor != null)
        {
            JitsiApplication.getSensorManager().unregisterListener(this);
            proximitySensor = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onSensorChanged(SensorEvent event)
    {
        if(sensorDisabled)
            return;

        float proximity = event.values[0];
        float max = event.sensor.getMaximumRange();
        logger.debug("Proximity updated: " + proximity + " max range: " + max);

        if(proximity != max)
        {
            screenOff();
        }
        else
        {
            screenOn();
        }
    }

    private ScreenOffDialog getScreenOffDialog()
    {
        Activity activity = getActivity();
        if(activity == null)
        {
            logger.warn("Activity was null when trying to get ScreenOffDialog");
            return null;
        }

        FragmentManager fm = ((OSGiActivity)activity)
            .getSupportFragmentManager();

        return (ScreenOffDialog) fm.findFragmentByTag("screen_off_dialog");
    }

    /**
     * Turns the screen off.
     */
    private void screenOff()
    {
        Activity activity = getActivity();
        if(activity == null || sensorDisabled)
            return;

        FragmentManager fm = ((OSGiActivity)activity)
            .getSupportFragmentManager();
        ScreenOffDialog screenOffDialog = new ScreenOffDialog();
        screenOffDialog.show(fm, "screen_off_dialog");
    }

    /**
     * Turns the screen on.
     */
    private void screenOn()
    {
        ScreenOffDialog screenOffDialog = getScreenOffDialog();
        if(screenOffDialog != null)
        {
            screenOffDialog.dismiss();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
        if(accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
        {
            sensorDisabled = true;
            screenOn();
        }
        else
        {
            sensorDisabled = false;
        }
    }

    /**
     * Blank full screen dialog that captures all keys
     * (BACK is what interest us the most).
     */
    public static class ScreenOffDialog
        extends android.support.v4.app.DialogFragment
    {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
            setStyle(R.style.ScreenOffDialog,
                     android.R.style.Theme_Black_NoTitleBar_Fullscreen);

            Dialog d = super.onCreateDialog(savedInstanceState);
            d.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            d.setOnKeyListener(new DialogInterface.OnKeyListener()
            {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode,
                                     KeyEvent event)
                {
                    // Do not catch volume buttons
                    if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                        || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                        || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE)
                    {
                        return false;
                    }
                    // Capture all other events
                    return true;
                }
            });

            return d;
        }
    }
}
