/*
 * Copyright [2015-2017] Fraunhofer Gesellschaft e.V., Institute for
 * Open Communication Systems (FOKUS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.rethink.lhcb.client.android;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import eu.rethink.lhcb.client.LHCBClient;
import eu.rethink.lhcb.client.android.objects.ConnectivityMonitorAndroid;
import eu.rethink.lhcb.client.android.objects.ExtendedDeviceAndroid;
import eu.rethink.lhcb.client.objects.ConnectivityMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Random;

/**
 * Android Activity that runs the LHCB Client.
 */
public class LHCBClientAndroid extends AppCompatActivity {

    private static final Logger LOG = LoggerFactory.getLogger(LHCBClientAndroid.class);
    private LHCBClient lhcbClient = null;
    private Switch switchBtn = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lhcb_client_android);
        lhcbClient = new LHCBClient();

        // setup ConnectivityMonitorAndroid
        final ConnectivityMonitor cmInstance = new ConnectivityMonitorAndroid(getApplicationContext());
        //cmInstance.startRunner();
        lhcbClient.setConnectivityMonitorInstance(cmInstance);
        lhcbClient.setExtendedDevice(new ExtendedDeviceAndroid(getApplicationContext()));

        // --- get layout elements ---
        // Broker IP
        final EditText brokerIp = (EditText) findViewById(R.id.broker_ip);

        // Broker Port
        final EditText brokerPort = (EditText) findViewById(R.id.broker_port);

        // Client Name
        final EditText clientName = (EditText) findViewById(R.id.client_name);

        // connection state TextView
        final TextView conMonState = (TextView) findViewById(R.id.conmonstate);

        // thread that updates connection state
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Random r = new Random();
                try {
                    while (!Thread.interrupted()) {
                        //final String text = String.valueOf(r.nextInt());
                        final String text = cmInstance.toJson();
                        //LOG.debug("setting text: {}", text);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                conMonState.setText(text);
                            }
                        });
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
            }
        });
        t.start();

        // switch that starts and stops the LHCB Client
        switchBtn = (Switch) findViewById(R.id.connect_switch);
        switchBtn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                //buttonView.setText("connected");
                lhcbClient.setServerHost(brokerIp.getText().toString());
                lhcbClient.setServerPort(Integer.parseInt(brokerPort.getText().toString()));
                lhcbClient.setName(clientName.getText().toString());
                InputStream inputStream = getResources().openRawResource(R.raw.keystore);
                try {
                    LOG.debug("Trying to load BKS KeyStore");
                    KeyStore bks = KeyStore.getInstance("BKS");
                    bks.load(inputStream, "fraunhofer".toCharArray());
                    lhcbClient.setKeyStore(bks);
                } catch (KeyStoreException e) {
                    e.printStackTrace();
                } catch (CertificateException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                lhcbClient.start();
                //
            } else {
                stopClient();
            }
        });

        int permissionCheck1 = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        int permissionCheck2 = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);

        LOG.info("permissionCheck1: {}", permissionCheck1);
        LOG.info("permissionCheck2: {}", permissionCheck2);
        LOG.info("needed Value: {}", PackageManager.PERMISSION_GRANTED);
        if (permissionCheck1 != PackageManager.PERMISSION_GRANTED || permissionCheck2 != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE}, 1337);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopClient();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopClient();
    }

    @Override
    protected void onResume() {
        super.onResume();
        switchBtn.setChecked(false);
    }

    private void stopClient() {
        AsyncTask<Integer, Integer, Integer> blub = new AsyncTask<Integer, Integer, Integer>() {
            @Override
            protected Integer doInBackground(Integer... params) {
                if (lhcbClient != null) {
                    lhcbClient.stop();
                    lhcbClient.getConnectivityMonitorInstance().stopRunner();
                    return 0;
                }
                return 1;
            }
        };
        blub.execute();
    }

}
