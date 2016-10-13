package monitoring.web;

public class ClientResponse {
    private final long[] cpu;
    private final long[] ram;

    public ClientResponse(long[] cpu, long[] ram) {
        this.cpu = cpu;
        this.ram = ram;
    }

    public long[] getCpu() {
        return cpu;
    }

    public long[] getRam() {
        return ram;
    }
}
