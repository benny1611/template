package com.benny1611.template.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProfileImageServiceTest {

    private ProfileImageService profileImageService;

    @TempDir
    Path tempDir; // JUnit 5 will create a fresh directory here for every test

    @BeforeEach
    void setUp() throws IOException {
        // Initialize the service using the temporary directory path
        profileImageService = new ProfileImageService(tempDir.toString());
    }

    private byte[] createTestImageBytes() throws IOException {
        // Create a 1x1 pixel image to act as a valid source
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    @Test
    @DisplayName("saveAsPng - Should resize and save image successfully")
    void saveAsPng_Success() throws IOException {
        // Arrange
        Long userId = 123L;
        byte[] imageBytes = createTestImageBytes();
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "original.png", "image/png", imageBytes);

        // Act
        String resultPath = profileImageService.saveAsPng(mockFile, userId);

        // Assert
        assertEquals("/users/123/avatar.png", resultPath);

        // Verify the file actually exists on the "disk"
        Path expectedFile = tempDir.resolve("123").resolve("avatar.png");
        assertTrue(Files.exists(expectedFile), "The file should be saved to the filesystem");

        // Verify it was resized to 256x256
        BufferedImage savedImage = ImageIO.read(expectedFile.toFile());
        assertEquals(256, savedImage.getWidth());
        assertEquals(256, savedImage.getHeight());
    }

    @Test
    @DisplayName("saveAsPng - Should throw exception for empty file")
    void saveAsPng_EmptyFile_ThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> {
            profileImageService.saveAsPng(emptyFile, 1L);
        });
    }

    @Test
    @DisplayName("saveAsPng - Should throw exception for non-image content type")
    void saveAsPng_InvalidType_ThrowsException() {
        MockMultipartFile textFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", "hello".getBytes());

        assertThrows(IllegalArgumentException.class, () -> {
            profileImageService.saveAsPng(textFile, 1L);
        });
    }

    @Test
    @DisplayName("saveAsPng - Should throw exception for corrupted image data")
    void saveAsPng_CorruptedData_ThrowsException() {
        // Valid image content type, but the "bytes" are just garbage text
        MockMultipartFile garbageFile = new MockMultipartFile(
                "file", "fake.png", "image/png", "not-an-image".getBytes());

        assertThrows(IllegalArgumentException.class, () -> {
            profileImageService.saveAsPng(garbageFile, 1L);
        });
    }
}
