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
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import java.io.File;

import at.mroland.android.mifareclassictool.R;

public class LaunchSettingsActivity extends AppCompatPreferenceActivity {

    private static final int RESULT_OUTPUT_PATH = 100;
    private static final int RESULT_INPUT_PATH = 101;
    private static final int RESULT_INPUT2_PATH = 102;

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference)preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                        ? listPreference.getEntries()[index]
                        : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        updatePreference(preference);
    }


    private static void updatePreference(Preference preference) {
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                                                                 PreferenceManager
                                                                         .getDefaultSharedPreferences(preference.getContext())
                                                                         .getString(preference.getKey(), ""));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                finish();
                //NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onIsMultiPane() {
        return false;
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return MfocPreferenceFragment.class.getName().equals(fragmentName) ||
               MfcukPreferenceFragment.class.getName().equals(fragmentName) ||
                WriteClonePreferenceFragment.class.getName().equals(fragmentName);
    }

    public static class MfocPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_launch_mfoc);

            bindPreferenceSummaryToValue(findPreference("mfoc_output_file_path"));
            bindPreferenceSummaryToValue(findPreference("mfoc_output_file_name"));
            bindPreferenceSummaryToValue(findPreference("known_keys"));
            bindPreferenceSummaryToValue(findPreference("mfoc_probes"));
            bindPreferenceSummaryToValue(findPreference("mfoc_tolerance"));

            findPreference("mfoc_output_file_path").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent filePicker = new Intent(getActivity(), FilePickerActivity.class);
                    filePicker.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
                    filePicker.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
                    filePicker.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE_AND_DIR | FilePickerActivity.MODE_WRITABLE);
                    File extFilesDir = getActivity().getExternalFilesDir(null);
                    if (extFilesDir != null) {
                        filePicker.putExtra(FilePickerActivity.EXTRA_BASE_PATHS, new String[]{
                                extFilesDir.getAbsolutePath(),
                                getActivity().getFilesDir().getAbsolutePath(),
                                Environment.getExternalStorageDirectory().getAbsolutePath(),
                                });
                    } else {
                        filePicker.putExtra(FilePickerActivity.EXTRA_BASE_PATHS, new String[]{
                                getActivity().getFilesDir().getAbsolutePath(),
                                Environment.getExternalStorageDirectory().getAbsolutePath(),
                                });
                    }
                    String filePath = preference.getSharedPreferences().getString(preference.getKey(), null);
                    if ((filePath == null) || filePath.isEmpty()) {
                        if (extFilesDir != null) {
                            filePath = extFilesDir.getAbsolutePath();
                        } else {
                            filePath = getActivity().getFilesDir().getAbsolutePath();
                        }
                    }
                    filePicker.putExtra(FilePickerActivity.EXTRA_START_PATH, filePath);
                    startActivityForResult(filePicker, RESULT_OUTPUT_PATH);
                    return true;
                }
            });

            findPreference("launch_mfoc").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent mfoc = NavUtils.getParentActivityIntent(getActivity());
                    mfoc.putExtra(MainActivity.EXTRA_LAUNCH_MODE, MainActivity.LAUNCH_MODE_MFOC);
                    NavUtils.navigateUpTo(getActivity(), mfoc);
                    return true;
                }
            });
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
                case RESULT_OUTPUT_PATH:
                    if (resultCode == FilePickerActivity.RESULT_OK) {
                        Preference filePath = findPreference("mfoc_output_file_path");
                        Preference fileName = findPreference("mfoc_output_file_name");
                        File file = new File(data.getData().getPath());
                        if (file.isDirectory()) {
                            if (filePath != null) {
                                filePath.getSharedPreferences().edit().putString(filePath.getKey(), file.getAbsolutePath()).apply();
                                updatePreference(filePath);
                            }
                        } else {
                            if (filePath != null) {
                                filePath.getSharedPreferences().edit().putString(filePath.getKey(), file.getParentFile().getAbsolutePath()).apply();
                                updatePreference(filePath);
                            }
                            if (fileName != null) {
                                fileName.getSharedPreferences().edit().putString(fileName.getKey(), file.getName()).apply();
                                updatePreference(fileName);
                            }
                        }
                    }
                    break;

                default:
                    super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    public static class MfcukPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_launch_mfcuk);

            bindPreferenceSummaryToValue(findPreference("mfcuk_output_file_path"));
            bindPreferenceSummaryToValue(findPreference("mfcuk_output_file_name"));
            bindPreferenceSummaryToValue(findPreference("mfcuk_recover_sector"));
            bindPreferenceSummaryToValue(findPreference("mfcuk_recover_keytype"));
            bindPreferenceSummaryToValue(findPreference("mfcuk_sleep_field_off"));
            bindPreferenceSummaryToValue(findPreference("mfcuk_sleep_field_on"));

            findPreference("mfcuk_output_file_path").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent filePicker = new Intent(getActivity(), FilePickerActivity.class);
                    filePicker.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
                    filePicker.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
                    filePicker.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE_AND_DIR | FilePickerActivity.MODE_WRITABLE);
                    File extFilesDir = getActivity().getExternalFilesDir(null);
                    if (extFilesDir != null) {
                        filePicker.putExtra(FilePickerActivity.EXTRA_BASE_PATHS, new String[]{
                                extFilesDir.getAbsolutePath(),
                                getActivity().getFilesDir().getAbsolutePath(),
                                Environment.getExternalStorageDirectory().getAbsolutePath(),
                                });
                    } else {
                        filePicker.putExtra(FilePickerActivity.EXTRA_BASE_PATHS, new String[]{
                                getActivity().getFilesDir().getAbsolutePath(),
                                Environment.getExternalStorageDirectory().getAbsolutePath(),
                                });
                    }
                    String filePath = preference.getSharedPreferences().getString(preference.getKey(), null);
                    if ((filePath == null) || filePath.isEmpty()) {
                        if (extFilesDir != null) {
                            filePath = extFilesDir.getAbsolutePath();
                        } else {
                            filePath = getActivity().getFilesDir().getAbsolutePath();
                        }
                    }
                    filePicker.putExtra(FilePickerActivity.EXTRA_START_PATH, filePath);
                    startActivityForResult(filePicker, RESULT_OUTPUT_PATH);
                    return true;
                }
            });

            findPreference("launch_mfcuk").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent mfcuk = NavUtils.getParentActivityIntent(getActivity());
                    mfcuk.putExtra(MainActivity.EXTRA_LAUNCH_MODE, MainActivity.LAUNCH_MODE_MFCUK);
                    NavUtils.navigateUpTo(getActivity(), mfcuk);
                    return true;
                }
            });
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
                case RESULT_OUTPUT_PATH:
                    if (resultCode == FilePickerActivity.RESULT_OK) {
                        Preference filePath = findPreference("mfcuk_output_file_path");
                        Preference fileName = findPreference("mfcuk_output_file_name");
                        File file = new File(data.getData().getPath());
                        if (file.isDirectory()) {
                            if (filePath != null) {
                                filePath.getSharedPreferences().edit().putString(filePath.getKey(), file.getAbsolutePath()).apply();
                                updatePreference(filePath);
                            }
                        } else {
                            if (filePath != null) {
                                filePath.getSharedPreferences().edit().putString(filePath.getKey(), file.getParentFile().getAbsolutePath()).apply();
                                updatePreference(filePath);
                            }
                            if (fileName != null) {
                                fileName.getSharedPreferences().edit().putString(fileName.getKey(), file.getName()).apply();
                                updatePreference(fileName);
                            }
                        }
                    }
                    break;

                default:
                    super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    public static class WriteClonePreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_launch_write_clone);

            bindPreferenceSummaryToValue(findPreference("clone_input_file_path"));
            bindPreferenceSummaryToValue(findPreference("clone_input_file_name"));

            findPreference("clone_input_file_path").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent filePicker = new Intent(getActivity(), FilePickerActivity.class);
                    filePicker.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
                    filePicker.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
                    filePicker.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE | FilePickerActivity.MODE_WRITABLE);
                    File extFilesDir = getActivity().getExternalFilesDir(null);
                    if (extFilesDir != null) {
                        filePicker.putExtra(FilePickerActivity.EXTRA_BASE_PATHS, new String[]{
                                extFilesDir.getAbsolutePath(),
                                getActivity().getFilesDir().getAbsolutePath(),
                                Environment.getExternalStorageDirectory().getAbsolutePath(),
                                });
                    } else {
                        filePicker.putExtra(FilePickerActivity.EXTRA_BASE_PATHS, new String[]{
                                getActivity().getFilesDir().getAbsolutePath(),
                                Environment.getExternalStorageDirectory().getAbsolutePath(),
                                });
                    }
                    String filePath = preference.getSharedPreferences().getString(preference.getKey(), null);
                    if ((filePath == null) || filePath.isEmpty()) {
                        if (extFilesDir != null) {
                            filePath = extFilesDir.getAbsolutePath();
                        } else {
                            filePath = getActivity().getFilesDir().getAbsolutePath();
                        }
                    }
                    filePicker.putExtra(FilePickerActivity.EXTRA_START_PATH, filePath);
                    startActivityForResult(filePicker, RESULT_INPUT_PATH);
                    return true;
                }
            });
            findPreference("clone_input_file_name").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Preference pathPref = findPreference("clone_input_file_path");
                    return (pathPref != null) &&
                           pathPref.getOnPreferenceClickListener().onPreferenceClick(pathPref);

                }
            });

            findPreference("key_input_file_path").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent filePicker = new Intent(getActivity(), FilePickerActivity.class);
                    filePicker.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
                    filePicker.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
                    filePicker.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE | FilePickerActivity.MODE_WRITABLE);
                    File extFilesDir = getActivity().getExternalFilesDir(null);
                    if (extFilesDir != null) {
                        filePicker.putExtra(FilePickerActivity.EXTRA_BASE_PATHS, new String[]{
                                extFilesDir.getAbsolutePath(),
                                getActivity().getFilesDir().getAbsolutePath(),
                                Environment.getExternalStorageDirectory().getAbsolutePath(),
                                });
                    } else {
                        filePicker.putExtra(FilePickerActivity.EXTRA_BASE_PATHS, new String[]{
                                getActivity().getFilesDir().getAbsolutePath(),
                                Environment.getExternalStorageDirectory().getAbsolutePath(),
                                });
                    }
                    String filePath = preference.getSharedPreferences().getString(preference.getKey(), null);
                    if ((filePath == null) || filePath.isEmpty()) {
                        if (extFilesDir != null) {
                            filePath = extFilesDir.getAbsolutePath();
                        } else {
                            filePath = getActivity().getFilesDir().getAbsolutePath();
                        }
                    }
                    filePicker.putExtra(FilePickerActivity.EXTRA_START_PATH, filePath);
                    startActivityForResult(filePicker, RESULT_INPUT2_PATH);
                    return true;
                }
            });
            findPreference("key_input_file_name").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Preference pathPref = findPreference("key_input_file_path");
                    return (pathPref != null) &&
                           pathPref.getOnPreferenceClickListener().onPreferenceClick(pathPref);

                }
            });

            findPreference("launch_write_clone").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent writeClone = NavUtils.getParentActivityIntent(getActivity());
                    writeClone.putExtra(MainActivity.EXTRA_LAUNCH_MODE, MainActivity.LAUNCH_MODE_WRITE_CLONE);
                    NavUtils.navigateUpTo(getActivity(), writeClone);
                    return true;
                }
            });
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
                case RESULT_INPUT_PATH:
                    if (resultCode == FilePickerActivity.RESULT_OK) {
                        Preference filePath = findPreference("clone_input_file_path");
                        Preference fileName = findPreference("clone_input_file_name");
                        File file = new File(data.getData().getPath());
                        if (file.isDirectory()) {
                            if (filePath != null) {
                                filePath.getSharedPreferences().edit().putString(filePath.getKey(), file.getAbsolutePath()).apply();
                                updatePreference(filePath);
                            }
                        } else {
                            if (filePath != null) {
                                filePath.getSharedPreferences().edit().putString(filePath.getKey(), file.getParentFile().getAbsolutePath()).apply();
                                updatePreference(filePath);
                            }
                            if (fileName != null) {
                                fileName.getSharedPreferences().edit().putString(fileName.getKey(), file.getName()).apply();
                                updatePreference(fileName);
                            }
                        }
                    }
                    break;

                case RESULT_INPUT2_PATH:
                    if (resultCode == FilePickerActivity.RESULT_OK) {
                        Preference filePath = findPreference("key_input_file_path");
                        Preference fileName = findPreference("key_input_file_name");
                        File file = new File(data.getData().getPath());
                        if (file.isDirectory()) {
                            if (filePath != null) {
                                filePath.getSharedPreferences().edit().putString(filePath.getKey(), file.getAbsolutePath()).apply();
                                updatePreference(filePath);
                            }
                        } else {
                            if (filePath != null) {
                                filePath.getSharedPreferences().edit().putString(filePath.getKey(), file.getParentFile().getAbsolutePath()).apply();
                                updatePreference(filePath);
                            }
                            if (fileName != null) {
                                fileName.getSharedPreferences().edit().putString(fileName.getKey(), file.getName()).apply();
                                updatePreference(fileName);
                            }
                        }
                    }
                    break;

                default:
                    super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }
}
