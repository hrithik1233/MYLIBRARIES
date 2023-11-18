

import android.os.Handler;
import android.os.Looper;
import java.util.PriorityQueue;


interface SecondsCounterInterface {
    void updateTimeFormatSecondsCounter(long hour, long minute, long second);

    void totalSecondsCounter(long totalSeconds);

    void timeQueueInterfaceCall(long hour, long minute, long second);
}

class TimeQueueSecondCounter implements Comparable<TimeQueueSecondCounter> {
    public long hr, min, sec;

    public TimeQueueSecondCounter(long hr, long min, long sec) {
        this.hr = hr;
        this.min = min;
        this.sec = sec;
    }

    @Override
    public int compareTo(TimeQueueSecondCounter other) {
        if (this.hr != other.hr) {
            return Long.compare(this.hr, other.hr);
        } else if (this.min != other.min) {
            return Long.compare(this.min, other.min);
        } else {
            return Long.compare(this.sec, other.sec);
        }
    }
}

public class SecondsCounter {

    private final PriorityQueue<TimeQueueSecondCounter> queue = new PriorityQueue<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile long hr = 0;
    private volatile long min = 0;
    private volatile long sec = 0;
    private volatile boolean isPause = false;
    private volatile   boolean isCounting = false;
    private boolean destruct = false;
    private boolean callInterface;
    private SecondsCounterInterface counterInterface = null;

private   Thread thread;
    public SecondsCounter() {
    }

    public SecondsCounter(SecondsCounterInterface counterInterface) {
        this.counterInterface = counterInterface;
        this.callInterface = true;
    }

    public boolean addTimeQueueInterfaceCall(TimeQueueSecondCounter item) {
        if (item.hr > hr || (item.hr == hr && item.min > min) || (item.hr == hr && item.min == min && item.sec > sec)) {
            queue.add(item);
            return true;
        }
        return false;
    }

    public synchronized void start() {
        if (!isCounting) {
            isCounting = true;
          thread = new Thread(() -> {
                try {
                    while (!thread.isInterrupted()) {
                        if (!isPause) {
                            sec++;
                            min += sec / 60;
                            hr += min / 60;
                            sec %= 60;
                            min %= 60;
                            mainHandler.post(() -> {
                                if (counterInterface != null && callInterface) {
                                    counterInterface.updateTimeFormatSecondsCounter(hr, min, sec);
                                    counterInterface.totalSecondsCounter(getTotalSeconds());
                                    if (!queue.isEmpty()) {
                                        TimeQueueSecondCounter time = queue.peek();
                                        if (time.hr <= hr && time.min <= min && time.sec <= sec) {
                                            counterInterface.timeQueueInterfaceCall(hr, min, sec);
                                            queue.poll();
                                        }
                                    }
                                }
                            });

                            if (!Thread.interrupted()) {
                                Thread.sleep(1000);
                            }
                        }else{
                            break;
                        }
                    }
                    isCounting = false;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
       thread.start();

        }
    }

    public void pause() {
        isPause = true;
        isCounting = false;
        thread.interrupt();
    }

    public void resume() {
        isPause = false;
        start();
        isCounting = true;
    }

    public void reset() {
        isPause = false;
        thread.interrupt();
        hr = 0;
        min = 0;
        sec = 0;

        if (counterInterface != null) {
            counterInterface.updateTimeFormatSecondsCounter(hr, min, sec);
            counterInterface.totalSecondsCounter(getTotalSeconds());
        }
    }

    public void stop() {
        thread.interrupt();
        isPause = false;
        isCounting = false;
        destruct=true;
    }

    public boolean isCounting() {
        return isCounting;
    }

    public long getHour() {
        return hr;
    }

    public long getMinutes() {
        return min;
    }

    public long getSeconds() {
        return sec;
    }
    

    public long getTotalSeconds() {
        return (hr * 60 * 60) + (min * 60) + sec;
    }

    protected void finalize() {
        this.stop();
    }

    public void setHr(long hr) {

        this.hr = hr;
        if(isCounting){
            thread.interrupt();
            start();
        }
       
    }

    public void setMin(long min) {
        this.min = min;
        if(isCounting){
            thread.interrupt();
            start();
        }
    }

    public void setSec(long sec) {
        this.sec = sec;
        if(isCounting){
            thread.interrupt();
            start();
        }
    }
}
