package cat.urv.imas.tools;

import java.util.Comparator;

/**
 * Class used for sorting collections that store Work instances.
 */
public class WorkTypeComparator implements Comparator<Work> {
    
    /**
     * Method that sort two Work instances.
     * @param w1 First Work instance
     * @param w2 Second Work instance
     * @return -1/1 depending on the comparison
     */
    @Override
    public int compare(Work w1, Work w2) {
        if(w1.getType() == Work.WorkType.MOVE) {
            return -1;
        } else {
            return 1;
        }
    }

}