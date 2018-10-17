package com.apm70.fileq.server.topic;

import java.io.File;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;

class TopicFragmentIndex {
    public static final int CAPACITY = 0x00010000;
    @Getter
    private final long start;

    public TopicFragmentIndex(final long index) {
        this.start = (index >> 16) << 16;
    }

    public boolean contains(final long index) {
        return this.start == ((index >> 16) << 16);
    }

    public String toPath() {
        final ByteBuf longBuf = Unpooled.buffer(8);
        longBuf.writeLong(this.start);
        final String path = File.separator + longBuf.getUnsignedShort(0)
                + File.separator + longBuf.getUnsignedShort(2)
                + File.separator + longBuf.getUnsignedShort(4);
        longBuf.release();
        return path;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(this.start);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof TopicFragmentIndex)) {
            return false;
        }
        final TopicFragmentIndex key = (TopicFragmentIndex) obj;
        return this.start == key.start;
    }

    public long nextFragmentIndex() {
        return this.start + TopicFragmentIndex.CAPACITY;
    }

    //    public static void main(String[] args) {
    //    	    long v = Long.valueOf("000000ff00ffffff", 16);
    //    	    TopicFragmentKey key = new TopicFragmentKey(v);
    //    	    System.out.println(key.toPath());
    //    	    System.out.println();
    //    	    System.out.println(key.nextFragment().toPath());
    //    }
}
