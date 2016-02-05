package com.cisco.flare.trilateral;

import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceView;

/**
 * Created by ofrebour on 25/03/15.
 */
public class FullScreenAnimationThread extends Thread {
    CompassView myView;
    private boolean running = false;

    public FullScreenAnimationThread(CompassView view) {
        myView = view;
    }

    public void setRunning(boolean run) {
        running = run;
    }

    @Override
    public void run() {
        while(running) {
            try {
                Canvas canvas = myView.getHolder().lockCanvas();

                if (canvas != null) {
                    synchronized (myView.getHolder()) {
                        myView.drawScene(canvas);
                    }
                    myView.getHolder().unlockCanvasAndPost(canvas);
                }
                sleep(30);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                Log.d("AnimationThread", "ERROR. myView="+myView);
                e.printStackTrace();
            }
        }
    }
}
