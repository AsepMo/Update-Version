package com.app.evolution.application.updater;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import com.app.evolution.application.updater.utils.WebStream;

/**
 * Checks for app updates.
 */
public class UpdateChecker {

	private URL		latestApkUrl = null;
	private float	latestApkVersion = 0;

	private static String UPDATES_URL = "https://api.github.com/repos/ram-on/SkyTube/releases/latest";
	private static String TAG = UpdateChecker.class.getSimpleName();
    private String VERSION_NAME = "1.0";

	/**
	 * Check for app updates.  If an update is available, {@link this#latestApkUrl} and {@link this#latestApkVersion}
	 * will be set.
	 *
	 * @return True if if an update is available;  false otherwise.
	 */
	public boolean checkForUpdates() {
		boolean updatesAvailable = false;

		try {
			WebStream		webStream = new WebStream(UPDATES_URL);
			BufferedReader	bufferedReader = new BufferedReader(new InputStreamReader(webStream.getStream()));
			JSONObject		json = getJSON(bufferedReader);
			float			remoteVersionNumber  = getLatestVersionNumber(json);
			float			currentVersionNumber = getCurrentVerNumber();

			Log.d(TAG, "CURRENT_VER: " + currentVersionNumber);
			Log.d(TAG, "REMOTE_VER: " + remoteVersionNumber);

			if (currentVersionNumber < remoteVersionNumber) {
				this.latestApkUrl = getLatestApkUrl(json);
				this.latestApkVersion = remoteVersionNumber;
				updatesAvailable = true;
				Log.d(TAG, "Update available.  APK_URL: " + latestApkUrl);
			} else {
				Log.d(TAG, "Same version - not updating.");
			}
		} catch (Throwable e) {
			Log.e(TAG, "An error has occurred while checking for updates", e);
		}

		return updatesAvailable;
	}


	public URL getLatestApkUrl() {
		return latestApkUrl;
	}

	public float getLatestApkVersion() {
		return latestApkVersion;
	}


	/**
	 * Extracts the data from bufferedReader and uses that data to create a {@link JSONObject}.
	 *
	 * @param bufferedReader	HTTP Connection
	 * @return {@link JSONObject}
	 * @throws Exception
	 */
	private JSONObject getJSON(BufferedReader bufferedReader) throws Exception {
		StringBuilder	builder = new StringBuilder();
		String			line;

		while ((line = bufferedReader.readLine()) != null) {
			builder.append(line);
			builder.append("\n");
		}

		return new JSONObject(builder.toString());
	}


	/**
	 * Extracts from json the latest APP's version.
	 *
	 * @param json
	 * @return
	 * @throws Exception
	 */
	private float getLatestVersionNumber(JSONObject json) throws Exception {
		String  versionNumberStr = json.getString("tag_name").substring(1);  // tag_name = "v2.0" --> so we are going to delete the 'v' character
		return Float.parseFloat(versionNumberStr);
	}


	/**
	 * Extracts from json the APK's URL of the latest version.
	 *
	 * @param json
	 * @return
	 * @throws Exception
	 */
	private URL getLatestApkUrl(JSONObject json) throws Exception {
		String apkUrl = json.getJSONArray("assets").getJSONObject(0).getString("browser_download_url");
		return new URL(apkUrl);
	}

	/**
	 * @return The current app's version number.
	 */
	private float getCurrentVerNumber() {
		return Float.parseFloat(VERSION_NAME);
	}

}
