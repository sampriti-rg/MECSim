package common;

import java.util.Comparator;

public class DcDistanceIndexComparator implements Comparator {
    @Override
    public int compare(Object o1, Object o2) {
        DcDistanceIndexPair dc1 = (DcDistanceIndexPair) o1;
        DcDistanceIndexPair dc2 = (DcDistanceIndexPair) o2;
        if(dc1.getDistance() == dc2.getDistance())
            return 0;
        else if(dc1.getDistance() > dc2.getDistance())
            return 1;
        else
            return -1;
    }
}
