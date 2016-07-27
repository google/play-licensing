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
import com.google.android.vending.licensing.Obfuscator;
import com.google.android.vending.licensing.PreferenceObfuscator;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;


/**
 * Test suite for PreferenceObfuscator.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ObfuscatedPreferencesTest {

    private static final String filename =
            "com.android.vending.licnese.test.ObfuscatedPreferencePopulatedTest";
    private SharedPreferences sp;
    private PreferenceObfuscator op;

    @Before
    public void initFixture() {
        final byte[] SALT = new byte[] {
            104, -12, 112, 82, -85, -10, -11, 61, 15, 54, 44, -66, -117, -89, -64, 110, -53, 123, 33
        };

        // Prepare PreferenceObfuscator instance
        Context ctx = InstrumentationRegistry.getTargetContext();
        sp = ctx.getSharedPreferences(filename, Context.MODE_PRIVATE);
        String deviceId = Settings.Secure.getString(
                ctx.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        Obfuscator o = new AESObfuscator(SALT, ctx.getPackageName(), deviceId);
        op = new PreferenceObfuscator(sp, o);

        // Populate with test data
        op.putString("testString", "Hello world");
        op.commit();
    }

    @After
    public void cleanup() {
        // Manually clear out any saved preferences
        SharedPreferences.Editor spe = sp.edit();
        spe.clear();
        spe.commit();
    }

    @Test
    public void getString() {
        assertEquals("Hello world", op.getString("testString", "fail"));
    }

    @Test
    public void getDefaultString() {
        assertEquals("Android rocks", op.getString("noExist", "Android rocks"));
    }

    @Test
    public void getDefaultNullString() {
        assertEquals(null, op.getString("noExist", null));
    }

    @Test
    public void corruptDataRetunsDefaultString() {
        // Insert non-obfuscated string
        SharedPreferences.Editor spe = sp.edit();
        spe.putString("corruptData", "foo");
        spe.commit();

        // Read back contents
        assertEquals("Android rocks", op.getString("corruptdata", "Android rocks"));
    }

}
