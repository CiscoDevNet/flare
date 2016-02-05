package com.cisco.flare.trilateral;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

import com.cisco.flare.Thing;
import com.cisco.flare.trilateral.common.Constants;
import com.cisco.flare.trilateral.common.HTMLColors;

import java.util.ArrayList;

/**
 * Created by ofrebour on 25/03/15.
 */
public class DrawableThing extends Thing {
    private Thing thing;
    float arcSweepAngle = 70.0f;
    private Paint paint;
    private float arcStartAngle = 0;
    ArrayList<PointF> boundaries = new ArrayList();
    private boolean isBeingTouched;
    public double distanceFromUser;

    public DrawableThing(Thing t) {
        super(t);
        thing = t;
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(Constants.FLARE_FIN_ARC_STROKE_WIDTH);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(HTMLColors.getHtmlColor(this.getColor()));

        isBeingTouched = false;
    }

    public Thing getThing() {
        return thing;
    }

    // returns an angle in degrees
    private float getAngleToPosition(PointF target) {
        // OF: the origin is in the top left hand corner
        float angle = /*180 - */(float) Math.toDegrees(Math.atan2(target.y - position.y, target.x - position.x));

        if (angle < 0) {
            angle += 360;
        }

        return angle;
    }

    public static double distanceBetweenTwoPoints(PointF p, PointF q) {
        double dx = q.x - p.x;
        double dy = q.y - p.y;
        return Math.sqrt((dx * dx) + (dy * dy));
    }

    private double getDistanceToPosition(PointF target) {
        return DrawableThing.distanceBetweenTwoPoints(target, position);
    }

    public int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    public void drawOn(RectF rectF, Canvas canvas, float strokeWidth) {
        paint.setStrokeWidth(strokeWidth);
        canvas.drawArc(rectF, arcStartAngle, arcSweepAngle, false, paint);
    }

    // calculate the boundaries of the fin to check for intersections
    private void updateBoundaries(RectF rect) {
        boundaries.clear();
        PointF center = new PointF(rect.centerX(), rect.centerY());
        float outerRadius=(rect.width()-Constants.FLARE_FIN_ARC_STROKE_WIDTH)/2+Constants.FLARE_FIN_ARC_STROKE_WIDTH;
        float innerRadius = outerRadius - Constants.FLARE_FIN_TOUCHABLE_DISTANCE;

        float arcStartAngleInRadians = (float)Math.toRadians(arcStartAngle);
        float arcEndAngleInRadians = (float)Math.toRadians(arcStartAngle+arcSweepAngle);

        // add bottom left hand corner (inner circle)
        boundaries.add(posFromCenter(center, arcStartAngleInRadians, innerRadius));
        // add top left hand corner (outer circle)
        boundaries.add(posFromCenter(center, arcStartAngleInRadians, outerRadius));
        // add top right hand corner (outer circle)
        boundaries.add(posFromCenter(center, arcEndAngleInRadians, outerRadius));
        // add bottom right hand corner (inner circle)
        boundaries.add(posFromCenter(center, arcEndAngleInRadians, innerRadius));
    }

    public void update(RectF rectF, PointF userPosition, float globalAngle, TouchInformation touchInfo) {
        distanceFromUser = getDistanceToPosition(userPosition);

        arcSweepAngle = (float) FinDrawingPreferences.finSize(distanceFromUser);
//        Log.i("DrawableThing", "pos="+userPosition.x+","+userPosition.y+" ; dist="+distanceFromUser+" ; finSize="+arcSweepAngle);
        float angle = -globalAngle+getAngleToPosition(userPosition) + (float)FinDrawingPreferences.getInstance().getWatchAngle();
        arcStartAngle = (float)FinDrawingPreferences.transformAngleDegrees(angle)-arcSweepAngle/2;

        float alpha = (float)FinDrawingPreferences.finOpacity(distanceFromUser);
        paint.setColor(adjustAlpha(HTMLColors.getHtmlColor(this.getColor()), alpha));

        updateBoundaries(rectF);
        isBeingTouched = touchInfo.screenTouched && intersectsWithPoint(touchInfo.coordinates.x, touchInfo.coordinates.y);
    }

    private PointF posFromCenter(PointF center, float angleInRadians, float distance) {
        return new PointF((float)(center.x+Math.cos(angleInRadians)*distance), (float)(center.y+Math.sin(angleInRadians)*distance));
    }

    // check if the point is within the boundaries of this fin
    // note that we need to lock the canvas to make sure that the boundaries are not being
    // updated as we check for intersections
    private boolean intersectsWithPoint(float x, float y) {
        int i;
        double angle=0;
        PointF p1 = new PointF(), p2 = new PointF();
        int n = boundaries.size();

        for (i = 0 ; i < n ; i++) {
            PointF vertex = boundaries.get(i);
            PointF nextVertex = boundaries.get((i+1)%n);
            p1.x = vertex.x - x;
            p1.y = vertex.y - y;
            p2.x = nextVertex.x - x;
            p2.y = nextVertex.y - y;
            angle += angle2D(p1.x,p1.y,p2.x,p2.y);
        }

        return Math.abs(angle) >= Math.PI;
    }

    /*
       Return the angle between two vectors on a plane
       The angle is from vector 1 to vector 2, positive anticlockwise
       The result is between -pi -> pi
    */
    private double angle2D(double x1, double y1, double x2, double y2)
    {
        double dtheta,theta1,theta2;
        double TWOPI = 2*Math.PI;

        theta1 = Math.atan2(y1,x1);
        theta2 = Math.atan2(y2,x2);
        dtheta = theta2 - theta1;
        while (dtheta > Math.PI)
            dtheta -= TWOPI;
        while (dtheta < -Math.PI)
            dtheta += TWOPI;

        return(dtheta);
    }

    public boolean isBeingTouched() {
        return isBeingTouched;
    }
}
