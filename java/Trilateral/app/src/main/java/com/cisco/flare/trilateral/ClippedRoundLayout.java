package com.cisco.flare.trilateral;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Region;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

/**
 * Custom layout clipping the output of all children views to a circle
 */
public class ClippedRoundLayout extends RelativeLayout {

    private Path clippingPath = null;

    public ClippedRoundLayout(Context context) {
        super(context);
        init();
    }

    public ClippedRoundLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ClippedRoundLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        // the line below ensures that draw(canvas) will be called
        setWillNotDraw(false);
    }

    @Override
    public void draw(Canvas canvas) {
        if (clippingPath == null) {
            if (getWidth() > 0 && getHeight() > 0) {
                clippingPath = new Path();
                float radius = getWidth() / 2 - 1;
                clippingPath.addCircle(radius, radius, radius, Path.Direction.CW);
            }
        }
        if (clippingPath != null) {
            canvas.clipPath(clippingPath, Region.Op.INTERSECT);
        }
        super.draw(canvas);
    }
}
