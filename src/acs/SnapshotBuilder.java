package acs;

import java.util.ArrayList;
import java.util.List;
import model.ProcessView;
import model.TimeStepSnapshot;

public class SnapshotBuilder {

    public static TimeStepSnapshot build(
            int time,
            String runningId,
            List<ProcessView> t1,
            List<ProcessView> t2,
            List<String> notes) {

        List<ProcessView> safeT1 = (t1 != null) ? new ArrayList<>(t1) : new ArrayList<>();
        List<ProcessView> safeT2 = (t2 != null) ? new ArrayList<>(t2) : new ArrayList<>();
        List<String> safeNotes = (notes != null) ? new ArrayList<>(notes) : new ArrayList<>();

        return new TimeStepSnapshot(time, runningId, safeT1, safeT2, safeNotes);
    }
}
