cd ../sample
ant install
cd ../test
ant install
adb shell am instrument -w com.example.android.market.licensing.test/android.test.InstrumentationTestRunner
