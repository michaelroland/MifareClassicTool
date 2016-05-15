/*
** Copyright (C) 2016  Michael Roland <mi.roland@gmail.com>
**
** This program is free software: you can redistribute it and/or modify
** it under the terms of the GNU General Public License as published by
** the Free Software Foundation, either version 3 of the License, or
** (at your option) any later version.
**
** This program is distributed in the hope that it will be useful,
** but WITHOUT ANY WARRANTY; without even the implied warranty of
** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
** GNU General Public License for more details.
**
** You should have received a copy of the GNU General Public License
** along with this program.  If not, see <http://www.gnu.org/licenses/>.
**
*/

package at.mroland.android.mifareclassictool.activities;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import at.mroland.android.mifareclassictool.R;
import at.mroland.utils.FileUtils;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    public static final String EXTRA_LAUNCH_MODE = MainActivity.class.getName() + ".LAUNCH_MODE";
    public static final String LAUNCH_MODE_MFOC = "mfoc";
    public static final String LAUNCH_MODE_MFCUK = "mfcuk";
    public static final String LAUNCH_MODE_WRITE_CLONE = "write_clone";

    private static final Object sCurrentProcessSync = new Object();
    private static Process sCurrentProcess = null;
    private static boolean sCommandRunning = false;
    private static Window sConsoleWnd;
    private static TextView sConsoleOutput;
    private static HorizontalScrollView sConsoleScrollHorizontal;
    private static NestedScrollView sConsoleScrollVertical;
    private int mConsoleTextColorDefault;
    private int mConsoleTextColorWarn;
    private int mConsoleTextColorNotice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setIcon(R.mipmap.ic_launcher);
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        sConsoleWnd = getWindow();
        sConsoleOutput = (TextView)findViewById(R.id.consoleOutput);
        sConsoleScrollHorizontal = (HorizontalScrollView)findViewById(R.id.consoleScrollHorizontal);
        sConsoleScrollVertical = (NestedScrollView)findViewById(R.id.consoleScrollVertical);
        mConsoleTextColorDefault = ContextCompat.getColor(this, R.color.colorConsoleTextDefault);
        mConsoleTextColorWarn = ContextCompat.getColor(this, R.color.colorConsoleTextWarning);
        mConsoleTextColorNotice = ContextCompat.getColor(this, R.color.colorConsoleTextNotice);

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sCommandRunning) {
            sConsoleWnd.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        Intent intent = getIntent();
        if (intent != null) {
            if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {
                String launchMode = intent.getStringExtra(EXTRA_LAUNCH_MODE);
                intent.removeExtra(EXTRA_LAUNCH_MODE);

                if (LAUNCH_MODE_MFOC.equals(launchMode)) {
                    runMfoc();
                } else if (LAUNCH_MODE_MFCUK.equals(launchMode)) {
                    runMfcuk();
                } else if (LAUNCH_MODE_WRITE_CLONE.equals(launchMode)) {
                    runWriteClone();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.setGroupEnabled(R.id.menugroup_command_actions, sCommandRunning);
        menu.setGroupEnabled(R.id.menugroup_commands, !sCommandRunning);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //case R.id.menu_stop_command:
            //    runCommand(null, null);
            //    return true;

            case R.id.menu_mfoc:
            {
                Intent launchPrefs = new Intent(this, LaunchSettingsActivity.class);
                launchPrefs.putExtra(LaunchSettingsActivity.EXTRA_SHOW_FRAGMENT, LaunchSettingsActivity.MfocPreferenceFragment.class.getName());
                startActivity(launchPrefs);
                return true;
            }

            case R.id.menu_mfcuk:
            {
                Intent launchPrefs = new Intent(this, LaunchSettingsActivity.class);
                launchPrefs.putExtra(LaunchSettingsActivity.EXTRA_SHOW_FRAGMENT, LaunchSettingsActivity.MfcukPreferenceFragment.class.getName());
                startActivity(launchPrefs);
                return true;
            }

            case R.id.menu_nfc_mfclassic_write:
            {
                Intent launchPrefs = new Intent(this, LaunchSettingsActivity.class);
                launchPrefs.putExtra(LaunchSettingsActivity.EXTRA_SHOW_FRAGMENT, LaunchSettingsActivity.WriteClonePreferenceFragment.class.getName());
                startActivity(launchPrefs);
                return true;
            }

            case R.id.menu_nfc_list:
                runCommand("nfc-list", null);
                return true;

            case R.id.menu_hex_editor:
            {
                Intent launchHexEditor = new Intent(this, EditorActivity.class);
                startActivity(launchHexEditor);
                return true;
            }

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private Process spawnProcess(final String executable, final String... args) {
        Process p = null;

        ArrayList<String> command = new ArrayList<>();
        command.add("su");
        command.add("-c");
        StringBuilder suCommand = new StringBuilder();
        suCommand.append("LD_LIBRARY_PATH=").append(getApplicationInfo().nativeLibraryDir).append(" ");
        suCommand.append(getApplicationInfo().nativeLibraryDir).append("/libex-").append(executable).append(".exe.so");
        if (args != null) {
            for (String arg : args) {
                suCommand.append(" ").append(arg);
            }
        }
        command.add(suCommand.toString());
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.environment().put("LD_LIBRARY_PATH", getApplicationInfo().nativeLibraryDir);
            p = builder.start();
        } catch (IOException e) {
            Log.e(TAG, "spawnProcess() failed: IOException while executing \"" + executable + "\"", e);
        }

        return p;
    }

    private void runCommand(final String command, final IProcessExitCallback exitCallback, final String... args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                setCommandRunning();

                synchronized (sCurrentProcessSync) {
                    if (sCurrentProcess != null) {
                        Log.d(TAG, "runCommand(): Forcing process to stop");
                        sCurrentProcess.destroy();
                        try {
                            sCurrentProcess.waitFor();
                        } catch (InterruptedException ex) {}
                        sCurrentProcess = null;
                    }
                }

                if (command == null) {
                    clearCommandRunning();
                    return;
                }

                setCommandRunning();
                consoleClear();

                StringBuilder argString = new StringBuilder();
                if (args != null) {
                    for (String arg : args) {
                        argString.append(" ").append(arg);
                    }
                }
                Log.d(TAG, "runCommand(): Starting process " + command + argString.toString());
                consoleAppendText(mConsoleTextColorNotice, "Starting process " + command + argString.toString() + "\n");
                final Process p = spawnProcess(command, args);
                synchronized (sCurrentProcessSync) {
                    sCurrentProcess = p;
                }

                if (p == null) {
                    Log.e(TAG, "runCommand(): Failed to spawn process");
                    consoleAppendText(mConsoleTextColorWarn, "Failed to start process\n");
                    clearCommandRunning();
                } else {
                    final BufferedReader stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    final BufferedReader stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));


                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String line;
                                while ((line = stderr.readLine()) != null) {
                                    Log.d(TAG, "runCommand() STDERR: " + line);
                                    consoleAppendText(mConsoleTextColorWarn, line + "\n");
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "runCommand() failed with IOException while reading STDERR", e);
                            } finally {
                                try {
                                    stderr.close();
                                } catch (Exception ex) {
                                }
                            }
                        }
                    }).start();

                    try {
                        String line;
                        while ((line = stdout.readLine()) != null) {
                            Log.d(TAG, "runCommand() STDOUT: " + line);
                            consoleAppendText(mConsoleTextColorDefault, line + "\n");
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "runCommand() failed with IOException while reading STDOUT", e);
                    } finally {
                        try {
                            stdout.close();
                        } catch (Exception ex) {
                        }
                    }

                    try {
                        int res = p.waitFor();
                        Log.d(TAG, "runCommand(): Process ended");
                        consoleAppendText(mConsoleTextColorNotice, "Process " + command + " ended (exit code: " + res + ")\n");
                        synchronized (sCurrentProcessSync) {
                            sCurrentProcess = null;
                        }
                        if (exitCallback != null) {
                            exitCallback.onProcessExit(res);
                        }
                    } catch (InterruptedException ex) {
                    } finally {
                        clearCommandRunning();
                    }
                }
            }
        }).start();
    }

    public void runMfoc() {
        try {
            final File mfocTempFile = File.createTempFile("temp-mfoc", ".mfd", getFilesDir());

            ArrayList<String> parameters = new ArrayList<>();

            String optProbes = PreferenceManager.getDefaultSharedPreferences(this).getString("mfoc_probes", getString(R.string.pref_mfoc_probes_default));
            if (!optProbes.isEmpty()) {
                parameters.add("-P");
                parameters.add(optProbes);
            }

            String optTolerance = PreferenceManager.getDefaultSharedPreferences(this).getString("mfoc_tolerance", getString(R.string.pref_mfoc_tolerance_default));
            if (!optTolerance.isEmpty()) {
                parameters.add("-T");
                parameters.add(optTolerance);
            }

            String[] knownKeys = PreferenceManager.getDefaultSharedPreferences(this).getString("known_keys", "").split("[\\r\\n,;]+");
            for (String knownKey : knownKeys) {
                knownKey = knownKey.replace("_", "").replaceAll("[^\\W]", "").trim();
                if (knownKey.matches("^[a-fA-f0-9]{16}$")) {
                    parameters.add("-k");
                    parameters.add(knownKey);
                }
            }

            parameters.add("-O");
            parameters.add(mfocTempFile.getAbsolutePath());

            runCommand("mfoc", new IProcessExitCallback() {
                @Override
                public void onProcessExit(int exitCode) {
                    if (exitCode == 0) {
                        File defaultOutputPath = getExternalFilesDir(null);
                        String defaultOutputPathString;
                        if (defaultOutputPath != null) {
                            defaultOutputPathString = defaultOutputPath.getAbsolutePath();
                        } else {
                            defaultOutputPathString = mfocTempFile.getParentFile().getAbsolutePath();
                        }
                        File outputFile = new File(
                                PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString(
                                        "mfoc_output_file_path",
                                        defaultOutputPathString),
                                PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString(
                                        "mfoc_output_file_name",
                                        mfocTempFile.getName().replace("temp-", "")));
                        try {
                            FileUtils.moveFile(mfocTempFile, outputFile);
                        } catch (Exception e) {
                            consoleAppendText(mConsoleTextColorWarn, "Could not move temporary file " + mfocTempFile.getAbsolutePath() + " to output file at " + outputFile.getAbsolutePath());
                            Log.e(TAG, "Could not move temporary file " + mfocTempFile.getAbsolutePath() + " to external storage at " + outputFile.getAbsolutePath());
                        }
                    } else {
                        try {
                            if (!mfocTempFile.delete()) {
                                throw new Exception();
                            }
                        } catch (Exception e) {
                            consoleAppendText(mConsoleTextColorWarn, "Could not remove temporary file " + mfocTempFile.getAbsolutePath());
                            Log.e(TAG, "Could not remove temporary file " + mfocTempFile.getAbsolutePath());
                        }
                    }
                }
            }, parameters.toArray(new String[0]));
        } catch (IOException e) {
            consoleAppendText(mConsoleTextColorWarn, "Failed to create temporary file");
            Log.e(TAG, "Failed to create temporary file", e);
        }
    }

    public void runMfcuk() {
        try {
            final File mfcukTempFile = File.createTempFile("temp-mfcuk", ".mfd", getFilesDir());

            ArrayList<String> parameters = new ArrayList<>();

            parameters.add("-C");

            parameters.add("-R");
            String recoverSector = PreferenceManager.getDefaultSharedPreferences(this).getString("mfcuk_recover_sector", getString(R.string.pref_mfcuk_recover_sector_default));
            String recoverKeytype = PreferenceManager.getDefaultSharedPreferences(this).getString("mfcuk_recover_keytype", getString(R.string.pref_mfcuk_recover_keytype_default));
            String recoverOption = "";
            if (!recoverSector.isEmpty()) {
                recoverOption += recoverSector;
            } else {
                recoverOption += getString(R.string.pref_mfcuk_recover_sector_default);
            }
            if (!recoverKeytype.isEmpty()) {
                recoverOption += ":" + recoverKeytype;
            } else {
                recoverOption += ":" + getString(R.string.pref_mfcuk_recover_keytype_default);
            }
            parameters.add(recoverOption);

            String optFieldOff = PreferenceManager.getDefaultSharedPreferences(this).getString("mfcuk_sleep_field_off", null);
            if ((optFieldOff != null) && !optFieldOff.isEmpty()) {
                parameters.add("-s");
                parameters.add(optFieldOff);
            }

            String optFieldOn = PreferenceManager.getDefaultSharedPreferences(this).getString("mfcuk_sleep_field_on", null);
            if ((optFieldOn != null) && !optFieldOn.isEmpty()) {
                parameters.add("-S");
                parameters.add(optFieldOn);
            }

//            String[] knownKeys = PreferenceManager.getDefaultSharedPreferences(this).getString("known_keys", "").split("[\\r\\n,;]+");
//            if (knownKeys != null) {
//                for (String knownKey : knownKeys) {
//            knownKey = knownKey.replace("_", "").replaceAll("[^\\W]", "").trim();
//                    if (knownKey.matches("^[a-fA-f0-9]{16}$")) {
//                        parameters.add("-d");
//                        parameters.add(knownKey);
//                    }
//                }
//            }

            parameters.add("-O");
            parameters.add(mfcukTempFile.getAbsolutePath());

            runCommand("mfcuk", new IProcessExitCallback() {
                @Override
                public void onProcessExit(int exitCode) {
                    if (exitCode == 0) {
                        File defaultOutputPath = getExternalFilesDir(null);
                        String defaultOutputPathString;
                        if (defaultOutputPath != null) {
                            defaultOutputPathString = defaultOutputPath.getAbsolutePath();
                        } else {
                            defaultOutputPathString = mfcukTempFile.getParentFile().getAbsolutePath();
                        }
                        File outputFile = new File(
                                PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString(
                                        "mfcuk_output_file_path",
                                        defaultOutputPathString),
                                PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString(
                                        "mfcuk_output_file_name",
                                        mfcukTempFile.getName().replace("temp-", "")));
                        try {
                            FileUtils.moveFile(mfcukTempFile, outputFile);
                        } catch (Exception e) {
                            consoleAppendText(mConsoleTextColorWarn, "Could not move temporary file " + mfcukTempFile.getAbsolutePath() + " to output file at " + outputFile.getAbsolutePath());
                            Log.e(TAG, "Could not move temporary file " + mfcukTempFile.getAbsolutePath() + " to external storage at " + outputFile.getAbsolutePath());
                        }
                    } else {
                        try {
                            if (!mfcukTempFile.delete()) {
                                throw new Exception();
                            }
                        } catch (Exception e) {
                            consoleAppendText(mConsoleTextColorWarn, "Could not remove temporary file " + mfcukTempFile.getAbsolutePath());
                            Log.e(TAG, "Could not remove temporary file " + mfcukTempFile.getAbsolutePath());
                        }
                    }
                }
            }, parameters.toArray(new String[0]));
        } catch (IOException e) {
            consoleAppendText(mConsoleTextColorWarn, "Failed to create temporary file");
            Log.e(TAG, "Failed to create temporary file", e);
        }
    }

    public void runWriteClone() {
        try {
            File defaultInputPath = getExternalFilesDir(null);
            String defaultInputPathString;
            if (defaultInputPath != null) {
                defaultInputPathString = defaultInputPath.getAbsolutePath();
            } else {
                defaultInputPathString = getFilesDir().getAbsolutePath();
            }
            File inputFile = new File(
                    PreferenceManager.getDefaultSharedPreferences(this).getString("clone_input_file_path", defaultInputPathString),
                    PreferenceManager.getDefaultSharedPreferences(this).getString("clone_input_file_name", ""));
            if (inputFile.exists() && inputFile.isFile()) {
                final File inputFileTemp = File.createTempFile("temp-dump", ".mfd", getFilesDir());
                FileUtils.copyFile(inputFile, inputFileTemp);

                final File keyFileTemp;

                ArrayList<String> parameters = new ArrayList<>();

                if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("clone_target_chinese", false)) {
                    parameters.add("w");
                    parameters.add(PreferenceManager.getDefaultSharedPreferences(this).getString("write_clone_keytype", getString(R.string.pref_write_clone_keytype_default)).toUpperCase());
                    parameters.add(inputFileTemp.getAbsolutePath());

                    File keyFile = new File(
                            PreferenceManager.getDefaultSharedPreferences(this).getString(
                                    "key_input_file_path",
                                    PreferenceManager.getDefaultSharedPreferences(this).getString(
                                            "clone_input_file_path",
                                            defaultInputPathString)),
                            PreferenceManager.getDefaultSharedPreferences(this).getString(
                                    "key_input_file_name",
                                    PreferenceManager.getDefaultSharedPreferences(this).getString(
                                            "clone_input_file_name",
                                            "")));
                    keyFileTemp = File.createTempFile("temp-keys", ".mfd", getFilesDir());
                    FileUtils.copyFile(keyFile, keyFileTemp);

                    parameters.add(keyFileTemp.getAbsolutePath());
                    parameters.add("f");
                } else {
                    parameters.add("W");
                    parameters.add("X");
                    parameters.add(inputFileTemp.getAbsolutePath());
                    keyFileTemp = null;
                }

                runCommand("nfc-mfclassic", new IProcessExitCallback() {
                    @Override
                    public void onProcessExit(int exitCode) {
                        try {
                            if (!inputFileTemp.delete()) {
                                throw new Exception();
                            }
                        } catch (Exception e) {
                            consoleAppendText(mConsoleTextColorWarn, "Could not remove temporary file " + inputFileTemp.getAbsolutePath());
                            Log.e(TAG, "Could not remove temporary file " + inputFileTemp.getAbsolutePath());
                        }
                        if (keyFileTemp != null) {
                            try {
                                if (!keyFileTemp.delete()) {
                                    throw new Exception();
                                }
                            } catch (Exception e) {
                                consoleAppendText(mConsoleTextColorWarn, "Could not remove temporary file " + keyFileTemp.getAbsolutePath());
                                Log.e(TAG, "Could not remove temporary file " + keyFileTemp.getAbsolutePath());
                            }
                        }
                    }
                }, parameters.toArray(new String[0]));
            }
        } catch (IOException e) {
            consoleAppendText(mConsoleTextColorWarn, "Failed to create temporary file");
            Log.e(TAG, "Failed to create temporary file", e);
        }
    }

    private void consoleClear() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sConsoleOutput.setText("");
                //sConsoleScrollHorizontal.fullScroll(sConsoleOutput.getLayoutDirection() == View.LAYOUT_DIRECTION_LTR ? View.FOCUS_LEFT : View.FOCUS_RIGHT);
                //sConsoleScrollVertical.fullScroll(View.FOCUS_UP);
                sConsoleScrollHorizontal.scrollTo(0, 0);
                sConsoleScrollVertical.scrollTo(0, 0);
            }
        });
    }

    private void consoleSetText(final int color, final CharSequence text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CharSequence formattedText = text;
                if (color != sConsoleOutput.getCurrentTextColor()) {
                    Spannable spannableText;
                    if (text instanceof Spannable) {
                        spannableText = (Spannable)text;
                    } else {
                        spannableText = Spannable.Factory.getInstance().newSpannable(text);
                    }
                    spannableText.setSpan(new ForegroundColorSpan(color), 0, spannableText.length(), 0);
                    formattedText = spannableText;
                }
                sConsoleOutput.setText(formattedText);
                //sConsoleScrollHorizontal.fullScroll(sConsoleOutput.getLayoutDirection() == View.LAYOUT_DIRECTION_LTR ? View.FOCUS_LEFT : View.FOCUS_RIGHT);
                //sConsoleScrollVertical.fullScroll(View.FOCUS_UP);
                sConsoleScrollHorizontal.scrollTo(0, 0);
                sConsoleScrollVertical.scrollTo(0, 0);
            }
        });
    }

    private void consoleAppendText(final int color, final CharSequence text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CharSequence formattedText = text;
                if (color != sConsoleOutput.getCurrentTextColor()) {
                    Spannable spannableText;
                    if (text instanceof Spannable) {
                        spannableText = (Spannable)text;
                    } else {
                        spannableText = Spannable.Factory.getInstance().newSpannable(text);
                    }
                    spannableText.setSpan(new ForegroundColorSpan(color), 0, spannableText.length(), 0);
                    formattedText = spannableText;
                }
                sConsoleOutput.append(formattedText);
                //sConsoleScrollHorizontal.fullScroll(sConsoleOutput.getLayoutDirection() == View.LAYOUT_DIRECTION_LTR ? View.FOCUS_LEFT : View.FOCUS_RIGHT);
                //sConsoleScrollVertical.fullScroll(View.FOCUS_DOWN);
                sConsoleScrollHorizontal.scrollTo(0, sConsoleScrollHorizontal.getBottom());
                sConsoleScrollVertical.scrollTo(0, sConsoleScrollVertical.getBottom());
            }
        });
    }

    private void setCommandRunning() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sCommandRunning = true;
                sConsoleWnd.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                invalidateOptionsMenu();
            }
        });
    }

    private void clearCommandRunning() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sCommandRunning = false;
                sConsoleWnd.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                invalidateOptionsMenu();
            }
        });
    }

    private interface IProcessExitCallback {
        void onProcessExit(int exitCode);
    }
}
