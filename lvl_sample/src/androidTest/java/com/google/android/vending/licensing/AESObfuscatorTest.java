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
import com.google.android.vending.licensing.ValidationException;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AESObfuscatorTest {
    private static final String TAG = "AESObfuscatorTest";
    private static final byte[] SALT = new byte[]{
            104, -12, 112, 82, -85, -10, -11, 61, 15, 54, 44, -66, -117, -89, -64, 110, -53, 123, 33
    };
    private static final String PACKAGE = "package";
    private static final String DEVICE = "device";

    private Obfuscator mObfuscator;

    @Before
    public void setUp() throws Exception {
        mObfuscator = new AESObfuscator(SALT, PACKAGE, DEVICE);
    }

    @Test
    public void obfuscateUnobfuscate() throws Exception {
        isInvertible(null);
        isInvertible("");
        isInvertible(
                "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ!@#$%^&*-=/\\|~`,.;:"
                        + "()[]{}<>\u00F6");
    }

    @Test
    public void unobfuscateInvalid() throws Exception {
        try {
            mObfuscator.unobfuscate("invalid", "testKey");
            fail("Should have thrown ValidationException");
        } catch (ValidationException expected) {
        }
    }

    @Test
    public void unobfuscateDifferentSalt() throws Exception {
        String obfuscated = mObfuscator.obfuscate("test", "testKey");
        Obfuscator differentSalt = new AESObfuscator(new byte[]{1}, PACKAGE, DEVICE);
        try {
            differentSalt.unobfuscate(obfuscated, "testKey");
            fail("Should have thrown ValidationException");
        } catch (ValidationException expected) {
        }
    }

    @Test
    public void unobfuscateAvoidBadPaddingException() throws Exception {
        // Length should be equal to the cipher block size, to make sure that all padding lengths
        // are accounted for.
        for (int length = 0; length < 255; length++) {
            char[] data = new char[length];
            Arrays.fill(data, '0');
            String input = String.valueOf(data);
            Log.d(TAG, "Input: (" + length + ")" + input);
            String obfuscated = mObfuscator.obfuscate(input, "testKey");
            Obfuscator differentSalt = new AESObfuscator(new byte[]{1}, PACKAGE, DEVICE);
            try {
                differentSalt.unobfuscate(obfuscated, "testKey");
                fail("Should have thrown ValidationException");
            } catch (ValidationException expected) {
            }
        }
    }

    @Test
    public void unobfuscateDifferentDevice() throws Exception {
        String obfuscated = mObfuscator.obfuscate("test", "testKey");
        Obfuscator differentDevice = new AESObfuscator(SALT, PACKAGE, "device2");
        try {
            differentDevice.unobfuscate(obfuscated, "testKey");
            fail("Should have thrown ValidationException");
        } catch (ValidationException expected) {
        }
    }

    @Test
    public void unobfuscateDifferentPackage() throws Exception {
        String obfuscated = mObfuscator.obfuscate("test", "testKey");
        Obfuscator differentPackage = new AESObfuscator(SALT, "package2", DEVICE);
        try {
            differentPackage.unobfuscate(obfuscated, "testKey");
            fail("Should have thrown ValidationException");
        } catch (ValidationException expected) {
        }
    }

    @Test
    public void unobfuscateDifferentKey() throws Exception {
        String obfuscated = mObfuscator.obfuscate("test", "testKey");
        Obfuscator differentPackage = new AESObfuscator(SALT, "package2", DEVICE);
        try {
            differentPackage.unobfuscate(obfuscated, "notMyKey");
            fail("Should have thrown ValidationException");
        } catch (ValidationException expected) {
        }
    }

    @Test
    public void obfuscateSame() throws Exception {
        String obfuscated = mObfuscator.obfuscate("test", "testKey");
        assertEquals(obfuscated, mObfuscator.obfuscate("test", "testKey"));

        Obfuscator same = new AESObfuscator(SALT, PACKAGE, DEVICE);
        assertEquals(obfuscated, same.obfuscate("test", "testKey"));
        assertEquals("test", same.unobfuscate(obfuscated, "testKey"));
    }

    private void isInvertible(String original) throws Exception {
        assertEquals(original,
                mObfuscator.unobfuscate(mObfuscator.obfuscate(original, original + "Key"),
                        original + "Key"));
    }
}
