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

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import eu.rethink.lhcb.client.LHCBClient;
import eu.rethink.lhcb.client.android.objects.ConnectivityMonitorAndroid;
import eu.rethink.lhcb.client.objects.ConnectivityMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Android Activity that runs the LHCB Client.
 */
public class LHCBClientAndroid extends AppCompatActivity {

    private static final Logger LOG = LoggerFactory.getLogger(LHCBClientAndroid.class);
    private LHCBClient lhcbClient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lhcb_client_android);
        lhcbClient = new LHCBClient();

        final ConnectivityMonitor cmInstance = new ConnectivityMonitorAndroid(getApplicationContext());
        //cmInstance.startRunner();
        lhcbClient.setConnectivityMonitorInstance(cmInstance);

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
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        t.start();

        // switch that starts and stops the LHCB Client
        Switch switchBtn = (Switch) findViewById(R.id.connect_switch);
        switchBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //buttonView.setText("connected");
                    lhcbClient.setServerHost(brokerIp.getText().toString());
                    lhcbClient.setServerPort(Integer.parseInt(brokerPort.getText().toString()));
                    lhcbClient.setName(clientName.getText().toString());
                    lhcbClient.start();
                } else {
                    //buttonView.setText("disconnected");
                    lhcbClient.stop();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (lhcbClient != null)
            lhcbClient.stop();
    }
}
