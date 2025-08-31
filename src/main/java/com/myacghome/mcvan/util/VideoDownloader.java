package com.myacghome.mcvan.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static com.myacghome.mcvan.IndexMod.GAME_ROOT_PATH;

public class VideoDownloader {
    private String Url;
    private String FileName;

    public VideoDownloader(int resolutionX,int resolutionY,String url,byte widthBlocks,byte heightBlocks) {
        this.Url = url;
        this.FileName = ToVideoProperty2FileName.VideoProperty2FileName(resolutionX,resolutionY,url,widthBlocks,heightBlocks);
    }

    public void downloadAndProcessVideo() {
        try {
            Path storeVanPath = GAME_ROOT_PATH.resolve("storeVan");
            Files.createDirectories(storeVanPath); // 确保目录存在
            File destinationFile = storeVanPath.resolve(FileName).toFile();
            // 下载文件
            FileDownloader.downloadFile(Url, destinationFile);
            System.out.println("[VideoDownloader] Video downloaded to: " + destinationFile.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("[VideoDownloader] Error downloading or processing video: " + e.getMessage());
            e.printStackTrace();
        }
    }
}