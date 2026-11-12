package com.pradyumnan.bpm_service.service;

import java.io.IOException;

/**
 * Where uploaded documents live. Today that is a local folder; on free hosting, whose disks are
 * wiped on restart, this is the one place to swap in cloud storage.
 */
public interface FileStorage {

    /** Stores the bytes and returns the key to load them back with. */
    String save(byte[] content, String extension) throws IOException;

    byte[] load(String key) throws IOException;
}
