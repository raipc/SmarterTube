package com.liskovsoft.smartyoutubetv2.tv.ui.playback;

import android.app.Activity;
import android.app.Application;
import android.app.PictureInPictureParams;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView;
import com.liskovsoft.smartyoutubetv2.common.app.views.ViewManager;
import com.liskovsoft.smartyoutubetv2.common.prefs.GeneralData;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
public class PlaybackActivityPipTest {
    private TestPlaybackActivity activity;
    private PlaybackFragment fragment;

    public static class TestPlaybackActivity extends PlaybackActivity {
        boolean acceptsPip = true;
        int pipRequests;

        @Override
        public boolean enterPictureInPictureMode(PictureInPictureParams params) {
            pipRequests++;
            return acceptsPip;
        }
    }

    public static class TestPlaybackFragment extends PlaybackFragment {
        @Override
        public void onPIPChanged(boolean isInPip) {}
    }

    @Before
    public void setUp() {
        ReflectionHelpers.setStaticField(ViewManager.class, "sInstance", null);
        activity = Robolectric.buildActivity(TestPlaybackActivity.class).get();
        activity.setIntent(new Intent(activity, TestPlaybackActivity.class));
        shadowOf(activity.getPackageManager()).setSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE, true);
        fragment = new TestPlaybackFragment();
        fragment.blockEngine(true);
        ReflectionHelpers.setField(activity, "mPlaybackFragment", fragment);
        GeneralData.instance(activity).setBackgroundPlaybackShortcut(GeneralData.BACKGROUND_PLAYBACK_SHORTCUT_HOME_BACK);
        PlayerData.instance(activity).setBackgroundMode(PlayerData.BACKGROUND_MODE_PIP);
        ViewManager manager = ViewManager.instance(activity);
        manager.register(PlaybackView.class, TestPlaybackActivity.class, Activity.class);
        manager.addTop(activity);
    }

    @Test
    public void parentIsNotStartedUntilPlayerIsPinned() {
        activity.finish();
        assertEquals(1, activity.pipRequests);
        assertTrue(fragment.isEngineBlocked());
        assertNull(shadowOf(activity).getNextStartedActivity());

        activity.onPictureInPictureModeChanged(true);
        Intent parent = shadowOf(activity).getNextStartedActivity();
        assertNotNull(parent);
        assertEquals(Activity.class.getName(), parent.getComponent().getClassName());

        activity.onPictureInPictureModeChanged(true);
        assertNull(shadowOf(activity).getNextStartedActivity());
    }

    @Test
    public void parentResumingDuringPipTransitionDoesNotReopenPlayer() {
        activity.finish();
        Activity parentActivity = Robolectric.buildActivity(Activity.class).get();
        ViewManager manager = ViewManager.instance(activity);
        manager.register(Object.class, Activity.class, Activity.class);
        manager.addTop(parentActivity);

        activity.onPictureInPictureModeChanged(true);
        Intent parent = shadowOf(activity).getNextStartedActivity();
        assertNotNull(parent);
        assertEquals(Activity.class.getName(), parent.getComponent().getClassName());
    }

    @Test
    public void rejectedPipFallsBackWithoutWaitingForCallback() {
        activity.acceptsPip = false;
        activity.finish();
        assertNotNull(shadowOf(activity).getNextStartedActivity());
        activity.onPictureInPictureModeChanged(true);
        assertNull(shadowOf(activity).getNextStartedActivity());
    }

    @Test
    public void homeDoesNotLaunchParentOverTheLauncher() {
        activity.onUserLeaveHint();
        assertEquals(1, activity.pipRequests);
        activity.onPictureInPictureModeChanged(true);
        assertNull(shadowOf(activity).getNextStartedActivity());
    }
}
