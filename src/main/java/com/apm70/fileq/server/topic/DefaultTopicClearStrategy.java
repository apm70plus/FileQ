package com.apm70.fileq.server.topic;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.apm70.fileq.util.Destroyable;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultTopicClearStrategy implements TopicClearStrategy, Destroyable {
    /** 订阅消息存储的根目录 */
    private final String topicStoreDir;
    /** 订阅消息保留的分片数（每片65536条消息） */
    private final int keepAliveFragments;
    /** 订阅消息保留的时长（小时数） */
    private final int keepAliveHours;

    private final Timer cleanTimer;

    public DefaultTopicClearStrategy(
            final String topicStoreDir,
            final int keepAliveFragments,
            final int keepAliveHours) {
        this.topicStoreDir = topicStoreDir;
        this.keepAliveFragments = keepAliveFragments;
        this.keepAliveHours = keepAliveHours;
        this.cleanTimer = new Timer("TopicClearThread");
    }

    public void start() {
        this.cleanTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    DefaultTopicClearStrategy.this.clear();
                } catch (final Throwable e) {
                    DefaultTopicClearStrategy.log.error("订阅消息清理时发生异常", e);
                }
            }

        }, 60000L, 60000L * 60 * 24);
    }

    @Override
    public void clear() {
        final File root = new File(this.topicStoreDir);
        if (!root.exists()) {
            return;
        }
        final List<File> topicFiles = this.getTopicFiles(root);
        int index = 0;
        // 删除多余的文件
        if (this.keepAliveFragments > 0) {
			int endIndex = topicFiles.size() - (this.keepAliveFragments * 2);
			for (; index < endIndex; index++) {
				topicFiles.get(index).delete();
			}
        }
        if (this.keepAliveHours <= 0) {
            return;
        }
        // 删除超时的文件
        final long keepAliveMillis = this.keepAliveHours * 3600000L;
        for (; index < topicFiles.size(); index++) {
            if ((System.currentTimeMillis() - topicFiles.get(index).lastModified()) > keepAliveMillis) {
                topicFiles.get(index).delete();
            } else {
                break;
            }
        }
        // 删除空目录
        deleteEmptyDir(root);
    }

    private void deleteEmptyDir(File dir) {
    		final File[] childDirs = dir.listFiles((FileFilter) f -> f.isDirectory());
    		for(File file : childDirs) {
    			if (file.isDirectory()) {
    				if (file.listFiles().length == 0) {
    					file.delete();
    				} else {
    					deleteEmptyDir(file);
    				}
    			}
    		}
    }
    
    private List<File> getTopicFiles(final File topicDir) {
        final File[] level1Dirs = topicDir.listFiles((FileFilter) f -> f.isDirectory());
        final List<File> allFiles = new ArrayList<>();
        for (final File level1Dir : level1Dirs) {
            for (final File level2Dir : level1Dir.listFiles()) {
                for (final File storeFile : level2Dir.listFiles()) {
                    allFiles.add(storeFile);
                }
            }
        }
        // 排序
        allFiles.sort((a, b) -> {
            final long v = a.lastModified() - b.lastModified();
            if (v == 0) {
                return 0;
            } else if (v > 0) {
                return 1;
            } else {
                return -1;
            }
        });
        return allFiles;
    }

    @Override
    public void destroy() {
        this.cleanTimer.cancel();
    }
}
