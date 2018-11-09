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
import com.google.android.vending.licensing.APKExpansionPolicy;
import com.google.android.vending.licensing.Policy;
import com.google.android.vending.licensing.ResponseData;

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
public class APKExpansionPolicyTest {

    private APKExpansionPolicy p;

    @Before
    public void initFixture() {
        final byte[] SALT = new byte[] {
            104, -12, 112, 82, -85, -10, -11, 61, 15, 54, 44, -66, -117, -89, -64, 110, -53, 123, 33
        };

        Context ctx = InstrumentationRegistry.getTargetContext();
        String deviceId = Settings.Secure.getString(
                ctx.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        p = new APKExpansionPolicy(ctx,
                new AESObfuscator(SALT, ctx.getPackageName(), deviceId));
    }

    /**
     * Verify that extra data is parsed correctly on a LICENSED resopnse..
     */
    @Test
    public void extraDataParsed() {
        // This is a sample server response from Google Play for an application that has both
        // a main and a patch APK Expansion file.  The response includes the URLs used to
        // download the files from Google Play, the names of the files to save, and the
        // sizes of each file.  In addition, this response also contains licensing data, including
        // information about the server-managed policy.
        String sampleResponse = "0|1579380448|com.example.android.market.licensing|1|" +
                "ADf8I4ajjgc1P5ZI1S1DN/YIPIUNPECLrg==|1279578835423:VT=11&GT=22&GR=33" +
                "&FILE_URL1=http://jmt17.google.com/vending_kila/download/AppDownload?packageName%3Dcom.example.android.market.licensing%26versionCode%3D3%26ft%3Do%26token%3DAOTCm0RwlzqFYylBNSCTLJApGH0cYtm9g8mGMdUhKLSLJW4v9VM8GLj4GVlGU5oyW6y3FsXrJiQqMunTGw9B" +
                "&FILE_NAME1=main.3.com.example.android.market.licensing.obb&FILE_SIZE1=687801613" +
                "&FILE_URL2=http://jmt17.google.com/vending_kila/download/AppDownload?packageName%3Dcom.example.android.market.licensing%26versionCode%3D3%26ft%3Do%26token%3DAOTCm0RwlzqFYylBNSCTLJApGH0cYtm9g8mGMdUhKLSLJW4v9VM8GLsdSDjefsdfEKdVaseEsfaMeifTek9B" +
                "&FILE_NAME2=patch.3.com.example.android.market.licensing.obb&FILE_SIZE2=204233";
        p.processServerResponse(Policy.LICENSED,
                ResponseData.parse(sampleResponse));
        assertEquals(11L, p.getValidityTimestamp());
        assertEquals(22L, p.getRetryUntil());
        assertEquals(33L, p.getMaxRetries());
        assertEquals(2, p.getExpansionURLCount());
        assertEquals("main.3.com.example.android.market.licensing.obb",p.getExpansionFileName(0));
        assertEquals(687801613L,p.getExpansionFileSize(0));
        assertEquals("http://jmt17.google.com/vending_kila/download/AppDownload?packageName=com.example.android.market.licensing&versionCode=3&ft=o&token=AOTCm0RwlzqFYylBNSCTLJApGH0cYtm9g8mGMdUhKLSLJW4v9VM8GLj4GVlGU5oyW6y3FsXrJiQqMunTGw9B",
                p.getExpansionURL(0));
        assertEquals("patch.3.com.example.android.market.licensing.obb",p.getExpansionFileName(1));
        assertEquals(204233,p.getExpansionFileSize(1));
        assertEquals("http://jmt17.google.com/vending_kila/download/AppDownload?packageName=com.example.android.market.licensing&versionCode=3&ft=o&token=AOTCm0RwlzqFYylBNSCTLJApGH0cYtm9g8mGMdUhKLSLJW4v9VM8GLsdSDjefsdfEKdVaseEsfaMeifTek9B",
                p.getExpansionURL(1));
    }

    /**
     * Verify that retry counts are cleared after getting a NOT_LICENSED response.
     */
    @Test
    public void retryCountsCleared() {
        String sampleResponse = "0|1579380448|com.example.android.market.licensing|1|" +
                "ADf8I4ajjgc1P5ZI1S1DN/YIPIUNPECLrg==|1279578835423:VT=1&GT=2&GR=3";
        p.processServerResponse(Policy.LICENSED,
                ResponseData.parse(sampleResponse));
        // Sanity test
        assertTrue(0L != p.getValidityTimestamp());
        assertTrue(0L != p.getRetryUntil());
        assertTrue(0L != p.getMaxRetries());

        // Actual test
        p.processServerResponse(Policy.NOT_LICENSED, ResponseData.parse(sampleResponse));
        assertEquals(0L, p.getValidityTimestamp());
        assertEquals(0L, p.getRetryUntil());
        assertEquals(0L, p.getMaxRetries());
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
        p.processServerResponse(Policy.LICENSED, ResponseData.parse(sampleResponse));
        assertNull(p.getLicensingUrl());

        // Actual test
        p.processServerResponse(Policy.NOT_LICENSED, ResponseData.parse(sampleResponse));
        assertEquals("https://play.google.com/store/apps/details?id=com.example.android.market.licensing",
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
        assertEquals(1L, p.getValidityTimestamp());
        assertEquals(2L, p.getRetryUntil());
        assertEquals(3L, p.getMaxRetries());
    }

}
