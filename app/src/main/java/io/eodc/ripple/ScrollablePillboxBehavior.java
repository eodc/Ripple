package io.eodc.ripple;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

/**
 * Behavior for scrollable pillbox
 */
@SuppressWarnings("unused")
public class ScrollablePillboxBehavior extends CoordinatorLayout.Behavior<View> {

    private float distance;

    public ScrollablePillboxBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull View child, @NonNull View directTargetChild, @NonNull View target, int axes, int type) {
        return (axes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull View child, @NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
        distance -= dyConsumed;
    }

    @Override
    public boolean onNestedPreFling(@NonNull CoordinatorLayout coordinatorLayout, @NonNull View child, @NonNull View target, float velocityX, float velocityY) {
        Resources r = coordinatorLayout.getResources();
        final float threshold = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 108, r.getDisplayMetrics());
        if (velocityY < 0 && distance > threshold) {
            child.animate()
                    .setDuration(100)
                    .translationY(child.getMeasuredHeight());
        } else if (velocityY > 0) {
            child.animate()
                    .setDuration(100)
                    .translationY(0);
        }
        return super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY);
    }
}
