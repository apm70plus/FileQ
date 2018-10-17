package com.apm70.fileq.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.apm70.fileq.config.Constants;

public class MD5Utils {

    private static final char[] HEX_CHARS =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static byte[] getHash(final File file) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            final ByteBuffer bf = ByteBuffer.allocate(1024 * 1024);
            bf.putLong(file.length());
            bf.flip();
            digest.update(bf);
            bf.clear();
            try (RandomAccessFile md5File = new RandomAccessFile(file, "r")) {
                final FileChannel channel = md5File.getChannel();
                while (channel.read(bf) > 0) {
                    bf.flip();
                    digest.update(bf);
                    bf.clear();
                }
            }
            return digest.digest();
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not find MessageDigest with algorithm \"" + "MD5" + "\"", e);
        } catch (final IOException e) {
            throw new IllegalStateException("Read file error", e);
        }
    }

    public static byte[] signature(final String content, final String secretKey, final long random) {
        final List<String> sortData = new ArrayList<>();
        sortData.add(content);
        sortData.add(secretKey);
        sortData.add(String.valueOf(random));
        Collections.sort(sortData);
        final StringBuilder builder = new StringBuilder();
        for (final String value : sortData) {
            builder.append(value);
        }
        try {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            return digest.digest(builder.toString().getBytes(Constants.defaultCharset));
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not find MessageDigest with algorithm \"" + "MD5" + "\"", e);
        }
    }

    public static boolean verifySignature(final byte[] signature, final String content, final String secretKey,
            final long random) {
        final byte[] value = signature(content, secretKey, random);
        for (int i = 0; i < value.length; i++) {
            if (value[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    public static String encodeHex(final byte[] bytes) {
        final char chars[] = new char[bytes.length * 2];
        for (int i = 0; i < chars.length; i = i + 2) {
            final byte b = bytes[i / 2];
            chars[i] = MD5Utils.HEX_CHARS[(b >>> 0x4) & 0xf];
            chars[i + 1] = MD5Utils.HEX_CHARS[b & 0xf];
        }
        return new String(chars);
    }

    public static byte[] decodeHex(final String hexStr) {
        final char chars[] = hexStr.toCharArray();
        final byte[] bytes = new byte[chars.length / 2];
        for (int i = 0; i < chars.length; i = i + 2) {

            final int high = MD5Utils.hexCharToInt(chars[i]);
            final int low = MD5Utils.hexCharToInt(chars[i + 1]);
            bytes[i / 2] = (byte) ((high << 0x4) | (low & 0xff));
        }
        return bytes;
    }

    public static int hexCharToInt(final char hex) {
        if (hex <= '9') {
            return hex - '0';
        } else {
            return (hex - 'a') + 10;
        }
    }
}
