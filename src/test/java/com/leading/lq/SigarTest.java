package com.leading.lq;

import org.hyperic.sigar.SigarException;

public class SigarTest {

    public static void main(final String[] args) throws SigarException, InterruptedException {
        //        final Sigar sigar = new Sigar();
        //        final FileSystem fslist[] = sigar.getFileSystemList();
        //        for (final FileSystem fs : fslist) {
        //            FileSystemUsage usage = null;
        //            usage = sigar.getFileSystemUsage(fs.getDirName());
        //            switch (fs.getType()) {
        //            case 0: // TYPE_UNKNOWN ：未知
        //                break;
        //            case 1: // TYPE_NONE
        //                break;
        //            case 2: // TYPE_LOCAL_DISK : 本地硬盘
        //                System.out.println("=============================================");
        //                // 分区的盘符名称
        //                System.out.println("盘符名称:    " + fs.getDevName());
        //                // 分区的盘符名称
        //                System.out.println("盘符路径:    " + fs.getDirName());
        //                // 文件系统总大小
        //                System.out.println(fs.getDevName() + "总大小:    " + usage.getTotal() + "KB");
        //                // 文件系统剩余大小
        //                System.out.println(fs.getDevName() + "剩余大小:    " + usage.getFree() + "KB");
        //                // 文件系统可用大小
        //                System.out.println(fs.getDevName() + "可用大小:    " + usage.getAvail() + "KB");
        //                // 文件系统已经使用量
        //                System.out.println(fs.getDevName() + "已经使用量:    " + usage.getUsed() + "KB");
        //                final double usePercent = usage.getUsePercent() * 100D;
        //                // 文件系统资源的利用率
        //                System.out.println(fs.getDevName() + "资源的利用率:    " + usePercent + "%");
        //                System.out.println(fs.getDevName() + "读出：    " + usage.getDiskReads());
        //                System.out.println(fs.getDevName() + "写入：    " + usage.getDiskWrites());
        //                break;
        //            case 3:// TYPE_NETWORK ：网络
        //                break;
        //            case 4:// TYPE_RAM_DISK ：闪存
        //                break;
        //            case 5:// TYPE_CDROM ：光驱
        //                break;
        //            case 6:// TYPE_SWAP ：页面交换
        //                break;
        //            }
        //        }
        //        final String ifNames[] = sigar.getNetInterfaceList();
        //        for (final String name : ifNames) {
        //            final NetInterfaceConfig ifconfig = sigar.getNetInterfaceConfig(name);
        //            System.out.println("网络设备名:    " + name);// 网络设备名
        //            System.out.println("IP地址:    " + ifconfig.getAddress());// IP地址
        //            System.out.println("子网掩码:    " + ifconfig.getNetmask());// 子网掩码
        //            if ((ifconfig.getFlags() & 1L) <= 0L) {
        //                System.out.println("!IFF_UP...skipping getNetInterfaceStat");
        //                continue;
        //            }
        //            final NetInterfaceStat ifstat = sigar.getNetInterfaceStat(name);
        //
        //            System.out.println(name + "接收的总包裹数:" + ifstat.getRxPackets());// 接收的总包裹数
        //            System.out.println(name + "发送的总包裹数:" + ifstat.getTxPackets());// 发送的总包裹数
        //            System.out.println(name + "接收到的总字节数:" + ifstat.getRxBytes());// 接收到的总字节数
        //            System.out.println(name + "发送的总字节数:" + ifstat.getTxBytes());// 发送的总字节数
        //            System.out.println(name + "接收到的错误包数:" + ifstat.getRxErrors());// 接收到的错误包数
        //            System.out.println(name + "发送数据包时的错误数:" + ifstat.getTxErrors());// 发送数据包时的错误数
        //            System.out.println(name + "接收时丢弃的包数:" + ifstat.getRxDropped());// 接收时丢弃的包数
        //            System.out.println(name + "发送时丢弃的包数:" + ifstat.getTxDropped());// 发送时丢弃的包数
        //        }
    		System.out.println(System.getProperty("java.library.path"));
    }

}
