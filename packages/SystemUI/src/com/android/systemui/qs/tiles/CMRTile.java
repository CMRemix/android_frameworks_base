/*
 * Copyright (C) 2016 Resurrection Remix
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

package com.android.systemui.qs.tiles;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;

import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTileView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.android.internal.logging.MetricsProto.MetricsEvent;

public class CMRTile extends QSTile<QSTile.BooleanState> {
    private boolean mListening;
    private CMRObserver mObserver;
    private static final Intent CMR_SETTINGS = new Intent().setComponent(new ComponentName(
            "com.android.settings", "com.android.settings.Settings$MainSettingsActivity"));
    private static final Intent CMR_OTA = new Intent().setComponent(new ComponentName(
            "com.cmremix.updater", "com.cmremix.updater.UpdatesSettings"));

    public CMRTile(Host host) {
        super(host);
        mObserver = new CMRObserver(mHandler);
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.QUICK_SETTINGS;
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.cmr_qs_tile);
    }

    @Override
    protected void handleClick() {
      mHost.startActivityDismissingKeyguard(CMR_SETTINGS);
    }

    @Override
    public void handleLongClick() {
      mHost.startActivityDismissingKeyguard(CMR_SETTINGS);
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }


    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.icon = ResourceIcon.get(R.drawable.ic_qs_cmrtools);
        state.label = mContext.getString(R.string.cmr_qs_tile);

	}

    @Override
    public void setListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening;
    }

    private class CMRObserver extends ContentObserver {
        public CMRObserver(Handler handler) {
            super(handler);
        }
    }
}
