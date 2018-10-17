package com.leading.lq;

import org.junit.Test;

import com.apm70.fileq.util.FlowLimiter;

/**
 * 流量控制测试
 * @author liuyg
 *
 */
public class FlowLimiterTest {

    @Test
    public void test() {
        // 每秒1024KB
        final FlowLimiter limiter = new FlowLimiter(1024);
        // 模拟测试50M流量
        final long total = 1024L * 1024 * 50;
        final long start = System.currentTimeMillis();
        for (int i = 0; i < total; i += 1024 * 512) {// 每次1K数据
            limiter.flowIn(1024 * 512);
            if ((i % (1024 * 1024)) == 0) {
                System.out.println("发送" + (i / 1024 / 1024) + "M数据");
            }
        }
        System.out.println("发送50M耗时：" + ((System.currentTimeMillis() - start) / 1000) + "秒");
    }
}
