package com.rommelbendel.scanQ;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

class RectEdit extends View {
    private final Canvas canvas;
    private ArrayList<int[]> dims;
    private List<Integer> pos;
    private int[] dim;
    private final Paint paint;

    public RectEdit(Context context, Canvas canvas, ArrayList<int[]> dims, Paint paint, @Nullable List<Integer> pos) {
        super(context);
        this.canvas = canvas;
        this.dims = dims;
        this.pos = pos;
        this.paint = paint;
    }

    public RectEdit(Context context, Canvas canvas, int[] dim, Paint paint) {
        super(context);
        this.canvas = canvas;
        this.dim = dim;
        this.paint = paint;
    }

    public RectF draw() {
        RectF rect = new RectF(dim[0], dim[1], dim[2], dim[3]);
        paint.setColor(Color.WHITE);
        canvas.drawRect(rect, paint);

        return rect;
    }

    public RectF remove2nds(int color) {
        RectF rect = new RectF(dim[0], dim[1], dim[2], dim[3]);
        paint.setColor(color);
        canvas.drawRect(rect, paint);

        return rect;
    }

    public void add(int color) {
        if(pos != null) {
            for (int d = 0; d < pos.size(); d++) {
                RectF rect = new RectF(dims.get(pos.get(d))[0], dims.get(pos.get(d))[1], dims.get(pos.get(d))[2], dims.get(pos.get(d))[3]);
                paint.setColor(color);
                canvas.drawRect(rect, paint);
            }
        } else {
            for (int d = 0; d < dims.size(); d++) {
                RectF rect = new RectF(dims.get(d)[0], dims.get(d)[1], dims.get(d)[2], dims.get(d)[3]);
                paint.setColor(color);
                canvas.drawRect(rect, paint);
            }
        }
    }
}
