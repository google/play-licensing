/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.google.android.vending.licensing;


import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.Policy;
import com.google.android.vending.licensing.ResponseData;
import com.google.android.vending.licensing.ServerManagedPolicy;

import android.content.Context;
import android.provider.Settings;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test suite for StrictPolicy.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ServerManagedPolicyTest {

    private ServerManagedPolicy p;

    @Before
    public void initFixture() {
        final byte[] SALT = new byte[] {
            104, -12, 112, 82, -85, -10, -11, 61, 15, 54, 44, -66, -117, -89, -64, 110, -53, 123, 33
        };

        Context ctx = InstrumentationRegistry.getTargetContext();
        String deviceId = Settings.Secure.getString(
                ctx.getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);
        p = new ServerManagedPolicy(ctx.getApplicationContext(),
                new AESObfuscator(SALT, ctx.getPackageName(), deviceId));
    }

    /**
     * Verify that extra data is parsed correctly on a LICENSED response.
     */
    @Test
    public void extraDataParsed() {

        String sampleResponse = "0|1579380448|com.example.android.market.licensing|1|" +
                "ADf8I4ajjgc1P5ZI1S1DN/YIPIUNPECLrg==|1279578835423:VT=11&GT=22&GR=33";
        p.processServerResponse(Policy.LICENSED,
                ResponseData.parse(sampleResponse));
        assertEquals(11l, p.getValidityTimestamp());
        assertEquals(22l, p.getRetryUntil());
        assertEquals(33l, p.getMaxRetries());
    }

    /**
     * Verify that retry counts are cleared after getting a NOT_LICENSED response.
     */
    @Test
    public void retryCountsCleared() {
        String sampleResponse = "0|1579380448|com.example.android.market.licensing|1|" +
                "ADf8I4ajjgc1P5ZI1S1DN/YIPIUNPECLrg==|1279578835423:VT=1&GT=2&GR=3";
        // Sanity test
        p.processServerResponse(Policy.LICENSED,
                ResponseData.parse(sampleResponse));
        assertTrue(0l != p.getValidityTimestamp());
        assertTrue(0l != p.getRetryUntil());
        assertTrue(0l != p.getMaxRetries());

        // Actual test
        p.processServerResponse(Policy.NOT_LICENSED, null);
        assertEquals(0l, p.getValidityTimestamp());
        assertEquals(0l, p.getRetryUntil());
        assertEquals(0l, p.getMaxRetries());
    }

    /**
     * Verify that LU extra is parsed on NOT_LICENSED responses.
     */
    @Test
    public void licensingUrlExtraParsed() {
        String sampleResponse = "0|1579380448|com.example.android.market.licensing|1|" +
            "ADf8I4ajjgc1P5ZI1S1DN/YIPIUNPECLrg==|1279578835423:" +
            "LU=https%3A%2F%2Fplay.google.com%2Fstore%2Fapps%2Fdetails%3Fid%3Dcom.example.android.market.licensing";
        // Sanity test
        p.processServerResponse(Policy.LICENSED,
            ResponseData.parse(sampleResponse));
        assertNull(p.getLicensingUrl());

        // Actual test
        p.processServerResponse(Policy.NOT_LICENSED, ResponseData.parse(sampleResponse));
        assertEquals(
            "https://play.google.com/store/apps/details?id=com.example.android.market.licensing",
            p.getLicensingUrl());
    }

    /**
     * Verify that the policy can process null server responses.
     */
    @Test
    public void noFailureOnNullResponseData() {
        p.processServerResponse(Policy.RETRY, null);
        assertFalse(p.allowAccess());
    }

    @Test
    public void noFailureOnAdditionalEncodedExtras() {
        String sampleResponse = "0|1579380448|com.example.android.market.licensing|1|" +
                "ADf8I4ajjgc1P5ZI1S1DN/YIPIUNPECLrg==|1279578835423:VT=1&test=hello%20world%20%26" +
                "%20friends&GT=2&GR=3";
        p.processServerResponse(Policy.LICENSED,
                ResponseData.parse(sampleResponse));
        assertEquals(1l, p.getValidityTimestamp());
        assertEquals(2l, p.getRetryUntil());
        assertEquals(3l, p.getMaxRetries());
    }

}
