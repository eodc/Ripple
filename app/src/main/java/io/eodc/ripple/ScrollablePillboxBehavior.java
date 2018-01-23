package io.eodc.ripple;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

/**
 * Behavior for scrollable pillbox
 */
@SuppressWarnings("unused")
public class ScrollablePillboxBehavior extends CoordinatorLayout.Behavior<View> {
    public ScrollablePillboxBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private float distance;

    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull View child, @NonNull View directTargetChild, @NonNull View target, int axes, int type) {
        return (axes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull final View child, @NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
        distance -= dyConsumed;
        if (!target.canScrollVertically(1)) {
            if (child instanceof TextView) {
                if (child.isShown()) {
                    child.startAnimation(AnimationUtils.loadAnimation(coordinatorLayout.getContext(), R.anim.collapse_recents_notif));
                    child.postOnAnimation(new Runnable() {
                        @Override
                        public void run() {
                            child.setVisibility(View.INVISIBLE);
                        }
                    });
                }
            } else if (child instanceof ConstraintLayout) {
                showComposer(child);
            }
        }
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type);
    }

    @Override
    public boolean onNestedPreFling(@NonNull CoordinatorLayout coordinatorLayout, @NonNull View child, @NonNull View target, float velocityX, float velocityY) {
        Resources r = coordinatorLayout.getResources();
        float threshold = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 108, r.getDisplayMetrics());
        if (velocityY < 0 && distance > threshold) {
            if (child instanceof ConstraintLayout) {
                hideComposer(child);
            } else if (child instanceof TextView) {
                if (!child.isShown()) {
                    child.setVisibility(View.VISIBLE);
                    child.startAnimation(AnimationUtils.loadAnimation(coordinatorLayout.getContext(), R.anim.expand_recents_notif));
                }
            }
        } else if (velocityY > 0) {
            if (child instanceof ConstraintLayout) {
                showComposer(child);
            }
        }
        return super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY);
    }

    private void hideComposer(View view) {
        view.animate()
                .setDuration(100)
                .translationY(view.getMeasuredHeight());
    }
    private void showComposer(View view) {
        view.animate()
                .setDuration(100)
                .translationY(0);
    }
}
