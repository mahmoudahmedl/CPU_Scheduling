package acs;

import model.Process;
import model.ProcessView;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/** FIFO ready queue used by the Tier 1 Round Robin scheduler. */
public class Tier1Queue {
    private final Queue<Process> queue = new ArrayDeque<>();

    public void enqueue(Process p) {
        if (p != null) {
            queue.offer(p);
        }
    }

    public Process dequeue() {
        return queue.poll();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }

    /** Returns a read-only view of the current FIFO order. */
    public List<ProcessView> snapshotView() {
        List<ProcessView> view = new ArrayList<>();
        for (Process p : queue) {
            view.add(ProcessView.of(p));
        }
        return view;
    }
}

