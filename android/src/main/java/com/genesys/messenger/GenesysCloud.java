package com.genesys.messenger;

import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class GenesysCloud extends ReactContextBaseJavaModule {
    public GenesysCloud(ReactApplicationContext context) {
        super(context);
    }

    @ReactMethod
    public void startChat(String deploymentId, String domain, String tokenStoreKey, Boolean logging) {
        if (getReactApplicationContext().hasCurrentActivity()) {
            getReactApplicationContext().getCurrentActivity().startActivity(
                    GenesysCloudChatActivity.intentFactory(
                            deploymentId, domain, tokenStoreKey, logging)
            );
        } else {
            Log.w(getName(), "Unable to launch chat activity: no current activity");
        }
    }

    @NonNull
    @Override
    public String getName() {
        return "GenesysCloud";
    }
}

