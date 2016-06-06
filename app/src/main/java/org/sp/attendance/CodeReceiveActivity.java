package org.sp.attendance;

import android.app.ProgressDialog;
import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.Connections;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Daniel Quah on 21/5/2016
 */
public class CodeReceiveActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        Connections.MessageListener,
        Connections.EndpointDiscoveryListener {
    /**
     * Timeouts (in millis) for startAdvertising and startDiscovery.  At the end of these time
     * intervals the app will silently stop advertising or discovering.
     *
     * To set advertising or discovery to run indefinitely, use 0L where timeouts are required.
     */

    private static final long TIMEOUT_DISCOVER = 0L;

    /**
     * Possible states for this application:
     *      IDLE - GoogleApiClient not yet connected, can't do anything.
     *      READY - GoogleApiClient connected, ready to use Nearby Connections API.
     *      ADVERTISING - advertising for peers to connect.
     *      DISCOVERING - looking for a peer that is advertising.
     *      CONNECTED - found a peer.
     */
    @Retention(RetentionPolicy.CLASS)
    @IntDef({STATE_DISCOVERING, STATE_CONNECTED})
    public @interface NearbyConnectionState {}
    private static final int STATE_DISCOVERING = 1026;
    private static final int STATE_CONNECTED = 1027;

    /** GoogleApiClient for connecting to the Nearby Connections API **/
    private GoogleApiClient mGoogleApiClient;

    /** Views and Dialogs **/
    private MyListDialog mMyListDialog;

    /** The endpoint ID of the connected peer, used for messaging **/
    private String mOtherEndpointId;

    private ProgressBar spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive);
        spinner = (ProgressBar) findViewById(R.id.progressBar);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Nearby.CONNECTIONS_API)
                .enableAutoManage(this, this)
                .build();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();

        // Disconnect the Google API client and stop any ongoing discovery or advertising. When the
        // GoogleAPIClient is disconnected, any connected peers will get an onDisconnected callback.
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * Check if the device is connected (or connecting) to a WiFi network.
     * @return true if connected or connecting, false otherwise.
     */
    private boolean isConnectedToNetwork() {
        ConnectivityManager connManager = (ConnectivityManager)
                getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        return (info != null && info.isConnectedOrConnecting());
    }

    /**
     * Begin discovering devices advertising Nearby Connections, if possible.
     */
    private void startDiscovery() {
        if (!isConnectedToNetwork()) {
            return;
        }

        // Discover nearby apps that are advertising with the required service ID.
        String serviceId = getString(R.string.service_id);
        Nearby.Connections.startDiscovery(mGoogleApiClient, serviceId, TIMEOUT_DISCOVER, this)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {

                        } else {

                            }

                        }
                    }
                );
    }

    /**
     * Send a connection request to a given endpoint.
     * @param endpointId the endpointId to which you want to connect.
     * @param endpointName the name of the endpoint to which you want to connect. Not required to
     *                     make the connection, but used to display after success or failure.
     */

    private void connectTo(String endpointId, final String endpointName) {

        // Send a connection request to a remote endpoint. By passing 'null' for the name,
        // the Nearby Connections API will construct a default name based on device model
        // such as 'LGE Nexus 5'.
        String myName = null;
        byte[] myPayload = null;
        Nearby.Connections.sendConnectionRequest(mGoogleApiClient, myName, endpointId, myPayload,
                new Connections.ConnectionResponseCallback() {
                    @Override
                    public void onConnectionResponse(String endpointId, Status status,
                                                     byte[] bytes) {
                        if (status.isSuccess()) {
                            Toast.makeText(CodeReceiveActivity.this, "Connected to " + endpointName + "success!",
                                    Toast.LENGTH_SHORT).show();

                            mOtherEndpointId = endpointId;
                        } else {
                            Toast.makeText(CodeReceiveActivity.this, "Connection to " + endpointName + "failed! Retrying.....",
                                    Toast.LENGTH_LONG).show();
                            startDiscovery();
                        }
                    }
                }, this);
    }


    @Override
    public void onMessageReceived(String endpointId, byte[] payload, boolean isReliable) {
        // A message has been received from a remote endpoint.
    }

    @Override
    public void onDisconnected(String endpointId) {

    }

    @Override
    public void onEndpointFound(final String endpointId, String deviceId, String serviceId,
                                final String endpointName) {

        // This device is discovering endpoints and has located an advertiser. Display a dialog to
        // the user asking if they want to connect, and send a connection request if they do.
        if (mMyListDialog == null) {
            // Configure the AlertDialog that the MyListDialog wraps
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.lecturer_broadcasting);

            // Create the MyListDialog with a listener
            mMyListDialog = new MyListDialog(this, builder, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String selectedEndpointName = mMyListDialog.getItemKey(which);
                    String selectedEndpointId = mMyListDialog.getItemValue(which);

                    CodeReceiveActivity.this.connectTo(selectedEndpointId, selectedEndpointName);
                    mMyListDialog.dismiss();
                }
            });
        }

        mMyListDialog.addItem(endpointName, endpointId);
        mMyListDialog.show();
    }

    @Override
    public void onEndpointLost(String endpointId) {

        // An endpoint that was previously available for connection is no longer. It may have
        // stopped advertising, gone out of range, or lost connectivity. Dismiss any dialog that
        // was offering a connection.
        if (mMyListDialog != null) {
            mMyListDialog.removeItemByValue(endpointId);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        startDiscovery();
    }

    @Override
    public void onConnectionSuspended(int i) {

        // Try to re-connect
        mGoogleApiClient.reconnect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Can't connect to Lecturer device!")
                .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }}).create().show();

    }

}