package com.yyds.feng.op.controller;

import com.yyds.feng.op.OpApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/template")
public class TemplateDownloadController {

    /**
     * Optional override for the directory where template Excel files live.
     * - If relative, it's resolved against the jar directory.
     * - If empty, defaults to the jar directory.
     */
    @Value("${op.template.dir:}")
    private String templateDir;

    @GetMapping("/excel/{filename:.+}")
    public ResponseEntity<Resource> downloadExcel(@PathVariable String filename) {
        String safeName = toSafeFilename(filename);
        validateExcelExtension(safeName);

        Path baseDir = resolveBaseDir();
        Path filePath = baseDir.resolve(safeName).normalize();
        if (!filePath.startsWith(baseDir)) {
            throw new ResponseStatusException(BAD_REQUEST, "invalid filename");
        }

        Resource resource = new FileSystemResource(filePath.toFile());
        if (!resource.exists() || !resource.isReadable()) {
            throw new ResponseStatusException(NOT_FOUND, "file not found");
        }

        MediaType mediaType = contentTypeForExcel(safeName);
        ContentDisposition contentDisposition =
                ContentDisposition.attachment()
                        .filename(resource.getFilename(), StandardCharsets.UTF_8)
                        .build();

        ResponseEntity.BodyBuilder builder =
                ResponseEntity.ok()
                        .contentType(mediaType)
                        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());

        try {
            return builder.contentLength(resource.contentLength()).body(resource);
        } catch (Exception ignored) {
            return builder.body(resource);
        }
    }

    private String toSafeFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            throw new ResponseStatusException(BAD_REQUEST, "filename is required");
        }
        String trimmed = filename.trim();
        if (trimmed.contains("\\") || ".".equals(trimmed) || "..".equals(trimmed)) {
            throw new ResponseStatusException(BAD_REQUEST, "invalid filename");
        }
        String onlyName = Paths.get(trimmed).getFileName().toString();
        if (!onlyName.equals(trimmed)) {
            throw new ResponseStatusException(BAD_REQUEST, "invalid filename");
        }
        return onlyName;
    }

    private void validateExcelExtension(String filename) {
        String lower = filename.toLowerCase();
        if (!(lower.endsWith(".xlsx") || lower.endsWith(".xls"))) {
            throw new ResponseStatusException(BAD_REQUEST, "only .xls/.xlsx allowed");
        }
    }

    private MediaType contentTypeForExcel(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".xlsx")) {
            return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        }
        if (lower.endsWith(".xls")) {
            return MediaType.parseMediaType("application/vnd.ms-excel");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private Path resolveBaseDir() {
        Path jarDir = resolveJarDir();
        if (!StringUtils.hasText(templateDir)) {
            return jarDir;
        }
        Path configured = Paths.get(templateDir.trim());
        if (configured.isAbsolute()) {
            return configured.normalize();
        }
        return jarDir.resolve(configured).normalize();
    }

    private Path resolveJarDir() {
        ApplicationHome home = new ApplicationHome(OpApplication.class);
        File source = home.getSource();
        if (source != null) {
            File dir = source.isFile() ? source.getParentFile() : source;
            if (dir != null) {
                return dir.toPath().toAbsolutePath().normalize();
            }
        }
        return Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
    }
}

