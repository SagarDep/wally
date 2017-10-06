/*
 * Copyright (C) 2014 Freddie (Musenkishi) Lust-Hed
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

package com.musenkishi.wally.base;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.musenkishi.wally.BuildConfig;
import com.musenkishi.wally.dataprovider.DataProvider;
import com.musenkishi.wally.dataprovider.CustomDataProvider;
import com.musenkishi.wally.dataprovider.FilterProvider;
import com.musenkishi.wally.dataprovider.ImageDownloadManager;
import com.musenkishi.wally.dataprovider.SharedPreferencesDataProvider;
import com.musenkishi.wally.dataprovider.okhttp.OkHttpUrlLoader;
import com.musenkishi.wally.fragments.SearchFragment;
import com.musenkishi.wally.models.ExceptionReporter;
import com.musenkishi.wally.models.filters.FilterAspectRatioKeys;
import com.musenkishi.wally.models.filters.FilterBoardsKeys;
import com.musenkishi.wally.models.filters.FilterGroupsStructure;
import com.musenkishi.wally.models.filters.FilterPurityKeys;
import com.musenkishi.wally.models.filters.FilterResOptKeys;
import com.musenkishi.wally.models.filters.FilterResolutionKeys;
import com.musenkishi.wally.models.filters.FilterTimeSpanKeys;
import com.squareup.okhttp.OkHttpClient;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Musenkishi on 2014-07-25.
 */
public class WallyApplication extends Application {

    private static final int FAMILIAR_USER_COUNT = 3;

    private static boolean SHOULD_SHOW_CRASH_LOGGING_PERMISSION = false;
    private static DataProvider dataProvider;
    private static CustomDataProvider customDataProvider;
    private static ImageDownloadManager imageDownloadManager;
    private static FilterProvider filterProvider;
    private static Context applicationContext;
    private static HashMap<Long, String> pairedDownloadIds;
    private static HashSet<Long> downloadIDs;
    private static HashMap<String, Object> searchFragmentMessages;

    private static Bitmap bitmapThumb;

    @Override
    public void onCreate(){
        super.onCreate();
        Glide.get(this).register(GlideUrl.class, InputStream.class,
                new OkHttpUrlLoader.Factory(new OkHttpClient()));
        applicationContext = getApplicationContext();
        startCrashLoggingIfUserAccepted();
        checkVersionInstalled(BuildConfig.VERSION_CODE);
    }

    private void checkVersionInstalled(int currentVersion) {
        int defaultVersion = 1;

        int latestVersion = getFilterProvider()
                .getSharedPreferencesDataProviderInstance()
                .getLatestVersion(defaultVersion); //This is just to get a default version.

        if (latestVersion < currentVersion) {
            // The user comes from an app version where they might have
            // Wallbase filter settings that conflicts with Wallhaven's filters.
            // That's why the user's settings gets reset.
            if (latestVersion == defaultVersion) {
                getFilterProvider()
                        .getSharedPreferencesDataProviderInstance()
                        .getPrefs()
                        .edit()
                        .clear()
                        .apply();
            }
        }
        getFilterProvider()
                .getSharedPreferencesDataProviderInstance()
                .setLatestVersion(BuildConfig.VERSION_CODE);
    }

    public static Context getContext(){
        return applicationContext;
    }

    public static boolean shouldShowCrashLoggingPermission(){
        return SHOULD_SHOW_CRASH_LOGGING_PERMISSION;
    }

    public static void setShouldShowCrashLoggingPermission(boolean shouldShow){
        SHOULD_SHOW_CRASH_LOGGING_PERMISSION = shouldShow;
    }

    private void startCrashLoggingIfUserAccepted() {
        if (getFilterProvider().getSharedPreferencesDataProviderInstance().getAppStartCount() >= FAMILIAR_USER_COUNT) {
            switch (getFilterProvider().getSharedPreferencesDataProviderInstance().hasUserApprovedCrashLogging()){

                case SharedPreferencesDataProvider.CRASH_LOGGING_APPROVED:
                    startCrashlytics(getContext());
                    break;

                case SharedPreferencesDataProvider.CRASH_LOGGING_NOT_READ:
                    SHOULD_SHOW_CRASH_LOGGING_PERMISSION = true;
                    break;

                case SharedPreferencesDataProvider.CRASH_LOGGING_NOT_APPROVED:
                    break;

                default:
                    break;
            }
        } else {
            getFilterProvider().getSharedPreferencesDataProviderInstance().incrementAppStartCount();
        }
    }

    public static void startCrashlytics(Context context) {
    }

    public static DataProvider getDataProviderInstance(){
        if (dataProvider == null){
            dataProvider = new DataProvider(getContext(), new ExceptionReporter.OnReportListener() {
                @Override
                public void report(Class fromClass, String reason, String exceptionMessage) {
                    if (getFilterProvider()
                            .getSharedPreferencesDataProviderInstance()
                            .hasUserApprovedCrashLogging() ==
                            SharedPreferencesDataProvider.CRASH_LOGGING_APPROVED) {
                        String message = "Class: " + fromClass.getName() + ", reason: " + reason +
                                ", exceptionMessage: " + exceptionMessage;
                        Log.w(WallyApplication.class.getSimpleName(), message);
                    }
                }
            });
        }
        return dataProvider;
    }

    public static FilterProvider getFilterProvider(){
        if(filterProvider == null){
            filterProvider = new FilterProvider(getContext());
        }
        return filterProvider;
    }


    public static CustomDataProvider getCustomDataProviderInstance(){
        if (customDataProvider == null){
            customDataProvider = new CustomDataProvider(getContext(), new ExceptionReporter.OnReportListener() {
                @Override
                public void report(Class fromClass, String reason, String exceptionMessage) {
                    if (getFilterProvider()
                            .getSharedPreferencesDataProviderInstance()
                            .hasUserApprovedCrashLogging() ==
                            SharedPreferencesDataProvider.CRASH_LOGGING_APPROVED) {
                        String message = "Class: " + fromClass.getName() + ", reason: " + reason +
                                ", exceptionMessage: " + exceptionMessage;
                        Log.w(WallyApplication.class.getSimpleName(), message);
                    }
                }
            });
        }
        return customDataProvider;
    }

    public static ImageDownloadManager getImageDownloadManagerInstance(){
        if(imageDownloadManager == null){
            imageDownloadManager = new ImageDownloadManager(getContext());
        }
        return imageDownloadManager;
    }

    public static HashMap<Long, String> getDownloadIDs() {
        if (pairedDownloadIds == null) {
            pairedDownloadIds = new HashMap<Long, String>();
        }
        return pairedDownloadIds;
    }

    public static FilterGroupsStructure getFilterSettings(){
        FilterGroupsStructure filterGroupsStructure = new FilterGroupsStructure();
        filterGroupsStructure.setTimespanFilter(filterProvider.getTimespan(FilterTimeSpanKeys.PARAMETER_KEY));
        filterGroupsStructure.setBoardsFilter(filterProvider.getBoards(FilterBoardsKeys.PARAMETER_KEY));
        filterGroupsStructure.setPurityFilter(filterProvider.getPurity(FilterPurityKeys.PARAMETER_KEY));
        filterGroupsStructure.setAspectRatioFilter(filterProvider.getAspectRatio(FilterAspectRatioKeys.PARAMETER_KEY));
        filterGroupsStructure.setResOptFilter(filterProvider.getResolutionOption(FilterResOptKeys.PARAMETER_KEY));
        filterGroupsStructure.setResolutionFilter(filterProvider.getResolution(FilterResolutionKeys.PARAMETER_KEY));
        return filterGroupsStructure;
    }

    public static Bitmap getBitmapThumb() {
        return bitmapThumb;
    }

    public static void setBitmapThumb(Bitmap bitmapThumb) {
        WallyApplication.bitmapThumb = bitmapThumb;
    }

    public static HashMap<String, Object> getSearchFragmentMessages() {
        if (searchFragmentMessages == null) {
            searchFragmentMessages = new HashMap<>();
        }
        return searchFragmentMessages;
    }

    public static HashMap<String, Object> readMessages(String fragmentTag) {
        HashMap<String, Object> messages = new HashMap<>();
        if (fragmentTag != null) {
            if (fragmentTag.equals(SearchFragment.TAG)
                    && !getSearchFragmentMessages().isEmpty()) {
                messages.putAll(searchFragmentMessages);
                searchFragmentMessages.clear();
                return messages;
            }
        }
        return messages;
    }
}
