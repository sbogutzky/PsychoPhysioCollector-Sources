package pl.flex_it.androidplot;

import com.androidplot.series.XYSeries;

import java.util.List;

public class XYSeriesShimmer implements XYSeries {
    private List<Number> dataY;
    private int seriesIndex;
    private String title;

    public XYSeriesShimmer(List<Number> datasource, int seriesIndex, String title) {
        this.dataY = datasource;
        this.seriesIndex = seriesIndex;
        this.title = title;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public int size() {
        return dataY.size();
    }

    @Override
    public Number getY(int index) {
        return dataY.get(index);
    }

    @Override
    public Number getX(int index) {
        return index;
    }


    public void updateData(List<Number> datasource) { //dont need to use this cause, the reference is only stored, modifying the datasource externally will cause this to be updated as well
        this.dataY = datasource;

    }

}