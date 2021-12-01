package com.genesys.messenger;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.integration.core.StateEvent;
import com.nanorep.convesationui.messenger.MessengerAccount;
import com.nanorep.convesationui.structure.controller.ChatController;
import com.nanorep.convesationui.structure.controller.ChatEventListener;
import com.nanorep.nanoengine.AccountInfo;
import com.nanorep.sdkcore.utils.NRError;

public class GenesysCloudChatActivity extends AppCompatActivity implements ChatEventListener {
    private static final String TAG = GenesysCloudChatActivity.class.getSimpleName();
    private static final String CONVERSATION_FRAGMENT_TAG = "conversation_fragment";

    public static final String EXTRA_DEPLOYMENT_ID = "deploymentId";
    public static final String EXTRA_DOMAIN = "domain";
    public static final String EXTRA_TOKEN_STORE_KEY = "tokenStoreKey";
    public static final String EXTRA_LOGGING = "logging";

    private ChatController chatController;
    private MenuItem endMenu;
    private AccountInfo account;

    public static Intent intentFactory(String deploymentId, String domain, String tokenStoreKey, Boolean logging) {
        Intent intent = new Intent("com.intent.action.Messenger_CHAT");
        intent.putExtra(EXTRA_DEPLOYMENT_ID, deploymentId);
        intent.putExtra(EXTRA_DOMAIN, domain);
        intent.putExtra(EXTRA_TOKEN_STORE_KEY, tokenStoreKey);
        intent.putExtra(EXTRA_LOGGING, logging);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "onCreate:");

        setContentView(R.layout.fragment_layout);
        setSupportActionBar(findViewById(R.id.chat_toolbar));
        initAccount();
        createChat();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.i(TAG, "onStart: " + chatController == null ? "controller is null" : chatController.isActive() ? "true" : "false");
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (isFinishing()) {
            destructChat();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i(TAG, "onConfigurationChanged:");
    }

    @Override
    public void onError(@NonNull NRError nrError) {
        Log.e(TAG, nrError.getDescription());
    }

    @Override
    public void onChatStateChanged(@NonNull StateEvent stateEvent) {
        Log.i(TAG, "Got chat state: " + stateEvent.getState());

        if (stateEvent.getState() == StateEvent.Ended) {
            finish();
        } else if (stateEvent.getState() == StateEvent.Started) {
            findViewById(R.id.waiting).setVisibility(View.GONE);
            enableMenu(endMenu, true);
        }
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            if (chatController != null) {
                chatController.endChat(true);
            }
        }
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        endMenu = menu.findItem(R.id.end_current_chat);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.end_current_chat) {
            if (chatController != null) {
                chatController.endChat(false);
            }
            item.setEnabled(false);
            return true;
        } else if (item.getItemId() == R.id.destruct_chat) {
            finish();
            enableMenu(endMenu, false);
            item.setEnabled(false);
            return true;
        }
        return false;
    }

    @Override
    public void onPhoneNumberSelected(@NonNull String s) {
        // empty
    }

    @Override
    public void onUploadFileRequest() {
        // empty
    }

    @Override
    public void onUrlLinkSelected(@NonNull String s) {
        // empty
    }

    private void enableMenu(MenuItem menuItem, Boolean enable) {
        if (menuItem != null) {
            menuItem.setEnabled(enable);
            if (enable && !menuItem.isVisible()) {
                menuItem.setVisible(true);
            }
        }
    }

    private void initAccount() {
        MessengerAccount theAccount = new MessengerAccount();
        account = theAccount;
        Intent intent = getIntent();
        theAccount.setDeploymentId(intent.getStringExtra(EXTRA_DEPLOYMENT_ID));
        theAccount.setDomain(intent.getStringExtra(EXTRA_DOMAIN));
        theAccount.setTokenStoreKey(intent.getStringExtra(EXTRA_TOKEN_STORE_KEY));
        theAccount.setLogging(intent.getBooleanExtra(EXTRA_LOGGING, false));
    }

    private void createChat() {
        chatController = new ChatController.Builder(this).chatEventListener(this).build(account, result -> {
            if (result.getError() == null && result.getFragment() != null) {
                openConversationFragment(result.getFragment());
            } else if (result.getError() == null && result.getFragment() == null) {
                Log.e(TAG, "Chat UI failed to load");
            } else if (result.getError() != null) {
                Log.e(TAG, "!!!Failed to load chat: " + result.getError());
            }
        });
    }

    private void openConversationFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        if (!fm.isStateSaved() && !isFinishing()) {
            Fragment chatFrag = fm.findFragmentByTag(CONVERSATION_FRAGMENT_TAG);
            if (chatFrag == fragment) {
                return;
            }

            if (chatFrag != null) {
                chatController.restoreChat(chatFrag);
            } else {
                fm.beginTransaction()
                        .replace(R.id.chat_container, fragment, CONVERSATION_FRAGMENT_TAG)
                        .addToBackStack(CONVERSATION_FRAGMENT_TAG)
                        .commit();
            }
        }
    }

    private void destructChat() {
        if (chatController != null && !chatController.getWasDestructed()) {
            chatController.terminateChat();
            chatController.destruct();
        }
    }
}
