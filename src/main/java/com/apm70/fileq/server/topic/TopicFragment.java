package com.apm70.fileq.server.topic;

import java.io.File;
import java.io.IOException;

import com.apm70.fileq.util.IList;
import com.apm70.fileq.util.PersistanceList;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class TopicFragment {

    @Getter
    private TopicFragmentIndex fragmentIndex;
    private String storeDir;
    private IList<TopicMessageBean> fragmentList;

    public TopicFragment(final String storeDir, final long index) throws IOException {
        this.storeDir = storeDir;
        this.fragmentIndex = new TopicFragmentIndex(index);
        final String path = this.storeDir + this.fragmentIndex.toPath();
        if (new File(path + ".index").exists()) {// 文件存在，加载数据文件
            this.build();
        }
    }

    public boolean exists() {
        return this.fragmentList != null;
    }

    public void build() throws IOException {
        if (this.fragmentList == null) {
            final String path = this.storeDir + this.fragmentIndex.toPath();
            final File indexFile = new File(path + ".index");
            final File dataFile = new File(path + ".data");
            if (!dataFile.getParentFile().exists()) {
                dataFile.getParentFile().mkdirs();
            }
            this.fragmentList = new PersistanceList<>(indexFile, dataFile, TopicMessageBean.class);
        }
    }

    public void add(final TopicMessageBean bean) {
        if (this.isFull()) {
            throw new IndexOutOfBoundsException();
        }
        this.fragmentList.add(bean);
    }

    public TopicMessageBean get(final long index) {
        if (!this.contains(index)) {
            throw new IndexOutOfBoundsException();
        }
        final int listIndex = (int) index & 0xFFFF;
        return this.fragmentList.get(listIndex);
    }

    public boolean isFull() {
        return this.fragmentList.size() >= TopicFragmentIndex.CAPACITY;
    }

    public TopicFragment nextFragment() throws IOException {
        return new TopicFragment(this.storeDir, this.fragmentIndex.nextFragmentIndex());
    }

    public boolean contains(final long index) {
        return this.fragmentIndex.contains(index);
    }
}
