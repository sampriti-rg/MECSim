package applications.delta;

import dag.TaskNode;

import java.util.Comparator;

public class RankComparator implements Comparator {
    @Override
    public int compare(Object o1, Object o2) {
        TaskNode t1 = (TaskNode) o1;
        TaskNode t2 = (TaskNode) o2;
        if(t1.getRank() == t2.getRank())
            return 0;
        else if(t1.getRank() < t2.getRank())
            return 1;
        else
            return -1;
    }
}
