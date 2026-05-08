package com.benny1611.template.service;

import com.benny1611.template.util.ByteArrayMultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ProfileImageService {

    private final Path uploadPath;

    public ProfileImageService(@Value("${upload.directory}") String uploadDirString) throws IOException {
        uploadPath = Files.createDirectories(Paths.get(uploadDirString));
    }

    private void validateImage(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Empty file");
        }

        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image. Actual content type: " + file.getContentType());
        }

        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            throw new IllegalArgumentException("Invalid or corrupted image");
        }
    }

    public String saveAsPng(MultipartFile file, Long id) throws IOException {
        validateImage(file);
        Path outputPath = uploadPath.resolve(String.valueOf(id)).resolve("avatar.png");
        Files.createDirectories(outputPath.getParent());
        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            throw new IllegalArgumentException("Corrupted PNG file");
        }

        BufferedImage resized = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        g.drawImage(image, 0, 0, 256, 256, null);
        g.dispose();
        ImageIO.write(resized, "png", outputPath.toFile());
        return "/users/" + id + "/avatar.png";
    }

    public String saveAsPng(byte[] imageBytes, Long id) throws IOException {
        ByteArrayMultipartFile file = new ByteArrayMultipartFile(imageBytes);
        file.setContentType("image/png");
        return saveAsPng(file, id);
    }

    public static byte[] downloadImage(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to download the image");
            }
            return response.body();
        }
    }
}
