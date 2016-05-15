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
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;

import at.mroland.android.mifareclassictool.R;

public class EditorActivity extends AppCompatActivity {

    private static final String TAG = EditorActivity.class.getSimpleName();

    private static final int RESULT_OPEN_FILE = 100;
    private static final int RESULT_SAVE_FILE = 101;
    private static final String PREF_LAST_PATH = "editor_last_path";

    private File mCurrentFile;
    private EditText mEditText;
    private HorizontalScrollView mConsoleScrollHorizontal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mEditText = (EditText)findViewById(R.id.editText);
        mConsoleScrollHorizontal = (HorizontalScrollView)findViewById(R.id.editorScrollHorizontal);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.editor, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_save).setEnabled(mCurrentFile != null);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                //NavUtils.navigateUpFromSameTask(this);
                return true;

            case R.id.menu_open: {
                Intent filePicker = new Intent(this, FilePickerActivity.class);
                filePicker.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
                filePicker.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
                filePicker.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE | FilePickerActivity.MODE_WRITABLE);
                File extFilesDir = getExternalFilesDir(null);
                if (extFilesDir != null) {
                    filePicker.putExtra(FilePickerActivity.EXTRA_BASE_PATHS, new String[]{
                            extFilesDir.getAbsolutePath(),
                            getFilesDir().getAbsolutePath(),
                            Environment.getExternalStorageDirectory().getAbsolutePath(),
                            });
                    filePicker.putExtra(FilePickerActivity.EXTRA_START_PATH,
                                        getPreferences(MODE_PRIVATE).getString(PREF_LAST_PATH,
                                                                               extFilesDir.getAbsolutePath()));
                } else {
                    filePicker.putExtra(FilePickerActivity.EXTRA_BASE_PATHS, new String[]{
                            getFilesDir().getAbsolutePath(),
                            Environment.getExternalStorageDirectory().getAbsolutePath(),
                            });
                    filePicker.putExtra(FilePickerActivity.EXTRA_START_PATH,
                                        getPreferences(MODE_PRIVATE).getString(PREF_LAST_PATH,
                                                                               getFilesDir().getAbsolutePath()));
                }
                startActivityForResult(filePicker, RESULT_OPEN_FILE);
                return true;
            }

            case R.id.menu_save:
                if (mCurrentFile != null) {
                    saveFile(mCurrentFile);
                }
                return true;

            case R.id.menu_save_as: {
                Intent filePicker = new Intent(this, FilePickerActivity.class);
                filePicker.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
                filePicker.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
                filePicker.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE | FilePickerActivity.MODE_WRITABLE);
                File extFilesDir = getExternalFilesDir(null);
                if (extFilesDir != null) {
                    filePicker.putExtra(FilePickerActivity.EXTRA_BASE_PATHS, new String[]{
                            extFilesDir.getAbsolutePath(),
                            getFilesDir().getAbsolutePath(),
                            Environment.getExternalStorageDirectory().getAbsolutePath(),
                            });
                    filePicker.putExtra(FilePickerActivity.EXTRA_START_PATH,
                                        getPreferences(MODE_PRIVATE).getString(PREF_LAST_PATH,
                                                                               extFilesDir.getAbsolutePath()));
                } else {
                    filePicker.putExtra(FilePickerActivity.EXTRA_BASE_PATHS, new String[]{
                            getFilesDir().getAbsolutePath(),
                            Environment.getExternalStorageDirectory().getAbsolutePath(),
                            });
                    filePicker.putExtra(FilePickerActivity.EXTRA_START_PATH,
                                        getPreferences(MODE_PRIVATE).getString(PREF_LAST_PATH,
                                                                               getFilesDir().getAbsolutePath()));
                }
                startActivityForResult(filePicker, RESULT_SAVE_FILE);
                return true;
            }

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESULT_OPEN_FILE:
                if (resultCode == FilePickerActivity.RESULT_OK) {
                    mCurrentFile = new File(data.getData().getPath());
                    getPreferences(MODE_PRIVATE).edit().putString(PREF_LAST_PATH, mCurrentFile.getAbsolutePath()).apply();
                    loadFile(mCurrentFile);
                    invalidateOptionsMenu();
                }
                break;

            case RESULT_SAVE_FILE:
                if (resultCode == FilePickerActivity.RESULT_OK) {
                    mCurrentFile = new File(data.getData().getPath());
                    getPreferences(MODE_PRIVATE).edit().putString(PREF_LAST_PATH, mCurrentFile.getAbsolutePath()).apply();
                    saveFile(mCurrentFile);
                    invalidateOptionsMenu();
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    private void loadFile(File src) {
        mEditText.setText("");

        InputStream in = null;
        try {
            in = new FileInputStream(src);

            int lineLen = 0;
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < len; ++i) {
                    byte b = buf[i];
                    int upper = (b >>> 4) & 0x00f;
                    int lower = b & 0x00f;
                    sb.append(HEX_DIGITS[upper]).append(HEX_DIGITS[lower]).append(" ");
                    ++lineLen;
                    if (lineLen == 16) {
                        lineLen = 0;
                        sb.append("\n");
                    }
                }
                mEditText.append(sb);
            }
            Toast.makeText(this, getString(R.string.toast_open_complete, src.getAbsolutePath()), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "Failed to open file " + src.getAbsolutePath(), e);
            Toast.makeText(this, getString(R.string.toast_open_failed, src.getAbsolutePath()), Toast.LENGTH_SHORT).show();
        } finally {
            try {
                in.close();
            } catch (Exception e) {}
        }

        mConsoleScrollHorizontal.fullScroll(mEditText.getLayoutDirection() == View.LAYOUT_DIRECTION_LTR ? View.FOCUS_LEFT : View.FOCUS_RIGHT);
    }

    private void saveFile(File dst) {
        OutputStream out = null;
        try {
            out = new FileOutputStream(dst);

            StringReader rd = new StringReader(mEditText.getText().toString());
            int ch;
            boolean lowerNibble = false;
            int upper = 0;
            while ((ch = rd.read()) != -1) {
                int nibble = -1;
                if ((ch >= '0') && (ch <= '9')) {
                    nibble = ch - '0';
                } else if ((ch >= 'a') && (ch <= 'f')) {
                    nibble = ch - 'a' + 10;
                } else if ((ch >= 'A') && (ch <= 'F')) {
                    nibble = ch - 'A' + 10;
                }
                if (nibble != -1) {
                    if (lowerNibble) {
                        lowerNibble = false;
                        out.write(((upper << 4) & 0x0f0) | (nibble & 0x00f));
                    } else {
                        upper = nibble;
                        lowerNibble = true;
                    }
                } else {
                    lowerNibble = false;
                }
            }

            Toast.makeText(this, getString(R.string.toast_save_complete, dst.getAbsolutePath()), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to file " + dst.getAbsolutePath(), e);
            Toast.makeText(this, getString(R.string.toast_save_failed, dst.getAbsolutePath()), Toast.LENGTH_SHORT).show();
        } finally {
            try {
                out.flush();
            } catch (Exception e) {}
            try {
                out.close();
            } catch (Exception e) {}
        }
    }
}
