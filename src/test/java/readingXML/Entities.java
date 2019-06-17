package readingXML;

import java.util.ArrayList;
import java.util.List;

public class Entities<E>
{
    private String TotalResults;

    private ArrayList<E> Entity;//you can use List<> insted 

    public String getTotalResults ()
    {
        return TotalResults;
    }

    public void setTotalResults (String TotalResults)
    {
        this.TotalResults = TotalResults;
    }

    public ArrayList<E> getEntity ()
    {
        return Entity;
    }

    public void setEntity (ArrayList<E> Entity)
    {
        this.Entity = Entity;
    }

    @Override
    public String toString()
    {
        return "ClassPojo [TotalResults = "+TotalResults+", Entity = "+Entity+"]";
    }
}