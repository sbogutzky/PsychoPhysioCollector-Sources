package com.shimmerresearch.adapters;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.androidplot.series.XYSeries;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;

import java.util.Arrays;

import de.bogutzky.datacollector.app.R;
import pl.flex_it.androidplot.MultitouchPlot;

public class ListViewAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final String[] values;
    MultitouchPlot multitouchPlot;
    SparseArray<MultitouchPlot> hm = new SparseArray<MultitouchPlot>();

    public ListViewAdapter(Context context, String[] values) {
        super(context, R.layout.row_layout_gesture, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.row_layout_gesture, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.label);
        textView.setText(values[position]);

        multitouchPlot = (MultitouchPlot) rowView.findViewById(R.id.multitouchPlot);
        hm.put(position, multitouchPlot);

        LineAndPointFormatter series1Format;
        series1Format = new LineAndPointFormatter(
                Color.rgb(89, 156, 255),  // line color
                null,                   // point color
                null);                 // fill color (optional)


        Number[] jaTime = new Number[100];
        for (int i = 0; i < jaTime.length; i++) {
            jaTime[i] = i;
        }

        XYSeries series1 = new SimpleXYSeries(Arrays.asList(jaTime), Arrays.asList(jaTime), "Joint Angle");
        multitouchPlot.addSeries(series1, series1Format);

        Button buttonConfig = (Button) rowView.findViewById(R.id.buttonConfig);
        buttonConfig.setTag(R.id.PARENT_ID, position); //use tags to keep track of your buttons

        buttonConfig.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub

                //should be some code here to look through the folder for the *.dat file you want

                Log.d("ShimmerButtonTag", (arg0.getTag(R.id.PARENT_ID).toString()));

                LineAndPointFormatter series1Format;
                series1Format = new LineAndPointFormatter(
                        Color.rgb(89, 156, 255),  // line color
                        null,                   // point color
                        null);                 // fill color (optional)


                Number[] jaTime = new Number[100];
                for (int i = 0; i < jaTime.length; i++) {
                    jaTime[i] = i * 2;
                }

                XYSeries series1 = new SimpleXYSeries(Arrays.asList(jaTime), Arrays.asList(jaTime), "Joint Angle");
                hm.get(Integer.parseInt(arg0.getTag(R.id.PARENT_ID).toString())).clear();
                hm.get(Integer.parseInt(arg0.getTag(R.id.PARENT_ID).toString())).addSeries(series1, series1Format);
                hm.get(Integer.parseInt(arg0.getTag(R.id.PARENT_ID).toString())).redraw();
            }

        });

        return rowView;
    }
} 