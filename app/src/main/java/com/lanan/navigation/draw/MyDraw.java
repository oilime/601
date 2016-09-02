package com.lanan.navigation.draw;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.lanan.zigbeetransmission.dataclass.LocationInfo;

import java.text.DecimalFormat;

public class MyDraw extends View {

    private Paint brush = new Paint();
    private final Path path = new Path();

    private final DecimalFormat decimalFormat = new DecimalFormat("#.000000");

    private final double west = 112.988455;
    private final double east = 113.001845;
    private final double north = 28.215616;
    private final double south = 28.208438;
    private final double startX = (west + east) / 2;
    private final double startY = (north + south) / 2;
    private int width;
    private int height;

    public enum location {WEST, EAST, NORTH, SOUTH}

    public MyDraw(Context context) {
        super(context);
    }

    public MyDraw(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setBackgroundColor(Color.GRAY);

        brush.setAntiAlias(true);
        brush.setColor(Color.BLUE);
        brush.setStyle(Paint.Style.STROKE);
        brush.setStrokeJoin(Paint.Join.ROUND);
        brush.setStrokeWidth(2);
    }

    public MyDraw(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawPath(path, brush);
    }

    public void drawNew(LocationInfo point) {
        float x = (float) (((point.getLng() - startX) / (east - west)) * width + width / 2);
        float y = (float) (((startY - point.getLat()) / (north - south)) * height + height / 2);

        path.lineTo(x, y);
        path.addCircle(x, y, 1, Path.Direction.CW);
        postInvalidate();
    }

    public void drawOrigin(LocationInfo point) {
        height = this.getMeasuredHeight();
        width = this.getMeasuredWidth();

        Log.d("Emilio", "height: " + height + " width: " + width);

        float x = (float) (((point.getLng() - startX) / (east - west)) * width + width / 2);
        float y = (float) (((startY - point.getLat()) / (north - south)) * height + height / 2);

        path.moveTo(x, y);
        path.addCircle(x, y, 1, Path.Direction.CW);
        postInvalidate();
    }

    public String getParam(location l) {
        switch (l) {
            case WEST:
                return decimalFormat.format(west);
            case EAST:
                return decimalFormat.format(east);
            case NORTH:
                return decimalFormat.format(north);
            case SOUTH:
                return decimalFormat.format(south);
        }
        return null;
    }
}