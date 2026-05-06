package com.wetalk.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.*;
import java.util.Set;

import com.wetalk.VO.HttpResult;
import com.wetalk.VO.FileController.ImagesUploadVO;
import com.wetalk.ws.protocol.WsResult;
import com.wetalk.ws.Type.Chat.Content.*;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class FileService {
    private static final String BASE_URL = "http://120.48.156.237:90/static_resources/wetalk";
    private static final String AVATAR_DIR = "/var/www/static_resources/wetalk/avatar/";
    private static final String IMAGE_DIR = "/var/www/static_resources/wetalk/talk_image/";
    private static final String VIDEO_DIR = "/var/www/static_resources/wetalk/talk_video/";
    private static final String AUDIO_DIR = "/var/www/static_resources/wetalk/talk_audio/";
    private static final String FILE_DIR = "/var/www/static_resources/wetalk/talk_file/";

    private static final Set<String> ALLOWED_IMAGE_EXT = Set.of("png", "jpg", "jpeg", "webp", "gif", "bmp");
    private static final Set<String> ALLOWED_AUDIO_EXT = Set.of("mp3", "wav", "aac", "ogg", "flac", "m4a");
    private static final Set<String> ALLOWED_VIDEO_EXT = Set.of("mp4", "avi", "mov", "wmv", "flv", "mkv", "webm");
    private static final Set<String> ALLOWED_FILE_EXT = Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "zip", "rar", "7z");

    private void deleteOldImages(Path dir, String name) {
        try {
            // 遍历目录中的所有文件
            Files.list(dir)
                    .filter(Files::isRegularFile) // 只处理普通文件
                    .filter(path -> path.getFileName().toString().contains(name)) // 匹配包含 accountname 的文件名
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path); // 删除匹配的文件
                            System.out.println("Deleted file: " + path.getFileName());
                        } catch (Exception e) {
                            System.err.println("Failed to delete file: " + path.getFileName());
                            e.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            System.err.println("Error while listing files in directory: " + dir);
            e.printStackTrace();
        }
    }

    private String getExt(String name) {
        if (name == null)
            return "";
        int i = name.lastIndexOf('.');
        return i >= 0 ? name.substring(i + 1) : "";
    }

    // uploadAvatar:用于处理头像上传逻辑
    public HttpResult<Object> uploadAvatars(MultipartFile[] avatars, String[] names) {
        System.out.println("uploadImages: " + names);
        Integer len = avatars.length;
        String[] urls = new String[len];
        try {
            for (int i = 0; i < len; i++) {
                MultipartFile file = avatars[i];
                String name = names[i];
                if (file == null || file.isEmpty()) {
                    throw new RuntimeException("empty file");
                }

                if (name == null || name.isBlank()) {
                    throw new RuntimeException("not login");
                }

                // 只允许安全字符，避免路径穿越
                name = name.replaceAll("[^a-zA-Z0-9_-]", "");

                String ext = getExt(file.getOriginalFilename()).toLowerCase();
                if (!ALLOWED_IMAGE_EXT.contains(ext)) {
                    throw new RuntimeException("invalname ext");
                }

                Path dir = Paths.get(AVATAR_DIR);

                // ✅ 1) 先删除旧头像（不同后缀都删）
                deleteOldImages(dir, name);

                // ✅ 2) 新文件名固定
                String filename = name + '_' + System.currentTimeMillis() + "." + ext;
                Path target = dir.resolve(filename);

                // ✅ 3) 更稳：先写临时文件，再原子替换
                Path tmp = dir.resolve(name + ".uploading." + System.currentTimeMillis() + "." + ext);
                file.transferTo(tmp.toFile());
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

                String url = BASE_URL + "/avatar/" + filename;
                urls[i] = url;
            }
            return HttpResult.success(0, "上传成功", new ImagesUploadVO(urls));
        } catch (Exception e) {
            System.out.println("uploadAvatar:" + e);
            return HttpResult.success(1, "上传失败", null);
        }

    }

    // 上传聊天文件
    public HttpResult<Object> uploadMedia(MultipartFile media, Integer msgType, Long senderId, Long convId) {
        System.out.println("uploadMedia: " + msgType + " " + senderId + " " + convId);
        if (media == null || media.isEmpty() || msgType == null || senderId == null || convId == null) {
            return HttpResult.error(1, "上传失败", null);
        }
        Long size = media.getSize();
        String ext = getExt(media.getOriginalFilename()).toLowerCase();
        String filename = "conv_" + convId + "sender_" + senderId + '_' + System.currentTimeMillis() + "."
                + ext;
        Path dir = null;
        String url = BASE_URL;
        switch (msgType) {
            case 2:
                if (!ALLOWED_AUDIO_EXT.contains(ext)) {
                    return HttpResult.error(1, "上传失败", null);
                }
                if (size > 10 * 1024 * 1024) {
                    return HttpResult.error(1, "语音不能超过10MB", null);
                }
                dir = Paths.get(AUDIO_DIR);
                url += "/talk_audio/";
                break;
            case 3:
                if (!ALLOWED_IMAGE_EXT.contains(ext)) {
                    return HttpResult.error(1, "上传失败", null);
                }
                if (size > 20 * 1024 * 1024) {
                    return HttpResult.error(1, "图片不能超过20MB", null);
                }
                dir = Paths.get(IMAGE_DIR);
                url += "/talk_image/";
                break;
            case 4:
                if (!ALLOWED_VIDEO_EXT.contains(ext)) {
                    return HttpResult.error(1, "上传失败", null);
                }
                if (size > 200 * 1024 * 1024) {
                    return HttpResult.error(1, "视频不能超过200MB", null);
                }
                dir = Paths.get(VIDEO_DIR);
                url += "/talk_video/";
                break;
            case 5:
                if (!ALLOWED_FILE_EXT.contains(ext) && !ALLOWED_AUDIO_EXT.contains(ext)) {
                    return HttpResult.error(1, "上传失败", null);
                }
                if (size > 100 * 1024 * 1024) {
                    return HttpResult.error(1, "文件不能超过100MB", null);
                }
                url += "/talk_file/";
                dir = Paths.get(FILE_DIR);
                break;
            default:
                return HttpResult.error(1, "上传失败", null);
        }
        url += filename;
        try {
            Path target = dir.resolve(filename);
            Path tmp = dir.resolve(filename + ".uploading." + System.currentTimeMillis() + "." + ext);
            media.transferTo(tmp.toFile());
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return HttpResult.success(0, "上传成功", url);
        } catch (Exception e) {
            e.printStackTrace();
            return HttpResult.error(1, "上传失败", null);
        }
    }

}
