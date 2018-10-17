package com.leading.lq;

import java.util.concurrent.TimeUnit;

import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.Swap;
import org.hyperic.sigar.cmd.Iostat;
import org.hyperic.sigar.shell.ShellCommandExecException;
import org.hyperic.sigar.shell.ShellCommandUsageException;
import org.junit.Test;

import com.apm70.fileq.metrics.CpuGaugeSet;
import com.apm70.fileq.metrics.DiskGaugeSet;
import com.apm70.fileq.metrics.MemoryUsageGaugeSet;
import com.apm70.fileq.metrics.SigarService;
import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;

/**
 * 监控指标测试
 * @author liuyg
 *
 */
public class MetricsTest {
    // https://github.com/yangengzhe/sigar-system_runtime/blob/master/demo/demo/sigar.java

    //    final Slf4jReporter reporter = Slf4jReporter
    //            .forRegistry(registry)
    //            .withLoggingLevel(LoggingLevel.INFO)
    //            .outputTo(LoggerFactory.getLogger("metrics"))
    //            .build();

    //@SigarTest
    public void testSystemMemory() throws SigarException {
        final Sigar sigar = new Sigar();
        final Mem mem = sigar.getMem();
        // 内存总量
        System.out.println("内存总量:    " + (mem.getTotal() / 1024L) + "K av");
        // 当前内存使用量
        System.out.println("当前内存使用量:    " + (mem.getUsed() / 1024L) + "K used");
        // 当前内存剩余量
        System.out.println("当前内存剩余量:    " + (mem.getFree() / 1024L) + "K free");
        final Swap swap = sigar.getSwap();
        // 交换区总量
        System.out.println("交换区总量:    " + (swap.getTotal() / 1024L) + "K av");
        // 当前交换区使用量
        System.out.println("当前交换区使用量:    " + (swap.getUsed() / 1024L) + "K used");
        // 当前交换区剩余量
        System.out.println("当前交换区剩余量:    " + (swap.getFree() / 1024L) + "K free");
    }

    //@SigarTest
    public void testCpuTotal() throws InterruptedException {
        final MetricRegistry registry = new MetricRegistry();
        final ConsoleReporter reporter = ConsoleReporter.forRegistry(registry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        final CpuGaugeSet cpuGauges = new CpuGaugeSet(new SigarService());
        registry.registerAll(cpuGauges);
        reporter.start(5, TimeUnit.SECONDS);
        Thread.sleep(60000L);
    }

    @Test
    public void testJvmMemory() throws InterruptedException {

        final MetricRegistry registry = new MetricRegistry();
        final ConsoleReporter reporter = ConsoleReporter.forRegistry(registry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        final MemoryUsageGaugeSet ms = new MemoryUsageGaugeSet();
        registry.registerAll(ms);
        reporter.start(5, TimeUnit.SECONDS);
        Thread.sleep(60000L);
    }

    //@SigarTest
    public void testJvmGc() throws InterruptedException {
        final MetricRegistry registry = new MetricRegistry();
        final ConsoleReporter reporter = ConsoleReporter.forRegistry(registry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();

        final GarbageCollectorMetricSet gc = new GarbageCollectorMetricSet();
        registry.registerAll(gc);
        reporter.start(5, TimeUnit.SECONDS);
        Thread.sleep(20000L);
    }

    //@SigarTest
    public void testCPU() throws ShellCommandUsageException, ShellCommandExecException, SigarException {
        final Sigar sigar = new Sigar();

        final CpuPerc perc = sigar.getCpuPerc();
        System.out.println("整体cpu的占用情况:");
        System.out.println("空闲率: " + CpuPerc.format(perc.getIdle()));//获取当前cpu的空闲率
        System.out.println("占用率: " + CpuPerc.format(perc.getCombined()));//获取当前cpu的占用率

        final CpuInfo infos[] = sigar.getCpuInfoList();
        CpuPerc cpuList[] = null;
        cpuList = sigar.getCpuPercList();
        for (int i = 0; i < infos.length; i++) {// 不管是单块CPU还是多CPU都适用
            final CpuInfo info = infos[i];
            System.out.println("第" + (i + 1) + "块CPU信息");
            System.out.println("CPU的总量MHz:    " + info.getMhz());// CPU的总量MHz
            System.out.println("CPU生产商:    " + info.getVendor());// 获得CPU的卖主，如：Intel
            System.out.println("CPU类别:    " + info.getModel());// 获得CPU的类别，如：Celeron
            System.out.println("CPU缓存数量:    " + info.getCacheSize());// 缓冲存储器数量
            printCpuPerc(cpuList[i]);
        }
    }

    private static void printCpuPerc(final CpuPerc cpu) {
        System.out.println("CPU用户使用率:    " + CpuPerc.format(cpu.getUser()));// 用户使用率
        System.out.println("CPU系统使用率:    " + CpuPerc.format(cpu.getSys()));// 系统使用率
        System.out.println("CPU当前等待率:    " + CpuPerc.format(cpu.getWait()));// 当前等待率
        System.out.println("CPU当前错误率:    " + CpuPerc.format(cpu.getNice()));//
        System.out.println("CPU当前空闲率:    " + CpuPerc.format(cpu.getIdle()));// 当前空闲率
        System.out.println("CPU总的使用率:    " + CpuPerc.format(cpu.getCombined()));// 总的使用率
    }

    //@SigarTest
    public void testDf() throws InterruptedException {
        final MetricRegistry registry = new MetricRegistry();
        final ConsoleReporter reporter = ConsoleReporter.forRegistry(registry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        final DiskGaugeSet disk = new DiskGaugeSet(new SigarService());
        registry.registerAll(disk);
        reporter.start(5, TimeUnit.SECONDS);
        Thread.sleep(60000L);
    }

    //@SigarTest
    public void testNetworkInterfaces() throws ShellCommandUsageException, ShellCommandExecException, SigarException {
        final Sigar sigar = new Sigar();
        final String ifNames[] = sigar.getNetInterfaceList();
        for (final String name : ifNames) {
            final NetInterfaceConfig ifconfig = sigar.getNetInterfaceConfig(name);
            System.out.println("网络设备名:    " + name);// 网络设备名
            System.out.println("IP地址:    " + ifconfig.getAddress());// IP地址
            System.out.println("子网掩码:    " + ifconfig.getNetmask());// 子网掩码
            if ((ifconfig.getFlags() & 1L) <= 0L) {
                System.out.println("!IFF_UP...skipping getNetInterfaceStat");
                continue;
            }
            final NetInterfaceStat ifstat = sigar.getNetInterfaceStat(name);

            System.out.println(name + "接收的总包裹数:" + ifstat.getRxPackets());// 接收的总包裹数
            System.out.println(name + "发送的总包裹数:" + ifstat.getTxPackets());// 发送的总包裹数
            System.out.println(name + "接收到的总字节数:" + ifstat.getRxBytes());// 接收到的总字节数
            System.out.println(name + "发送的总字节数:" + ifstat.getTxBytes());// 发送的总字节数
            System.out.println(name + "接收到的错误包数:" + ifstat.getRxErrors());// 接收到的错误包数
            System.out.println(name + "发送数据包时的错误数:" + ifstat.getTxErrors());// 发送数据包时的错误数
            System.out.println(name + "接收时丢弃的包数:" + ifstat.getRxDropped());// 接收时丢弃的包数
            System.out.println(name + "发送时丢弃的包数:" + ifstat.getTxDropped());// 发送时丢弃的包数
        }
    }

    //@SigarTest
    public void testIostat() throws ShellCommandUsageException, ShellCommandExecException {
        final Iostat iostat = new Iostat();
        iostat.processCommand(new String[] {});
    }
}
