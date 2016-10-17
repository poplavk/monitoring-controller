package monitoring.web.request;

public class TimeAndCountRequest extends ClientRequest {
    private long from;
    private int count;

    public TimeAndCountRequest(long from, int count) {
        this.from = from;
        this.count = count;
    }

    public long getFrom() {
        return from;
    }

    public int getCount() {
        return count;
    }

    @Override
    public String toString() {
        return "from=" + from + "," + "count=" + count;
    }
}
