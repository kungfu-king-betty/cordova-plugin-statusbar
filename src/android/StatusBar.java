/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/
package org.apache.cordova.statusbar;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import java.util.Arrays;

import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.R;

public class StatusBar extends CordovaPlugin {
    private static final String TAG = "StatusBar";
    private static int NOTIFICATION_ID = 1234;

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    @Override
    public void initialize(final CordovaInterface cordova, CordovaWebView webView) {
        LOG.v(TAG, "StatusBar: initialization");
        super.initialize(cordova, webView);

        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Clear flag FLAG_FORCE_NOT_FULLSCREEN which is set initially
                // by the Cordova.
                Window window = cordova.getActivity().getWindow();
                window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

                // Read 'StatusBarBackgroundColor' from config.xml, default is #000000.
                setStatusBarBackgroundColor(preferences.getString("StatusBarBackgroundColor", "#000000"));

                // Read 'StatusBarStyle' from config.xml, default is 'lightcontent'.
                setStatusBarStyle(preferences.getString("StatusBarStyle", "lightcontent"));
            }
        });
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArry of arguments for the plugin.
     * @param callbackContext   The callback id used when calling back into JavaScript.
     * @return                  True if the action was valid, false otherwise.
     */
    @Override
    public boolean execute(final String action, final CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        LOG.v(TAG, "Executing action: " + action);
        final Activity activity = this.cordova.getActivity();
        final Window window = activity.getWindow();

        if ("_ready".equals(action)) {
            boolean statusBarVisible = (window.getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) == 0;
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, statusBarVisible));
            return true;
        }

        if ("show".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // SYSTEM_UI_FLAG_FULLSCREEN is available since JellyBean, but we
                    // use KitKat here to be aligned with "Fullscreen"  preference
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        int uiOptions = window.getDecorView().getSystemUiVisibility();
                        uiOptions &= ~View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                        uiOptions &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;

                        window.getDecorView().setSystemUiVisibility(uiOptions);
                    }

                    // CB-11197 We still need to update LayoutParams to force status bar
                    // to be hidden when entering e.g. text fields
                    window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }
            });
            return true;
        }

        if ("hide".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // SYSTEM_UI_FLAG_FULLSCREEN is available since JellyBean, but we
                    // use KitKat here to be aligned with "Fullscreen"  preference
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        int uiOptions = window.getDecorView().getSystemUiVisibility()
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_FULLSCREEN;

                        window.getDecorView().setSystemUiVisibility(uiOptions);
                    }

                    // CB-11197 We still need to update LayoutParams to force status bar
                    // to be hidden when entering e.g. text fields
                    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }
            });
            return true;
        }

        if ("backgroundColorByHexString".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        setStatusBarBackgroundColor(args.getString(0));
                    } catch (JSONException ignore) {
                        LOG.e(TAG, "Invalid hexString argument, use f.i. '#777777'");
                    }
                }
            });
            return true;
        }

        if ("overlaysWebView".equals(action)) {
            if (Build.VERSION.SDK_INT >= 21) {
                this.cordova.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            setStatusBarTransparent(args.getBoolean(0));
                        } catch (JSONException ignore) {
                            LOG.e(TAG, "Invalid boolean argument");
                        }
                    }
                });
                return true;
            }
            else return args.getBoolean(0) == false;
        }

        if ("styleDefault".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setStatusBarStyle("default");
                }
            });
            return true;
        }

        if ("styleLightContent".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setStatusBarStyle("lightcontent");
                }
            });
            return true;
        }

        if ("styleBlackTranslucent".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setStatusBarStyle("blacktranslucent");
                }
            });
            return true;
        }

        if ("styleBlackOpaque".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setStatusBarStyle("blackopaque");
                }
            });
            return true;
        }

        if ("showSpinner".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // NOTIFICATION_ID = UUID.randomUUID();
                        showSpinner(NOTIFICATION_ID, args.getString(0), args.getString(1));
                    } catch (Exception ignore) {
                        System.out.println("Spinner Error:" + ignore);
                    }
                }
            });
            return true;
        }

        if ("hideSpinner".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        hideSpinner(NOTIFICATION_ID);
                    } catch (Exception ignore) {
                        System.out.println("Spinner Error:" + ignore);
                    }
                }
            });
            return true;
        }

        return false;
    }

    private void setStatusBarBackgroundColor(final String colorPref) {
        if (Build.VERSION.SDK_INT >= 21) {
            if (colorPref != null && !colorPref.isEmpty()) {
                final Window window = cordova.getActivity().getWindow();
                // Method and constants not available on all SDKs but we want to be able to compile this code with any SDK
                window.clearFlags(0x04000000); // SDK 19: WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.addFlags(0x80000000); // SDK 21: WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                try {
                    // Using reflection makes sure any 5.0+ device will work without having to compile with SDK level 21
                    window.getClass().getMethod("setStatusBarColor", int.class).invoke(window, Color.parseColor(colorPref));
                } catch (IllegalArgumentException ignore) {
                    LOG.e(TAG, "Invalid hexString argument, use f.i. '#999999'");
                } catch (Exception ignore) {
                    // this should not happen, only in case Android removes this method in a version > 21
                    LOG.w(TAG, "Method window.setStatusBarColor not found for SDK level " + Build.VERSION.SDK_INT);
                }
            }
        }
    }

    private void setStatusBarTransparent(final boolean transparent) {
        if (Build.VERSION.SDK_INT >= 21) {
            final Window window = cordova.getActivity().getWindow();
            if (transparent) {
                window.getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                window.setStatusBarColor(Color.TRANSPARENT);
            }
            else {
                window.getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_VISIBLE);
            }
        }
    }

    private void setStatusBarStyle(final String style) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (style != null && !style.isEmpty()) {
                View decorView = cordova.getActivity().getWindow().getDecorView();
                int uiOptions = decorView.getSystemUiVisibility();

                String[] darkContentStyles = {
                    "default",
                };

                String[] lightContentStyles = {
                    "lightcontent",
                    "blacktranslucent",
                    "blackopaque",
                };

                if (Arrays.asList(darkContentStyles).contains(style.toLowerCase())) {
                    decorView.setSystemUiVisibility(uiOptions | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                    return;
                }

                if (Arrays.asList(lightContentStyles).contains(style.toLowerCase())) {
                    decorView.setSystemUiVisibility(uiOptions & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                    return;
                }

                LOG.e(TAG, "Invalid style, must be either 'default', 'lightcontent' or the deprecated 'blacktranslucent' and 'blackopaque'");
            }
        }
    }

    private void showSpinner(int THIS_NOTIFICATION_ID, String notification_title, String notification_text) {
        Context this_ctx = (Context) this.cordova.getActivity();

        NotificationManager mNotificationManager =
                    (NotificationManager) this_ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        Sting app_name = getApplicationName(this_ctx);
        String CHANNEL_ID = app_name + "_channel_" + THIS_NOTIFICATION_ID;
        CharSequence name = app_name + "_channel";
        String Description = "This is a notification channel for the "+app_name+" app";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mChannel.setDescription(Description);
            mChannel.enableLights(false);
            mChannel.enableVibration(false);
            mChannel.setShowBadge(false);
            mNotificationManager.createNotificationChannel(mChannel);
        }
        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this_ctx, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this_ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_popup_sync)
            .setContentTitle(notification_title)
            .setContentText(notification_text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // Set the intent that will fire when the user taps the notification
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setTimeoutAfter(300000);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this_ctx);
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(THIS_NOTIFICATION_ID, builder.build());
    }

    private void hideSpinner(int THIS_NOTIFICATION_ID) {
        Context this_ctx = (Context) this.cordova.getActivity();

        NotificationManager notificationManager = 
                    (NotificationManager) this_ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        
        notificationManager.cancel(THIS_NOTIFICATION_ID);
    }

    private String getApplicationName(Context app_ctx) {
        ApplicationInfo applicationInfo = app_ctx.getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : app_ctx.getString(stringId);
    }
}
