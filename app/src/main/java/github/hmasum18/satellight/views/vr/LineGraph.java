package github.hmasum18.satellight.views.vr;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class LineGraph extends View {
    private int midPoint = 0;
    private int WIDTH = 0;
    private final int NUMBER_OF_POINTS = 500;
    private int pointDistance;

    private static final String TAG = "LineGraph";

    private int [] points = new int[NUMBER_OF_POINTS];

    private Paint midLineColor;
    private Paint graphLineColor;

    public LineGraph(Context context) {
        super(context);
        init();
    }

    public LineGraph(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {

        this.post(() -> {
            midPoint = this.getMeasuredHeight() / 2;
            WIDTH = getMeasuredWidth();
            pointDistance = this.getMeasuredWidth() / NUMBER_OF_POINTS;

//            Log.d(TAG, String.format("init-- mid: %d, wid %d", midPoint, WIDTH));
        });

        midLineColor = new Paint(Paint.ANTI_ALIAS_FLAG);
        graphLineColor = new Paint(Paint.ANTI_ALIAS_FLAG);

        midLineColor.setColor(Color.GRAY);
        graphLineColor.setColor(Color.GREEN);
        midLineColor.setStrokeWidth(3);
        graphLineColor.setStrokeWidth(3);
    }

    public void addPoint(int point) {
        for (int i = 1; i < NUMBER_OF_POINTS; i++) {
            points[i-1] = points[i];
        }
        points[NUMBER_OF_POINTS-1] = point;
        invalidate();
    }

    public void setGraphLineColor(int color){
        graphLineColor.setColor(color);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw Mid Line
        canvas.drawLine(0, midPoint, WIDTH, midPoint, midLineColor);

        // Draw Lines
        for (int i = 0; i < NUMBER_OF_POINTS; i++) {
//            Log.d(TAG, String.format("onDraw: [%d] %d", i, points[i]));
            if (i==0) continue;
            canvas.drawLine(pointDistance*(i-1), midPoint-points[i-1], pointDistance*i, midPoint-points[i], graphLineColor);
        }
    }
}
