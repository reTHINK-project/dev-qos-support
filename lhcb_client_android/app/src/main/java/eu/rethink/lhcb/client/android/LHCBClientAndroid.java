package eu.rethink.lhcb.client.android;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import eu.rethink.lhcb.client.LHCBClient;
import eu.rethink.lhcb.client.android.objects.ConnectivityMonitorAndroid;
import eu.rethink.lhcb.client.objects.ConnectivityMonitor;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

public class LHCBClientAndroid extends AppCompatActivity {

    private LHCBClient lhcbClient = null;
    private static final Logger LOG = LoggerFactory.getLogger(LHCBClientAndroid.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lhcb_client_android);
        NetworkConfig.createStandardWithoutFile();
        lhcbClient = new LHCBClient();

        final List<ObjectModel> objectModels = ObjectLoader.loadDefault();
        try {
            objectModels.addAll(ObjectLoader.loadJsonStream(getResources().openRawResource(R.raw.omaobjectsspec)));
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }

        //lhcbClient.setConnectivityMonitorClass(ConnectivityMonitorAndroid.class);
        final ConnectivityMonitor cmInstance = new ConnectivityMonitorAndroid(this);
        lhcbClient.setConnectivityMonitorInstance(cmInstance);
        lhcbClient.setServerHost("10.147.65.152");
        //lhcbClient.start(objectModels);

        Button connectBtn = (Button) findViewById(R.id.connectbtn);
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lhcbClient.start(objectModels);
            }
        });

        Button stopBtn = (Button) findViewById(R.id.stopbtn);
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lhcbClient.stop();
            }
        });

        final TextView conMonState = (TextView) findViewById(R.id.conmonstate);
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (lhcbClient != null)
            lhcbClient.stop();
    }
}
