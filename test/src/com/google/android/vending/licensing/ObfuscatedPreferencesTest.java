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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.test.AndroidTestCase;

/**
 * Test suite for PreferenceObfuscator.
 */
public class ObfuscatedPreferencesTest extends AndroidTestCase {

    private static final String filename =
            "com.android.vending.licnese.test.ObfuscatedPreferencePopulatedTest";
    private SharedPreferences sp;
    private PreferenceObfuscator op;

    @Override
    public void setUp() {
        final byte[] SALT = new byte[] {
            104, -12, 112, 82, -85, -10, -11, 61, 15, 54, 44, -66, -117, -89, -64, 110, -53, 123, 33
        };

        // Prepare PreferenceObfuscator instance
        sp = getContext().getSharedPreferences(filename, Context.MODE_PRIVATE);
        String deviceId = Settings.Secure.getString(
                getContext().getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);
        Obfuscator o = new AESObfuscator(SALT, getContext().getPackageName(), deviceId);
        op = new PreferenceObfuscator(sp, o);

        // Populate with test data
        op.putString("testString", "Hello world");
        op.commit();
    }

    public void cleanup() {
        // Manually clear out any saved preferences
        SharedPreferences.Editor spe = sp.edit();
        spe.clear();
        spe.commit();
    }

    public void testGetString() {
        assertEquals("Hello world", op.getString("testString", "fail"));
    }

    public void testGetDefaultString() {
        assertEquals("Android rocks", op.getString("noExist", "Android rocks"));
    }

    public void testGetDefaultNullString() {
        assertEquals(null, op.getString("noExist", null));
    }

    public void testCorruptDataRetunsDefaultString() {
        // Insert non-obfuscated string
        SharedPreferences.Editor spe = sp.edit();
        spe.putString("corruptData", "foo");
        spe.commit();

        // Read back contents
        assertEquals("Android rocks", op.getString("corruptdata", "Android rocks"));
    }

}
