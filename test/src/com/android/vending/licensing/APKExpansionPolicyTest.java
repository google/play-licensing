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

package com.android.vending.licensing;


import com.example.android.market.licensing.MainActivity;
import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.APKExpansionPolicy;
import com.google.android.vending.licensing.Policy;
import com.google.android.vending.licensing.ResponseData;

import android.provider.Settings;
import android.test.ActivityInstrumentationTestCase2;

/**
 * Test suite for StrictPolicy.
 */
public class APKExpansionPolicyTest extends ActivityInstrumentationTestCase2<MainActivity> {

    APKExpansionPolicy p;

    public APKExpansionPolicyTest() {
        super("com.example.android.market.licensing", MainActivity.class);
    }

    public void setUp() {
        final byte[] SALT = new byte[] {
            104, -12, 112, 82, -85, -10, -11, 61, 15, 54, 44, -66, -117, -89, -64, 110, -53, 123, 33
        };

        String deviceId = Settings.Secure.getString(
                getActivity().getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);
        p = new APKExpansionPolicy(getActivity().getApplicationContext(),
                new AESObfuscator(SALT, getActivity().getPackageName(), deviceId));
    }

    /**
     * Verify that extra data is parsed correctly on a LICENSED resopnse..
     */
    public void testExtraDataParsed() {
        String sampleResponse = "0|1579380448|com.example.android.market.licensing|1|" +
                "ADf8I4ajjgc1P5ZI1S1DN/YIPIUNPECLrg==|1279578835423:VT=11&GT=22&GR=33" + 
                "&FILE_URL1=http://jmt17.google.com/vending_kila/download/AppDownload?packageName%3Dcom.example.android.market.licensing%26versionCode%3D3%26ft%3Do%26token%3DAOTCm0RwlzqFYylBNSCTLJApGH0cYtm9g8mGMdUhKLSLJW4v9VM8GLj4GVlGU5oyW6y3FsXrJiQqMunTGw9B" +
                "&FILE_NAME1=main.3.com.example.android.market.licensing.obb&FILE_SIZE1=687801613" +
                "&FILE_URL2=http://jmt17.google.com/vending_kila/download/AppDownload?packageName%3Dcom.example.android.market.licensing%26versionCode%3D3%26ft%3Do%26token%3DAOTCm0RwlzqFYylBNSCTLJApGH0cYtm9g8mGMdUhKLSLJW4v9VM8GLsdSDjefsdfEKdVaseEsfaMeifTek9B" +
                "&FILE_NAME2=patch.3.com.example.android.market.licensing.obb&FILE_SIZE2=204233";
        p.processServerResponse(Policy.LICENSED,
                ResponseData.parse(sampleResponse));
        assertEquals(11l, p.getValidityTimestamp());
        assertEquals(22l, p.getRetryUntil());
        assertEquals(33l, p.getMaxRetries());
        assertEquals(2, p.getExpansionURLCount());
        assertEquals("main.3.com.example.android.market.licensing.obb",p.getExpansionFileName(0));
        assertEquals(687801613l,p.getExpansionFileSize(0));
        assertEquals("http://jmt17.google.com/vending_kila/download/AppDownload?packageName%3Dcom.example.android.market.licensing%26versionCode%3D3%26ft%3Do%26token%3DAOTCm0RwlzqFYylBNSCTLJApGH0cYtm9g8mGMdUhKLSLJW4v9VM8GLj4GVlGU5oyW6y3FsXrJiQqMunTGw9B",
                    p.getExpansionURL(0));
        assertEquals("patch.3.com.example.android.market.licensing.obb",p.getExpansionFileName(1));
        assertEquals(204233,p.getExpansionFileSize(1));
        assertEquals("http://jmt17.google.com/vending_kila/download/AppDownload?packageName%3Dcom.example.android.market.licensing%26versionCode%3D3%26ft%3Do%26token%3DAOTCm0RwlzqFYylBNSCTLJApGH0cYtm9g8mGMdUhKLSLJW4v9VM8GLsdSDjefsdfEKdVaseEsfaMeifTek9B",
                p.getExpansionURL(1));
    }
    
    /**
     * Verify that retry counts are cleared after getting a NOT_LICENSED response.
     */
    public void testRetryCountsCleared() {
        String sampleResponse = "0|1579380448|com.example.android.market.licensing|1|" +
                "ADf8I4ajjgc1P5ZI1S1DN/YIPIUNPECLrg==|1279578835423:VT=1&GT=2&GR=3";
        p.processServerResponse(Policy.LICENSED,
                ResponseData.parse(sampleResponse));
        // Sanity test
        assertTrue(0l != p.getValidityTimestamp());
        assertTrue(0l != p.getRetryUntil());
        assertTrue(0l != p.getMaxRetries());

        // Actual test
        p.processServerResponse(Policy.NOT_LICENSED, null);
        assertEquals(0l, p.getValidityTimestamp());
        assertEquals(0l, p.getRetryUntil());
        assertEquals(0l, p.getMaxRetries());
    }

    public void testNoFailureOnEncodedExtras() {
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
