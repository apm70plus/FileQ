package com.apm70.fileq.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SigarService {

    private final Sigar sigar = new Sigar();

    private CpuPerc cachedCpuPerc;
    private long cpuPercCachedTime;

    private final Cache<String, FileSystemUsage> fsCache =
            CacheBuilder.newBuilder().expireAfterWrite(3, TimeUnit.SECONDS).build();

    private final Cache<String, NetInterfaceStat> ifCache =
            CacheBuilder.newBuilder().expireAfterWrite(3, TimeUnit.SECONDS).build();

    /**
     * 获取本地磁盘列表
     *
     * @return
     * @throws SigarException
     */
    public List<FileSystem> getLocalDisks() throws SigarException {
        final FileSystem[] fslist = this.sigar.getFileSystemList();
        return Stream.of(fslist).filter(fs -> fs.getType() == 2).collect(Collectors.toList());
    }

    /**
     * 获取磁盘总容量（KB）
     *
     * @param dirName
     * @return
     */
    public double getDiskTotalKb(final FileSystem fs) {
        final FileSystemUsage usage = this.getDiskUsage(fs);
        return usage != null ? usage.getTotal() : 0.0D;
    }

    /**
     * 获取磁盘剩余容量（KB）
     *
     * @param dirName
     * @return
     */
    public double getDiskFreeKb(final FileSystem fs) {
        final FileSystemUsage usage = this.getDiskUsage(fs);
        return usage != null ? usage.getFree() : 0.0D;
    }

    /**
     * 获取磁盘使用百分比
     *
     * @param dirName
     * @return
     */
    public double getDiskUsePercent(final FileSystem fs) {
        final FileSystemUsage usage = this.getDiskUsage(fs);
        return usage != null ? usage.getUsePercent() : 0.0D;
    }

    /**
     * 获取磁盘文件系统使用情况
     *
     * @param dirName
     * @return
     */
    public FileSystemUsage getDiskUsage(final FileSystem fs) {
        try {
            final String dirName = fs.getDirName();
            return this.fsCache.get(dirName, () -> {
                return this.sigar.getFileSystemUsage(dirName);
            });
        } catch (final ExecutionException e) {
            log.error("获取磁盘使用情况失败", e);
            return null;
        }
    }

    /**
     * 获取CPU使用情况（所有CPU汇总）
     *
     * @return
     * @throws SigarException
     */
    public CpuPerc getCpuPerc() throws SigarException {
        if ((this.cachedCpuPerc == null) || ((System.currentTimeMillis() - this.cpuPercCachedTime) > 3000L)) {
            this.cachedCpuPerc = this.sigar.getCpuPerc();
            this.cpuPercCachedTime = System.currentTimeMillis();
        }
        return this.cachedCpuPerc;
    }

    public List<String> getNetInterfaces() throws SigarException {
        final List<String> effectiveInterfaces = new ArrayList<>();
        final String[] ifNames = this.sigar.getNetInterfaceList();
        for (final String n : ifNames) {
            final NetInterfaceConfig ifconfig = this.sigar.getNetInterfaceConfig(n);
            if ("0.0.0.0".equals(ifconfig.getAddress()) || "127.0.0.1".equals(ifconfig.getAddress())) {
                continue;
            }
            effectiveInterfaces.add(n);
        }
        return effectiveInterfaces;
    }

    public NetInterfaceStat getNetIfStat(final String ifName) {
        try {
            return this.ifCache.get(ifName, () -> {
                return this.sigar.getNetInterfaceStat(ifName);
            });
        } catch (final ExecutionException e) {
            log.error("获取网卡流量信息失败", e);
            return null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.sigar.close();
    }
}
