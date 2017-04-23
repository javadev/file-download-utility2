package com.example.mywork.model;

public interface AllFilesTransferCallback {
    void print(int percent, long numFiles, long bytes, long time, long speed);
}
