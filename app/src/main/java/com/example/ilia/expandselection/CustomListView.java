package com.example.ilia.expandselection;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.transition.TransitionManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class CustomListView extends ConstraintLayout {

    ConstraintSet layoutConstraints = new ConstraintSet();
    LinkedList<Integer> itemsId = new LinkedList<>();

    int expandedHeight;
    int collapsedHeight;

    boolean isHeightInvalidated = true;
    boolean isExpanded = true;

    int topViewId = -1;

    public CustomListView(Context context) {
        this(context, null);
    }

    public CustomListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void addItems(List<String> items) {
        int key = 1;
        topViewId = key;
        for (String name : items) {
            View v = LayoutInflater.from(getContext()).inflate(R.layout.item, null);
            v.setId(key);
            ((TextView) v).setText(name);

            v.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClick(v);
                }
            });

            addView(v);

            itemsId.add(key);

            key++;
        }
        layoutConstraints.clone(this);

        View prevView = null;

        for (int i = 0; i < getChildCount(); i++) {
            final int position = i;
            View child = getChildAt(position);
            if (position == 0) {
                setHeaderConstraints(child.getId());
                prevView = child;
                continue;
            }

            setListConstraints(child.getId(), prevView.getId());
            prevView = child;
        }

        isHeightInvalidated = true;

        layoutConstraints.applyTo(this);
        invalidate();
    }

    private void toggleAnimation() {
        if (isExpanded) {
            collapse();
        } else {
            expand();
        }
        isExpanded = !isExpanded;
    }

    private void expand() {
        animation(collapsedHeight, expandedHeight);
    }

    private void collapse() {
        animation(expandedHeight, collapsedHeight);
    }

    private void animation(int startHeight, int endHeight) {
        ValueAnimator animator = ValueAnimator.ofInt(startHeight, endHeight);
        animator.setDuration(500);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                LayoutParams lp = (LayoutParams) getLayoutParams();
                lp.height = value;
                setLayoutParams(lp);
                invalidate();
            }
        });
        animator.start();
    }

    private void onItemClick(View v) {
        if (itemsId.size() <= 1) return;

        if (v.getId() == itemsId.get(0)) {
            toggleAnimation();

            return;
        }
        int topItemId = itemsId.get(0);
        int selectedItemPosition = -1;
        for (int i = 0; i < itemsId.size(); i++) {
            if (v.getId() == itemsId.get(i)){
                selectedItemPosition = i;
            }
        }

        if (selectedItemPosition == -1) return;

        Collections.swap(itemsId, 0, selectedItemPosition);

        layoutConstraints.clear(v.getId(), ConstraintSet.TOP);
        layoutConstraints.clear(topItemId, ConstraintSet.TOP);

        layoutConstraints.connect(v.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
        layoutConstraints.connect(topItemId, ConstraintSet.TOP, itemsId.get(selectedItemPosition - 1), ConstraintSet.BOTTOM);

        if (selectedItemPosition != 1) {
            int secondItemId = itemsId.get(1);
            layoutConstraints.clear(secondItemId, ConstraintSet.TOP);
            layoutConstraints.connect(secondItemId, ConstraintSet.TOP, itemsId.getFirst(), ConstraintSet.BOTTOM);
        }

        if (selectedItemPosition != itemsId.size() - 1) {
            int nextAfterSelectedItemId = itemsId.get(selectedItemPosition + 1);
            layoutConstraints.clear(nextAfterSelectedItemId, ConstraintSet.TOP);
            layoutConstraints.connect(nextAfterSelectedItemId, ConstraintSet.TOP, itemsId.get(selectedItemPosition), ConstraintSet.BOTTOM);
        }

        TransitionManager.beginDelayedTransition(this);

        layoutConstraints.applyTo(this);
    }

    private void setHeaderConstraints(int id) {
        layoutConstraints.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
        setLeftAndRightConstraints(id);
    }

    private void setListConstraints(int id, int prevId) {
        layoutConstraints.connect(id, ConstraintSet.TOP, prevId, ConstraintSet.BOTTOM);
        setLeftAndRightConstraints(id);
    }

    private void setLeftAndRightConstraints(int id) {
        layoutConstraints.connect(id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT);
        layoutConstraints.connect(id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (isHeightInvalidated) {
            expandedHeight = getMeasuredHeight();
            isHeightInvalidated = false;
        }
        if (itemsId.size() != 0) {
            collapsedHeight = findViewById(itemsId.get(0)).getHeight();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        layoutConstraints.clone(this);
    }
}
