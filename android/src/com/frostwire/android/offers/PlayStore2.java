/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2018, FrostWire(R). All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.frostwire.android.offers;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.frostwire.util.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author gubatron
 * @author aldenml
 */
public final class PlayStore2 extends StoreBase {

    private static final Logger LOG = Logger.getLogger(PlayStore2.class);

    // Taken from: Google Play Developer Console -> Services & APIs
    // Base64-encoded RSA public key to include in your binary.
    private static final String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAn4zB2rCYz3oXs33iFIHagzwpca0AEvRYHyr2xOW9gGwBokU51LdIjzq5NOzj3++aa9vIvj/K9eFHCPxkXa5g2qjm1+lc+fJwIEA/hAnA4ZIee3KrD52kyTqfZfhEYGklzvarbo3WN2gcUzwvvsVP9e1UZqtoYgFDThttKaFUboqqt1424lp7C2da89WTgHNpUyykIwQ1zYR34YOQ23SFPesSx8Fmz/Nz2rAHBNuFy13OE2LWPK+kLfm8P+tUAOcDSlq0NuT/FkuGpvziPaOS5BVpvfiAjjnUNLfH7dEO5wh7RPAskcNhQH1ykp6RauZFryMJbbHUe6ydGRHzpRkRpwIDAQAB";

    private BillingClient billingClient;
    private PurchasesUpdatedListener purchasesUpdatedListener;

    private final List<Purchase> purchases = new ArrayList<>();

    private static final Object lock = new Object();
    private static PlayStore2 instance;

    @NonNull
    public static PlayStore2 getInstance(@NonNull Context context) {
        synchronized (lock) {
            if (instance == null) {
                instance = new PlayStore2(context.getApplicationContext());
            }
            return instance;
        }
    }

    private PlayStore2(Context context) {
        purchasesUpdatedListener = (responseCode, purchases) -> {
            if (purchases == null) {
                // could be the result of a call to endAsync
                LOG.info("onPurchasesUpdated() - purchases collection is null");
                return;
            }

            if (responseCode == BillingClient.BillingResponse.OK) {
                for (Purchase purchase : purchases) {
                    handlePurchase(purchase);
                }
                // TODO:
                //mBillingUpdatesListener.onPurchasesUpdated(mPurchases);
            } else if (responseCode == BillingClient.BillingResponse.USER_CANCELED) {
                LOG.info("onPurchasesUpdated() - user cancelled the purchase flow - skipping");
            } else {
                LOG.info("onPurchasesUpdated() got unknown resultCode: " + responseCode);
            }
        };

        billingClient = BillingClient.newBuilder(context)
                .setListener(purchasesUpdatedListener)
                .build();
    }

    @Override
    public void refresh() {

    }

    @Override
    public void purchase(Activity activity, Product p) {

    }

    private void handlePurchase(Purchase purchase) {
        if (!verifyValidSignature(purchase.getOriginalJson(), purchase.getSignature())) {
            Log.i(TAG, "Got a purchase: " + purchase + "; but signature is bad. Skipping...");
            return;
        }

        Log.d(TAG, "Got a verified purchase: " + purchase);

        purchases.add(purchase);
    }

    private static boolean verifyValidSignature(String signedData, String signature) {
        try {
            return Security.verifyPurchase(base64EncodedPublicKey, signedData, signature);
        } catch (IOException e) {
            Log.e(TAG, "Got an exception trying to validate a purchase: " + e);
            return false;
        }
    }
}
