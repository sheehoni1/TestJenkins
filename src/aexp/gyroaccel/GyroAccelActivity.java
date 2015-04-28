package aexp.gyroaccel;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.RemoteException;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.Button;

public class GyroAccelActivity extends Activity 
{
	static final String LOG_TAG = "GYROCAPTURE";
	static final String SAMPLING_SERVICE_ACTIVATED_KEY = "samplingServiceActivated";
	static final String GYROACCEL_KEY = "sampleCounter";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        uiHandler = new Handler();
        setContentView(R.layout.main);
		Log.d( LOG_TAG, "onCreate" );
		if( savedInstanceState != null ) {
			sampleCounterText = savedInstanceState.getString( GYROACCEL_KEY );
			samplingServiceActivated = savedInstanceState.getBoolean( SAMPLING_SERVICE_ACTIVATED_KEY, false );
        } else
            samplingServiceActivated = false;
        Log.d( LOG_TAG, "onCreate; samplingServiceActivated: "+samplingServiceActivated );
		bindSamplingService();
        setContentView( R.layout.main );
		SensorManager sensorManager = 
				(SensorManager)getSystemService( SENSOR_SERVICE  );
		updateSensorName( R.id.accelsensorname,
				sensorManager,
				Sensor.TYPE_ACCELEROMETER,
				"Accelerometer");
		updateSensorName( R.id.gyrosensorname,
				sensorManager,
				Sensor.TYPE_GYROSCOPE,
				"Gyroscope");
		sampleCounterTV = (TextView)findViewById( R.id.samplecounter );
		if( sampleCounterText != null ) {
			sampleCounterTV.setText( sampleCounterText );
			sampleCounterTV.setVisibility( View.VISIBLE );
        }
        ballPanel = (BallPanel)findViewById( R.id.ball);
		statusMessageTV = (TextView)findViewById( R.id.state );
		CheckBox cb = (CheckBox)findViewById( R.id.cb_sampling );
        if( samplingServiceActivated )
            cb.setChecked( true );
        else
            stopSamplingService();
		cb.setOnClickListener( new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				CheckBox cb = (CheckBox)v;
				boolean isChecked = cb.isChecked();
				if( isChecked ) {
					Log.d( LOG_TAG, "sampling activated" );
					samplingServiceActivated = true;
					startSamplingService();
			        sampleCounterTV.setText( sampleCounterText );
					sampleCounterTV.setVisibility( View.VISIBLE );
				} else {
					Log.d( LOG_TAG, "sampling deactivated" );
					samplingServiceActivated = false;
					sampleCounterTV.setVisibility( View.INVISIBLE );
					stopSamplingService();
				}
				
			}
		});
    }
    
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState( outState );
		Log.d( LOG_TAG, "onSaveInstanceState" );
		outState.putBoolean( SAMPLING_SERVICE_ACTIVATED_KEY, samplingServiceActivated ); 
		if( sampleCounterText != null )
			outState.putString( GYROACCEL_KEY, sampleCounterText );
	}

	protected void onDestroy() {
		super.onDestroy();
		Log.d( LOG_TAG, "onDestroy" );
		releaseSamplingService();
	}

	private void updateSensorName(
			int tvID,
			SensorManager sensorManager,
			int sensorType,
			String sensorName) {
		TextView tv = (TextView)findViewById( tvID );
		List<Sensor> sensors = 
				sensorManager.
					getSensorList( 
						sensorType);
		StringBuffer sb = new StringBuffer();
		if( sensors.size() == 0 ) {
			sb.append( sensorName );
			sb.append( ": N/A");
		} else {
			Sensor s = sensors.get(0);
			sb.append( s.getName());
		}
		tv.setText( new String( sb ));
	}
	
	private void startSamplingService() {
		if( samplingServiceRunning )	// shouldn't happen
			stopSamplingService();
        sampleCounterText = "0";
        Intent i = new Intent();
        i.setClassName( "aexp.gyroaccel","aexp.gyroaccel.SamplingService" );
        startService( i );
		samplingServiceRunning = true;				
	}

	private void stopSamplingService() {
		Log.d( LOG_TAG, "stopSamplingService" );
		if( samplingServiceRunning ) {
			stopSampling();
			samplingServiceRunning = false;
		}
	}

	private void bindSamplingService() {
		samplingServiceConnection = new SamplingServiceConnection();
		Intent i = new Intent();
		i.setClassName( "aexp.gyroaccel", "aexp.gyroaccel.SamplingService" );
		bindService( i, samplingServiceConnection, Context.BIND_AUTO_CREATE);
	}

	private void releaseSamplingService() {
		releaseCallbackOnService();
		unbindService( samplingServiceConnection );	  
		samplingServiceConnection = null;
	}


    private void setCallbackOnService() {
		if( samplingService == null )
			Log.e( LOG_TAG, "setCallbackOnService: Service not available" );
		else {
			try {
				samplingService.setCallback( iSteps.asBinder() );
			} catch( DeadObjectException ex ) {
				Log.e( LOG_TAG, "DeadObjectException",ex );
			} catch( RemoteException ex ) {
				Log.e( LOG_TAG, "RemoteException",ex );
			}
		}
	}

    private void releaseCallbackOnService() {
		if( samplingService == null )
			Log.e( LOG_TAG, "releaseCallbackOnService: Service not available" );
		else {
			try {
				samplingService.removeCallback();
			} catch( DeadObjectException ex ) {
				Log.e( LOG_TAG, "DeadObjectException",ex );
			} catch( RemoteException ex ) {
				Log.e( LOG_TAG, "RemoteException",ex );
			}
		}
	}

    private void updateSamplingServiceRunning() {
		if( samplingService == null )
			Log.e( LOG_TAG, "updateSamplingServiceRunning: Service not available" );
		else {
			try {
				samplingServiceRunning = samplingService.isSampling();
			} catch( DeadObjectException ex ) {
				Log.e( LOG_TAG, "DeadObjectException",ex );
			} catch( RemoteException ex ) {
				Log.e( LOG_TAG, "RemoteException",ex );
			}
		}
	}

    private void updateState() {
		if( samplingService == null )
			Log.e( LOG_TAG, "updateState: Service not available" );
		else {
			try {
				state = samplingService.getState();
			} catch( DeadObjectException ex ) {
				Log.e( LOG_TAG, "DeadObjectException",ex );
			} catch( RemoteException ex ) {
				Log.e( LOG_TAG, "RemoteException",ex );
			}
		}
	}
        
    private void stopSampling() {
		Log.d( LOG_TAG, "stopSampling" );
		if( samplingService == null )
			Log.e( LOG_TAG, "stopSampling: Service not available" );
		else {
			try {
				samplingService.stopSampling();
			} catch( DeadObjectException ex ) {
				Log.e( LOG_TAG, "DeadObjectException",ex );
			} catch( RemoteException ex ) {
				Log.e( LOG_TAG, "RemoteException",ex );
			}
		}
	}

    private String getStateName( int state ) {
		String stateName = null;
		switch( state ) {
		case SamplingService.ENGINESTATES_IDLE:
			stateName = "Idle";
			break;

		case SamplingService.ENGINESTATES_CALIBRATING:
			stateName = "Calibrating";
			break;
			
		case SamplingService.ENGINESTATES_MEASURING:
			stateName = "Measuring";
			break;
			
		default:
			stateName = "N/A";
			break;
		}
		return stateName;
	}

    private IGyroAccel.Stub iSteps 
				= new IGyroAccel.Stub() {

		@Override
		public void sampleCounter(int count) throws RemoteException {
			Log.d( LOG_TAG, "sample count: "+count );
			sampleCounterText = Integer.toString( count );
			sampleCounterTV.setText( sampleCounterText );			
		}
		
		public void statusMessage( int newState ) {
			state = newState;
			if( statusMessageTV  != null ) {
				statusMessageTV.setText( getStateName( newState ) );
			}
			switch( state ) {
			case SamplingService.ENGINESTATES_CALIBRATING:
			case SamplingService.ENGINESTATES_IDLE:
				ballPanel.setVisibility( View.INVISIBLE);
				break;
			
			case SamplingService.ENGINESTATES_MEASURING:
				ballPanel.setVisibility( View.VISIBLE);
				break;				
			}
		}

		@Override
		public void diff(double x, double y, double z) throws RemoteException {
			if( ballPanel != null ) {
				SurfaceHolder holder = ballPanel.getHolder();
				Canvas c = holder.lockCanvas();
				if( c != null ) {
					ballPanel.drawBall(c, true, (float)x, (float)y, (float)z);
					holder.unlockCanvasAndPost(c);
				}
			}
		}
    };

    private Handler uiHandler;
    private int state = SamplingService.ENGINESTATES_IDLE;
    private ISamplingService samplingService = null;
    private SamplingServiceConnection samplingServiceConnection = null;
	private boolean samplingServiceRunning = false;
    private boolean samplingServiceActivated = false;
	private TextView sampleCounterTV;
	private TextView statusMessageTV;
	private String sampleCounterText = null;
	public static BallPanel ballPanel = null;
	
    class SamplingServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName className, 
			IBinder boundService ) {
			Log.d( LOG_TAG, "onServiceConnected" );        	
        	samplingService = ISamplingService.Stub.asInterface((IBinder)boundService);
	    	setCallbackOnService();
			updateSamplingServiceRunning();
			updateState();
/*
            if( !samplingServiceRunning )
                startSamplingService();
*/                
		    CheckBox cb = (CheckBox)findViewById( R.id.cb_sampling );
            cb.setChecked( samplingServiceRunning );
			if( statusMessageTV  != null ) {
				statusMessageTV.setText( getStateName( state ) );
			}
			if( state == SamplingService.ENGINESTATES_MEASURING )
				ballPanel.setVisibility( View.VISIBLE);
		 	Log.d( LOG_TAG,"onServiceConnected" );
        }

        public void onServiceDisconnected(ComponentName className) {
        	samplingService = null;
			Log.d( LOG_TAG,"onServiceDisconnected" );
        }
    };


}
