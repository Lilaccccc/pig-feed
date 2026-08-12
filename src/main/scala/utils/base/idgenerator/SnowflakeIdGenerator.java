package utils.base.idgenerator;

public class SnowflakeIdGenerator {
    private final long workerId; // 机器ID
    private final long datacenterId; // 数据中心ID
    private long sequence = 0L; // 序列号
    // 配置参数
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;
    // 位移偏移量
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS + DATACENTER_ID_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS;

    private long lastTimestamp = -1L; // 上次生成ID的时间戳

    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    // 生成唯一ID
    public synchronized long generateUniqueId() {
        long timestamp = getCurrentTimestamp();

        // 检查时钟回退情况
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate ID.");
        }

        // 同一毫秒内自增序列号
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & ((1 << SEQUENCE_BITS) - 1); // 自增并掩码
            if (sequence == 0) {
                // 序列号用完，等待下一毫秒
                timestamp = getNextTimestamp(lastTimestamp);
            }
        } else sequence = 0L; // 不同毫秒重置序列号

        lastTimestamp = timestamp;

        // 组合生成唯一ID
        return ((timestamp << TIMESTAMP_SHIFT) | (datacenterId << DATACENTER_ID_SHIFT) | (workerId << WORKER_ID_SHIFT)
                | sequence);
    }

    // 获取当前时间戳
    private long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    // 等待下一毫秒，直到获得新的时间戳
    private long getNextTimestamp(long lastTimestamp) {
        long timestamp = getCurrentTimestamp();
        while (timestamp <= lastTimestamp) {
            timestamp = getCurrentTimestamp();
        }
        return timestamp;
    }
}