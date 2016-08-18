package eu.rethink.lhcb.client.android;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import eu.rethink.lhcb.client.LHCBClient;
import eu.rethink.lhcb.client.android.objects.ConnectivityMonitorAndroid;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;

import java.util.List;

public class LHCBClientAndroid extends AppCompatActivity {

    private LHCBClient lhcbClient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lhcb_client_android);
        NetworkConfig.createStandardWithoutFile();
        lhcbClient = new LHCBClient();

        List<ObjectModel> objectModels = ObjectLoader.loadDefault();
        try {
            objectModels.addAll(ObjectLoader.loadJsonStream(getResources().openRawResource(R.raw.omaobjectsspec)));
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }

        lhcbClient.setConnectivityMonitorClass(ConnectivityMonitorAndroid.class);
        lhcbClient.setServerHost("10.147.65.152");
        lhcbClient.start(objectModels);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (lhcbClient != null)
            lhcbClient.stop();
    }
}
