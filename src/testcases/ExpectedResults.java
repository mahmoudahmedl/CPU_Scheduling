package testcases;

import java.util.LinkedHashMap;
import java.util.Map;

public class ExpectedResults {

    public static Map<String, int[]> expectedFor(int caseNo) {
        Map<String, int[]> m = new LinkedHashMap<>();
        switch (caseNo) {
            case 1: // Q=3, CS=0
                m.put("P1", new int[]{4, 4, 0});
                m.put("P2", new int[]{13, 12, 5});
                m.put("P3", new int[]{6, 3, 1});
                break;
            case 2: // Q=3, CS=1
                m.put("P1", new int[]{5, 5, 0});
                m.put("P2", new int[]{17, 16, 10});
                m.put("P3", new int[]{10, 6, 2});
                break;
            case 3: // idle gap, CS=1
                m.put("P1", new int[]{2, 2, 0});
                m.put("P2", new int[]{11, 6, 3});
                m.put("P3", new int[]{8, 2, 1});
                break;
            case 4: // ties, CS=0
                m.put("P1", new int[]{3, 3, 0});
                m.put("P2", new int[]{6, 6, 3});
                m.put("P3", new int[]{9, 8, 5});
                break;
            case 5: // preemption, CS=0
                m.put("P1", new int[]{15, 15, 7});
                m.put("P2", new int[]{8, 7, 3});
                m.put("P3", new int[]{4, 2, 0});
                m.put("P4", new int[]{5, 2, 1});
                break;
            case 6: // big gaps, CS=2
                m.put("P1", new int[]{3, 3, 0});
                m.put("P2", new int[]{22, 12, 6});
                m.put("P3", new int[]{15, 4, 2});
                m.put("P4", new int[]{28, 8, 4});
                break;
            default:
                throw new IllegalArgumentException("Unknown test case: " + caseNo);
        }
        return m;
    }
}