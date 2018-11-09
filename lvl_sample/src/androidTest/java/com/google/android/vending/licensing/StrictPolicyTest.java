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

import android.support.test.runner.AndroidJUnit4;

import com.google.android.vending.licensing.Policy;
import com.google.android.vending.licensing.StrictPolicy;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test suite for StrictPolicy.
 */
@RunWith(AndroidJUnit4.class)
public class StrictPolicyTest {

    /**
     * Verify that initial response is to deny access.
     */
    @Test
    public void initialResponse() {
        StrictPolicy p = new StrictPolicy();
        boolean result = p.allowAccess();
        assertFalse(result);
    }

    /**
     * Verify that after receiving a LICENSED response, the policy grants
     * access.
     */
    @Test
    public void licensedResponse()  {
        StrictPolicy p = new StrictPolicy();
        p.processServerResponse(Policy.LICENSED, null);
        boolean result = p.allowAccess();
        assertTrue(result);
    }

    /**
     * Verify that after receiving a NOT_LICENSED response, the policy denies
     * access and extracts the licensing URL from the response.
     */
    @Test
    public void notLicensedResponse() {
        StrictPolicy p = new StrictPolicy();

        String sampleResponse = "0|1579380448|com.example.android.market.licensing|1|" +
            "ADf8I4ajjgc1P5ZI1S1DN/YIPIUNPECLrg==|1279578835423:" +
            "LU=https%3A%2F%2Fplay.google.com%2Fstore%2Fapps%2Fdetails%3Fid%3Dcom.example.android.market.licensing";
        p.processServerResponse(Policy.NOT_LICENSED, ResponseData.parse(sampleResponse));
        boolean result = p.allowAccess();
        assertFalse(result);
        assertEquals(
            "https://play.google.com/store/apps/details?id=com.example.android.market.licensing",
            p.getLicensingUrl());
    }

    /**
     * Verify that after receiving a RETRY response, the policy denies
     * access.
     */
    @Test
    public void retryResponse() {
        StrictPolicy p = new StrictPolicy();
        p.processServerResponse(Policy.RETRY, null);
        boolean result = p.allowAccess();
        assertFalse(result);
    }

}
