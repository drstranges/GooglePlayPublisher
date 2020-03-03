/*
 *  Copyright Roman Donchenko. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.drextended.gppublisher;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.*;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.AndroidPublisherScopes;
import com.google.api.services.androidpublisher.model.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper class to initialize the publisher APIs client library.
 * <p>
 * Before making any calls to the API through the client library you need to
 * call the {@link #init()} method.
 * This will run all precondition checks.
 * </p>
 */
public class AndroidPublishRequestWrapper {

    private static final Log logger = LogFactory.getLog(AndroidPublishRequestWrapper.class);

    static final String MIME_TYPE_APK = "application/vnd.android.package-archive";
    static final String MIME_TYPE_OCTET_STREAM = "application/octet-stream";
    public static final String TRACK_NONE = "none";
    public static final String TRACK_INTERNAL = "internal";
    public static final String TRACK_ALPHA = "alpha";
    public static final String TRACK_BETA = "beta";
    public static final String TRACK_PRODUCTION = "production";
    public static final String TRACK_ROLLOUT = "rollout";
    public static final String TRACK_CUSTOM = "custom";

    private final File mWorkingDirectory;
    private final String mApplicationName;
    private final String mPackageName;
    private final boolean mFindJsonKeyInFile;
    private final String mJsonKeyPath;
    private final String mJsonKeyContent;
    private final String mApkPath;
    private final String mDeobfuscationFilePath;
    private final String mRecentChangesListings;
    private final String mTrack;
    private final String mRolloutFractionString;
    private String mTrackCustomNames;

    private AndroidPublisher mAndroidPublisher;
    private File mApkFile;
    private File mDeobfuscationFile;
    private List<LocalizedText> mReleaseNotes;
    private Double mRolloutFraction;
    private String[] mCustomTracks;

    /**
     * @param workingDirectory
     * @param applicationName       The name of your application. If the application name is
     *                              {@code null} or blank, the application will log a warning. Suggested
     *                              format is "MyCompany-Application/1.0".
     * @param packageName           the package name of the app
     * @param findJsonKeyInFile
     * @param jsonKeyPath           the service account secret json file path
     * @param apkPath               the apk/aab file path of the apk/aab to upload
     * @param deobfuscationFilePath the deobfuscation file of the specified APK/AAB
     * @param recentChangesListings the recent changes in format: [BCP47 Language Code]:[recent changes file path].
     *                              Multiple listing thought comma. Sample: en-US:C:\temp\listing_en.txt
     * @param track                 The track for uploading the apk, can be 'internal', 'alpha', beta', 'production' or 'rollout'
     * @param rolloutFraction       The rollout fraction
     * @param trackCustomNames      Comma separated track names for `custom` track
     */
    public AndroidPublishRequestWrapper(
            File workingDirectory,
            String applicationName,
            String packageName,
            boolean findJsonKeyInFile,
            String jsonKeyPath,
            String jsonKeyContent,
            String apkPath,
            String deobfuscationFilePath,
            String recentChangesListings,
            String track,
            String rolloutFraction,
            String trackCustomNames
    ) {
        mWorkingDirectory = workingDirectory;
        mApplicationName = applicationName;
        mPackageName = packageName;
        mFindJsonKeyInFile = findJsonKeyInFile;
        mJsonKeyPath = jsonKeyPath;
        mJsonKeyContent = jsonKeyContent;
        mApkPath = apkPath;
        mDeobfuscationFilePath = deobfuscationFilePath;
        mRecentChangesListings = recentChangesListings;
        mTrack = track;
        mRolloutFractionString = rolloutFraction;
        mTrackCustomNames = trackCustomNames;
    }

    /**
     * Performs all necessary setup steps for running requests against the API.
     *
     * @throws GeneralSecurityException
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public void init() throws IOException, GeneralSecurityException, IllegalArgumentException {
        logger.info("Initializing...");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(mApplicationName), "Application name cannot be null or empty!");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(mPackageName), "Package name cannot be null or empty!");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(mTrack), "Track cannot be null or empty!");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(mApkPath), "Apk/aab path cannot be null or empty!");

        if (TRACK_ROLLOUT.equals(mTrack)) {
            try {
                mRolloutFraction = Double.parseDouble(mRolloutFractionString);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("User fraction cannot be parsed as double: " + mRolloutFractionString);
            }
            if (mRolloutFraction < 0 || mRolloutFraction >= 1) {
                throw new IllegalArgumentException("User fraction must be in range (0 <= fraction < 1): " + mRolloutFractionString);
            }
        } else if (TRACK_CUSTOM.equals(mTrack)) {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(mTrackCustomNames), "Not specified names for custom tracks!");
            mCustomTracks = mTrackCustomNames.split(",\\s*");
        }

        String apkFullPath = relativeToFullPath(mApkPath);
        mApkFile = new File(apkFullPath);
        Preconditions.checkArgument(mApkFile.exists(), "Apk file not found in path: " + apkFullPath);
        if (!Strings.isNullOrEmpty(mDeobfuscationFilePath)) {
            String deobfuscationFullPath = relativeToFullPath(mDeobfuscationFilePath);
            mDeobfuscationFile = new File(deobfuscationFullPath);
            Preconditions.checkArgument(mDeobfuscationFile.exists(), "Mapping (deobfuscation) file not found in path: " + deobfuscationFullPath);
        }

        final InputStream jsonKeyInputStream;
        if (mFindJsonKeyInFile) {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(mJsonKeyPath), "Secret json key path cannot be null or empty!");
            String jsonKeyFullPath = relativeToFullPath(mJsonKeyPath);
            File jsonKeyFile = new File(jsonKeyFullPath);
            Preconditions.checkArgument(jsonKeyFile.exists(), "Secret json key file not found in path: " + jsonKeyFullPath);
            jsonKeyInputStream = new FileInputStream(jsonKeyFile);
        } else {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(mJsonKeyContent), "Secret json key content cannot be null or empty!");
            jsonKeyInputStream = IOUtils.toInputStream(mJsonKeyContent, StandardCharsets.UTF_8);
        }

        if (!Strings.isNullOrEmpty(mRecentChangesListings)) {
            String[] rcParts = mRecentChangesListings.trim().split("\\s*,\\s*");
            mReleaseNotes = new ArrayList<LocalizedText>(rcParts.length);
            for (String rcPart : rcParts) {
                String[] rcPieces = rcPart.split("\\s*::\\s*");

                Preconditions.checkArgument(rcPieces.length == 2, "Wrong recent changes entry: " + rcPart);

                String languageCode = rcPieces[0];
                String recentChangesFilePath = relativeToFullPath(rcPieces[1]);
                Preconditions.checkArgument(!Strings.isNullOrEmpty(languageCode) && !Strings.isNullOrEmpty(recentChangesFilePath),
                        "Wrong recent changes entry: " + rcPart + ", lang = " + languageCode + ", path = " + recentChangesFilePath);

                File rcFile = new File(recentChangesFilePath);
                Preconditions.checkArgument(rcFile.exists(),
                        "Recent changes file for language \"" + languageCode + "\" not found in path: " + recentChangesFilePath);

                FileInputStream inputStream = new FileInputStream(rcFile);
                String recentChanges = null;
                try {
                    recentChanges = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                } finally {
                    inputStream.close();
                }

                mReleaseNotes.add(
                        new LocalizedText().setLanguage(languageCode).setText(recentChanges)
                );
            }
        }
        logger.info("Initialized successfully!");

        logger.info("Creating AndroidPublisher Api Service...");
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = GoogleCredential.fromStream(jsonKeyInputStream, httpTransport, jsonFactory)
                .createScoped(Collections.singleton(AndroidPublisherScopes.ANDROIDPUBLISHER));
        mAndroidPublisher = new AndroidPublisher.Builder(httpTransport, jsonFactory, new RequestInitializer(credential))
                .setApplicationName(mApplicationName)
                .build();
        logger.info("AndroidPublisher Api Service created!");
    }

    private String relativeToFullPath(String path) {
        if (path != null && !new File(path).isAbsolute()) {
            return new File(mWorkingDirectory, path).getAbsolutePath();
        }
        return path;
    }

    /**
     * Publishes apk file on Google Play
     *
     * @throws IOException
     * @throws GeneralSecurityException
     * @throws IllegalArgumentException
     */
    public void makeInsertRequest() throws IOException, GeneralSecurityException, IllegalArgumentException {
        Preconditions.checkArgument(mApkFile != null && mApkFile.exists(), "Apk file not found in path: " + mApkPath);

        logger.info("Creating a new edit session...");
        final AndroidPublisher.Edits edits = mAndroidPublisher.edits();
        AndroidPublisher.Edits.Insert editRequest = edits.insert(mPackageName, null);
        AppEdit edit = editRequest.execute();
        final String editId = edit.getId();
        logger.info(String.format("Created edit session with id: %s", editId));

        Integer apkVersionCode;

        if (mApkPath.endsWith(".apk")) {
            logger.info("Uploading new apk file...");
            final AbstractInputStreamContent apkFile = new FileContent(AndroidPublishRequestWrapper.MIME_TYPE_APK, mApkFile);
            Apk apk = edits.apks()
                    .upload(mPackageName, editId, apkFile)
                    .execute();
            apkVersionCode = apk.getVersionCode();
            logger.info(String.format("Apk file with version code %s has been uploaded!", apkVersionCode));
        } else if (mApkPath.endsWith(".aab")) {
            logger.info("Uploading new aab file...");
            final AbstractInputStreamContent aabFile = new FileContent(AndroidPublishRequestWrapper.MIME_TYPE_OCTET_STREAM, mApkFile);
            Bundle bundle = edits.bundles()
                    .upload(mPackageName, editId, aabFile)
                    .execute();
            apkVersionCode = bundle.getVersionCode();
            logger.info(String.format("App Bundle with version code %s has been uploaded!", apkVersionCode));
        } else {
            logger.info("File [" + mApkPath + "] is not apk nor aab file!");
            throw new IllegalArgumentException("File [" + mApkPath + "] is not apk nor aab file!");
        }

        if (mDeobfuscationFile != null) {
            logger.info("Uploading new mapping file...");
            Preconditions.checkArgument(mDeobfuscationFile.exists(), "Mapping (deobfuscation) file not found in path: " + mDeobfuscationFilePath);
            final AbstractInputStreamContent deobfuscationFile = new FileContent(AndroidPublishRequestWrapper.MIME_TYPE_OCTET_STREAM, mDeobfuscationFile);
            edits.deobfuscationfiles()
                    .upload(mPackageName, editId, apkVersionCode, "proguard", deobfuscationFile)
                    .execute();
            logger.info("Mapping has been uploaded!");
        }

        if (TRACK_NONE.equals(mTrack)) {
            logger.info("Track was not set, so apk will not be assigned to any track...");
        } else if (TRACK_CUSTOM.equals(mTrack)) {
            for (String customTrack : mCustomTracks) {
                assignToTrack(edits, editId, apkVersionCode, customTrack);
            }
        } else {
            assignToTrack(edits, editId, apkVersionCode, mTrack);
        }
        logger.info("Committing changes for edit...");
        AppEdit appEdit = edits.commit(mPackageName, editId)
                .execute();
        logger.info(String.format("App edit with id %s has been committed!", appEdit.getId()));
        logger.info("=\n\n==================\n\n PUBLISHED SUCCESSFUL \n\n==================\n\n");
    }

    private void assignToTrack(AndroidPublisher.Edits edits, String editId, Integer apkVersionCode, String trackName) throws IOException {
        logger.info("Assigning release to the track: " + trackName);

        TrackRelease release = new TrackRelease()
                .setVersionCodes(Collections.singletonList(Long.valueOf(apkVersionCode)))
                .setReleaseNotes(mReleaseNotes);

        if (TRACK_ROLLOUT.equals(trackName)) {
            release = release
                    .setUserFraction(mRolloutFraction)
                    .setStatus("inProgress");
        } else {
            release = release.setStatus("completed");
        }

        Track trackContent = new Track()
                .setTrack(trackName)
                .setReleases(Collections.singletonList(release));

        edits.tracks()
                .update(mPackageName, editId, trackName, trackContent)
                .execute();

        logger.info("Release successfully assigning to the track: " + trackName);
    }

    public static class RequestInitializer implements HttpRequestInitializer {

        private final HttpRequestInitializer initializer;

        public RequestInitializer(HttpRequestInitializer credential) {
            this.initializer = credential;
        }

        public void initialize(HttpRequest httpRequest) throws IOException {
            initializer.initialize(httpRequest);
            httpRequest.setConnectTimeout(3 * 60000);  // 3 minutes connect timeout
            httpRequest.setReadTimeout(3 * 60000);  // 3 minutes read timeout
        }
    }

}
