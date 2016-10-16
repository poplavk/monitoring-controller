package monitoring.web.request;

public class TimeRequest extends ClientRequest {
    private long from;
    private long to;

    public TimeRequest(long from, long to) {
        this.from = from;
        this.to = to;
    }

    public long getFrom() {
        return from;
    }

    public long getTo() {
        return to;
    }

    @Override
    public String toString() {
        return "from=" + from + "," + "to=" + to;
    }
}
