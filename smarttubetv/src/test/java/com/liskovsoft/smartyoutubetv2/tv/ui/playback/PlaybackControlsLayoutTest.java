package com.liskovsoft.smartyoutubetv2.tv.ui.playback;

import android.app.Application;
import android.content.Context;
import android.graphics.Rect;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.liskovsoft.smartyoutubetv2.tv.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
public class PlaybackControlsLayoutTest {
    @Test
    public void portraitKeepsInfoSeparateAndLetsLastButtonScrollIntoView() {
        ViewGroup row = createRow(600);
        assertSeparate(row, R.id.controls_dock, R.id.additional_info);
        assertSeparate(row, R.id.secondary_controls_dock, R.id.time_info);

        HorizontalScrollView scroll = (HorizontalScrollView) row.findViewById(R.id.controls_dock).getParent();
        assertTrue(scroll.getChildAt(0).getWidth() > scroll.getWidth());
        scroll.scrollTo(scroll.getChildAt(0).getWidth(), 0);
        assertTrue(scroll.getScrollX() > 0);
        ViewGroup buttons = (ViewGroup) ((ViewGroup) scroll.getChildAt(0)).getChildAt(0);
        Rect lastButton = bounds(row, buttons.getChildAt(buttons.getChildCount() - 1));
        Rect viewport = bounds(row, scroll);
        assertTrue(lastButton.left >= viewport.left && lastButton.right <= viewport.right);
    }

    @Test
    public void landscapeKeepsAllButtonsVisibleBesideInfo() {
        ViewGroup row = createRow(960);
        assertSeparate(row, R.id.controls_dock, R.id.additional_info);
        assertSeparate(row, R.id.secondary_controls_dock, R.id.time_info);
        View dock = row.findViewById(R.id.controls_dock);
        assertTrue(dock.getWidth() <= ((View) dock.getParent()).getWidth());
    }

    private ViewGroup createRow(int widthDp) {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.App_Theme_Leanback_Player);
        ViewGroup row = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.lb_playback_transport_controls_row, null);
        ((TextView) row.findViewById(R.id.quality_info)).setText("4K/60/VP9");
        ((TextView) row.findViewById(R.id.ending_time)).setText("Remaining: 8:43");
        ((TextView) row.findViewById(R.id.current_time)).setText("1:51");
        ((TextView) row.findViewById(R.id.total_time)).setText("10:35");
        for (int id : new int[]{R.id.controls_dock, R.id.secondary_controls_dock}) {
            LinearLayout buttons = new LinearLayout(context);
            for (int i = 0; i < 9; i++) {
                buttons.addView(new View(context), new LinearLayout.LayoutParams(dp(context, 60), dp(context, 48)));
            }
            ((ViewGroup) row.findViewById(id)).addView(buttons);
        }
        row.measure(View.MeasureSpec.makeMeasureSpec(dp(context, widthDp), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        row.layout(0, 0, row.getMeasuredWidth(), row.getMeasuredHeight());
        return row;
    }

    private void assertSeparate(ViewGroup row, int dockId, int infoId) {
        View viewport = (View) row.findViewById(dockId).getParent();
        assertFalse(Rect.intersects(bounds(row, viewport), bounds(row, row.findViewById(infoId))));
    }

    private Rect bounds(ViewGroup root, View child) {
        Rect rect = new Rect();
        child.getDrawingRect(rect);
        root.offsetDescendantRectToMyCoords(child, rect);
        return rect;
    }

    private int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
