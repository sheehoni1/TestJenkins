package aexp.gyroaccel;

import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Environment;
import android.os.PowerManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.DeadObjectException;
import android.hardware.SensorManager;
import android.util.Log;
import android.util.LogPrinter;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Date;

public class SamplingService extends Service implements SensorEventListener {
	static final String LOG_TAG = "GYROCAPTURE";
	static final boolean DEBUG = false;
	static final int SAMPLECTR_MOD = 1000;
	static final int CALIBRATING_LIMIT = 3000;
	static final double ACCEL_DEVIATION_LIMIT = 0.05;
	static final int ACCEL_DEVIATION_LENGTH = 5;
	static final double GYRO_NOISE_LIMIT = 0.06;
	static final long DIFF_UPDATE_TIMEOUT = 100L;
	public static final int  ENGINESTATES_IDLE = 0;
	public static final int ENGINESTATES_CALIBRATING = 1;
	public static final int ENGINESTATES_MEASURING = 2;
	public static final int SENSORTYPE_NA = 0;
	public static final int SENSORTYPE_ACCEL = 1;
	public static final int SENSORTYPE_GYRO = 0;
	public static final int IDX_X = 0;
	public static final int IDX_Y = 1;
	public static final int IDX_Z = 2;
		
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand( intent, flags, startId );
		Log.d( LOG_TAG, "onStartCommand" );
		stopSampling();		// just in case the activity-level service management fails
        rate = SensorManager.SENSOR_DELAY_FASTEST;
		sensorManager = (SensorManager)getSystemService( SENSOR_SERVICE  );
		startSampling();
		Log.d( LOG_TAG, "onStartCommand ends" );
		return START_NOT_STICKY;
	}

	public void onDestroy() {
		super.onDestroy();
		Log.d( LOG_TAG, "onDestroy" );
		stopSampling();
	}

	public IBinder onBind(Intent intent) {
		return serviceBinder;	// cannot bind
	}

// SensorEventListener
    public void onAccuracyChanged (Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent sensorEvent) {
		processSample( sensorEvent );
    }


	private void stopSampling() {
		if( !samplingStarted )
			return;
        if( sensorManager != null ) {
			Log.d( LOG_TAG, "unregisterListener/SamplingService" );
            sensorManager.unregisterListener( this );
		}
        if( captureFile != null ) {
            captureFile.close();
			captureFile = null;
        }
		samplingStarted = false;
		setState( ENGINESTATES_IDLE);
	}

	private void startSampling() {
		if( samplingStarted )
			return;
       	List<Sensor> sensors = sensorManager.getSensorList( Sensor.TYPE_ACCELEROMETER  );
      	accelSensor = sensors.size() == 0 ? null : sensors.get( 0 );
       	sensors = sensorManager.getSensorList( Sensor.TYPE_GYROSCOPE );
      	gyroSensor = sensors.size() == 0 ? null : sensors.get( 0 );
      	initSampling();
       	
      	if( ( accelSensor != null ) && ( gyroSensor != null ) ) {
			Log.d( LOG_TAG, "registerListener/SamplingService" );
           	sensorManager.registerListener( 
                            this, 
                            accelSensor,
                            rate );
           	sensorManager.registerListener( 
                    this, 
                    gyroSensor,
                    rate );
		} else {
			Log.d( LOG_TAG, 
				"Sensor(s) missing: accelSensor: "+
				accelSensor+
				"; gyroSensor: "+
				gyroSensor);
		}
      	captureFile = null;
      	if( DEBUG ) {
      		File captureFileName = new File( 
      				Environment.getExternalStorageDirectory(), 
      				"capture.csv" );
      		try {
      			captureFile = new PrintWriter( new FileWriter( captureFileName, false ) );
      		} catch( IOException ex ) {
      			Log.e( LOG_TAG, ex.getMessage(), ex );
      		}
      	}
      	samplingStarted = true;
	}

	private void initSampling() {
		sampleCounter = 0;
		previousTimeStamp = -1L;
// Values related to calibrating state
		calibratingAccelCounter = 0;
		calibratingLimit = CALIBRATING_LIMIT;
		simulatedGravity[IDX_X] = 0.0;
		simulatedGravity[IDX_Y] = 0.0;
		simulatedGravity[IDX_Z] = 0.0;
		diffTimeStamp = -1L;
		gravityAccelLimitLen = -1;
		setState( ENGINESTATES_CALIBRATING );

	}

	private void setState( int newState ) {
		Log.d( LOG_TAG, "Transitioning from "+getStateName( state )+
				" to "+getStateName( newState )+
				" at sample counter "+sampleCounter );
		if( state != newState ) {
			state = newState;
			if( iGyroAccel != null ) {
				try {
					iGyroAccel.statusMessage( state );
				} catch( DeadObjectException ex ) {
					Log.e( LOG_TAG,"step() callback", ex );
				} catch( RemoteException ex ) {
					Log.e( LOG_TAG, "RemoteException",ex );
				}
			} else
				Log.d( LOG_TAG, "setState: cannot call back activity");
		}
	}

	private String getStateName( int state ) {
		String stateName = null;
		switch( state ) {
		case ENGINESTATES_IDLE:
			stateName = "Idle";
			break;
		
		case ENGINESTATES_CALIBRATING:
			stateName = "Calibrating";
			break;
			
		case ENGINESTATES_MEASURING:
			stateName = "Measuring";
			break;
			
		default:
			stateName = "N/A";
			break;
		}
		return stateName;
	}
	
	private void updateSampleCounter() {		
		++sampleCounter;
		if( ( sampleCounter % SAMPLECTR_MOD ) == 0 ) {
			if( iGyroAccel == null )
				Log.d( 
					LOG_TAG, 
					"updateSampleCounter() callback: cannot call back (sampleCounter: "+
					sampleCounter+
					")" );
			else {
				Log.d( 
					LOG_TAG, 
					"updateSampleCounter() callback: sampleCounter: "+
					sampleCounter );
				try {
					iGyroAccel.sampleCounter( sampleCounter );
				} catch( DeadObjectException ex ) {
					Log.e( LOG_TAG,"step() callback", ex );
				} catch( RemoteException ex ) {
					Log.e( LOG_TAG, "RemoteException",ex );
				}
			}
		}
	}

	private void processSample( SensorEvent sensorEvent ) {
		float values[] = sensorEvent.values;
		if( values.length < 3 )
				return;
		String sensorName = "n/a";
		int sensorType = SENSORTYPE_NA;
		if( sensorEvent.sensor == accelSensor ) {
			sensorName="accel";
			sensorType = SENSORTYPE_ACCEL;
		} else
		if( sensorEvent.sensor == gyroSensor ) {
			sensorName = "gyro";
			sensorType = SENSORTYPE_GYRO;
		}
		if( captureFile != null ) {
			captureFile.println( sensorEvent.timestamp+
					","+
					sensorName+
					","+
					values[0]+
					","+
					values[1]+
					","+
					values[2]);
		}
		updateSampleCounter();
		switch( state ) {
		case ENGINESTATES_CALIBRATING:
			processCalibrating( sensorEvent.timestamp, sensorType, values );
			break;
			
		case ENGINESTATES_MEASURING:
			processMeasuring( sensorEvent.timestamp, sensorType, values );
			break;
		}
	}
	
	private void processCalibrating( long timeStamp, int sensorType, float values [] ) {
		if( sensorType == SENSORTYPE_ACCEL ) {
			simulatedGravity[IDX_X] += (double)values[IDX_X];
			simulatedGravity[IDX_Y] += (double)values[IDX_Y];
			simulatedGravity[IDX_Z] += (double)values[IDX_Z];
			++calibratingAccelCounter;
		}
		if( sensorType == SENSORTYPE_GYRO )
			previousTimeStamp = timeStamp;
		if( sampleCounter >= calibratingLimit ) {
			if( calibratingAccelCounter == 0 ) { // This shouldn't happen
				calibratingLimit += CALIBRATING_LIMIT;
				Log.d( LOG_TAG, "Increasing calibrating limit to "+calibratingLimit+" samples");
			} else {
				double avgDiv = (double)calibratingAccelCounter;
				simulatedGravity[IDX_X] /= avgDiv;
				simulatedGravity[IDX_Y] /= avgDiv;
				simulatedGravity[IDX_Z] /= avgDiv;
				gravityAccelLen = Math.sqrt(
						simulatedGravity[IDX_X]*simulatedGravity[IDX_X] + 
						simulatedGravity[IDX_Y]*simulatedGravity[IDX_Y] + 
						simulatedGravity[IDX_Z]*simulatedGravity[IDX_Z]);
				gravityAccelHighLimit = gravityAccelLen * ( 1.0 + ACCEL_DEVIATION_LIMIT );
				gravityAccelLowLimit = gravityAccelLen * ( 1.0 - ACCEL_DEVIATION_LIMIT );
				Log.d( LOG_TAG, 
						"Simulated gravity vector: x: "+
							simulatedGravity[IDX_X]+
							"; y: "+simulatedGravity[IDX_Y]+
							"; z: "+simulatedGravity[IDX_Z]+
							"; len: "+gravityAccelLen );
				setState( ENGINESTATES_MEASURING );
			}
		}
	}

	private void processMeasuring( long timeStamp, int sensorType, float values [] ) {
		double dv[] = new double[3];
		dv[IDX_X] = (double)values[0];
		dv[IDX_Y] = (double)values[1];
		dv[IDX_Z] = (double)values[2];
		if( sensorType == SENSORTYPE_ACCEL ) {
			double accelLen = Math.sqrt( 
					dv[IDX_X]*dv[IDX_X] + 
					dv[IDX_Y]*dv[IDX_Y] + 
					dv[IDX_Z]*dv[IDX_Z] );
			if( ( accelLen < gravityAccelHighLimit ) && 
				( accelLen > gravityAccelLowLimit ) ) {
				if( gravityAccelLimitLen < 0 )
					gravityAccelLimitLen = ACCEL_DEVIATION_LENGTH;
				--gravityAccelLimitLen;
				if( gravityAccelLimitLen <= 0 ) {
					gravityAccelLimitLen = 0;
					simulatedGravity[IDX_X] = dv[IDX_X];
					simulatedGravity[IDX_Y] = dv[IDX_Y];
					simulatedGravity[IDX_Z] = dv[IDX_Z];
				}
			} else
				gravityAccelLimitLen = -1;
			double[] diff = vecdiff( dv,simulatedGravity );
			double[] rotatedDiff = rotateToEarth( diff );
			if( captureFile != null ) {
				captureFile.println( timeStamp+
						","+
						"vecdiff"+
						","+
						diff[IDX_X]+
						","+
						diff[IDX_Y]+
						","+
						diff[IDX_Z]);
				captureFile.println( timeStamp+
						","+
						"rotateddiff"+
						","+
						rotatedDiff[IDX_X]+
						","+
						rotatedDiff[IDX_Y]+
						","+
						rotatedDiff[IDX_Z]);
			}
			sendDiff( rotatedDiff );
		} else
		if( sensorType == SENSORTYPE_GYRO ) {
			if( previousTimeStamp >= 0L ) {
				double dt = (double)(timeStamp - previousTimeStamp) / 1000000000.0;
				double dx = gyroNoiseLimiter( dv[IDX_X]*dt );
				double dy = gyroNoiseLimiter( dv[IDX_Y]*dt );
				double dz = gyroNoiseLimiter( dv[IDX_Z]*dt );
				rotx( simulatedGravity,-dx);
				roty( simulatedGravity,-dy);
				rotz( simulatedGravity,-dz);
				if( captureFile != null ) {
					if( difflimit())
						captureFile.println( timeStamp+
							","+
							"simul"+
							","+
							simulatedGravity[IDX_X]+
							","+
							simulatedGravity[IDX_Y]+
							","+
							simulatedGravity[IDX_Z]);
				}
			}
			previousTimeStamp = timeStamp;
		}
	}
		
	private double gyroNoiseLimiter( double gyroValue ) {
		double v = gyroValue;
		if( Math.abs( v ) < GYRO_NOISE_LIMIT )
			v = 0.0;
		return v;			
	}

	private void copySimulatedGravity() {
		previousSimulatedGravity[IDX_X] = simulatedGravity[IDX_X];
		previousSimulatedGravity[IDX_Y] = simulatedGravity[IDX_Y];
		previousSimulatedGravity[IDX_Z] = simulatedGravity[IDX_Z];
	}
	
	private boolean difflimit() {
		if( previousSimulatedGravity == null ) {
			previousSimulatedGravity = new double[3];
			copySimulatedGravity();
			return true;
		} else
		if( ( Math.abs( previousSimulatedGravity[IDX_X]-simulatedGravity[ IDX_X ]) > 0.1) ||
			( Math.abs( previousSimulatedGravity[IDX_Y]-simulatedGravity[ IDX_Y ]) > 0.1) ||
			( Math.abs( previousSimulatedGravity[IDX_Z]-simulatedGravity[ IDX_Z ]) > 0.1) ) {
				copySimulatedGravity();
				return true;
			}
		return false;
	}
	
	private void rotz( double vec[], double dz ) {
		double x = vec[IDX_X];
		double y = vec[IDX_Y];
		double z = vec[IDX_Z];
		vec[IDX_X] = x*Math.cos(dz)-y*Math.sin(dz);
		vec[IDX_Y] = x*Math.sin(dz)+y*Math.cos(dz);
	}

	private void rotx( double vec[], double dx ) {
		double x = vec[IDX_X];
		double y = vec[IDX_Y];
		double z = vec[IDX_Z];
		vec[IDX_Y] = y*Math.cos(dx)-z*Math.sin(dx);
		vec[IDX_Z] = y*Math.sin(dx)+z*Math.cos(dx);
	}
				
	private void roty( double vec[], double dy ) {
		double x = vec[IDX_X];
		double y = vec[IDX_Y];
		double z = vec[IDX_Z];
		vec[IDX_Z] = z*Math.cos(dy)-x*Math.sin(dy);
		vec[IDX_X] = z*Math.sin(dy)+x*Math.cos(dy);
	}

	private double[] vecdiff( double v1[], double v2[] ) {
		double diff[] = new double[3];
		diff[IDX_X] = v1[IDX_X] - v2[IDX_X];
		diff[IDX_Y] = v1[IDX_Y] - v2[IDX_Y];
		diff[IDX_Z] = v1[IDX_Z] - v2[IDX_Z];
		return diff;
	}

	private double fixAtanDegree( double deg, double y, double x ) {
		double rdeg = deg;
		if( ( x < 0.0 ) && ( y > 0.0 ) )
			rdeg = Math.PI - deg;
		if( ( x < 0.0 ) && ( y < 0.0 ) )
			rdeg = Math.PI + deg;
		return rdeg;
	}
	
	private double[] rotateToEarth( double diff[] ) {
		double rotatedDiff[] = new double[3];
		rotatedDiff[IDX_X] = diff[IDX_X];
		rotatedDiff[IDX_Y] = diff[IDX_Y];
		rotatedDiff[IDX_Z] = diff[IDX_Z];
		double gravity[] = new double[3];
		gravity[ IDX_X ] = simulatedGravity[ IDX_X ];
		gravity[ IDX_Y ] = simulatedGravity[ IDX_Y ];
		gravity[ IDX_Z ] = simulatedGravity[ IDX_Z ];
		double dz = Math.atan2( gravity[IDX_Y], gravity[IDX_X]);
		dz = fixAtanDegree( dz, gravity[IDX_Y], gravity[IDX_X] );
		rotz( rotatedDiff, -dz );
		rotz( gravity, -dz );
		double dy = Math.atan2( gravity[IDX_X], gravity[IDX_Z]);
		dy = fixAtanDegree( dy, gravity[IDX_X], gravity[IDX_Z]);
		roty( rotatedDiff, -dy );
		return rotatedDiff;
	}
	
	private void sendDiff( double v[] ) {
		long currentTime = System.currentTimeMillis();
		long tdiff =  currentTime - diffTimeStamp;
		if( ( diffTimeStamp < 0L ) || ( tdiff > DIFF_UPDATE_TIMEOUT )) {
			diffTimeStamp = currentTime;
			Log.d( LOG_TAG, "sendDiff: "+v[IDX_X]+","+v[IDX_Y]+","+v[IDX_Z]);
			if( iGyroAccel != null ) {
				try {
					iGyroAccel.diff( v[IDX_X], v[IDX_Y], v[IDX_Z] );
				} catch( DeadObjectException ex ) {
					Log.e( LOG_TAG,"step() callback", ex );
				} catch( RemoteException ex ) {
					Log.e( LOG_TAG, "RemoteException",ex );
				}
			} else
					Log.d(LOG_TAG, "sendDiff: cannot call back main activity");
		}
	}
	
    private IGyroAccel iGyroAccel = null;
    private final ISamplingService.Stub serviceBinder = 
			new ISamplingService.Stub() {
		public void setCallback( IBinder binder ) {
			Log.d( LOG_TAG,"setCallback" );
			iGyroAccel = IGyroAccel.Stub.asInterface( binder );
      	}

		public void removeCallback() {
			Log.d( LOG_TAG, "removeCallback" );
			iGyroAccel = null;
		}

		public boolean isSampling() {
			return samplingStarted;
		}

		public void stopSampling() {
			Log.d( LOG_TAG, "stopSampling" );
			SamplingService.this.stopSampling();
			stopSelf();
		}

		@Override
		public int getState() throws RemoteException {
			return state;
		}

    };


    private int calibratingLimit;
    private int calibratingAccelCounter;
    private double gravityAccelLen;
    private double gravityAccelHighLimit;
    private double gravityAccelLowLimit;
    private int gravityAccelLimitLen;
    private double simulatedGravity[] = new double[3];
    private double previousSimulatedGravity[] = null;
    private int sampleCounter;
    private long previousTimeStamp;
    private long diffTimeStamp;
    private int rate;
    private SensorManager sensorManager;
    private PrintWriter captureFile = null;
	private Sensor accelSensor;
	private Sensor gyroSensor;
	private boolean samplingStarted = false;
	private int state;
}

