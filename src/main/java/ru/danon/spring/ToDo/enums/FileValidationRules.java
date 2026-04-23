package ru.danon.spring.ToDo.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum FileValidationRules {

    MAX_FILE_SIZE(10 * 1024 * 1024);

    private final long value;

    public String getReadableSize() {
        return (value / (1024 * 1024)) + " MB";
    }

    // Опасные расширения
    public static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
            ".exe", ".bat", ".cmd", ".sh", ".bin", ".app", ".jar",
            ".msi", ".com", ".scr", ".pif", ".application", ".gadget",
            ".msc", ".msp", ".hta", ".cpl", ".msh", ".msh1", ".msh2",
            ".mshxml", ".msh1xml", ".msh2xml", ".ps1", ".ps1xml", ".ps2",
            ".ps2xml", ".psc1", ".psc2", ".scf", ".lnk", ".inf", ".reg"
    );

    // Опасные MIME-типы
    public static final Set<String> DANGEROUS_MIME_TYPES = Set.of(
            "application/x-msdownload",
            "application/x-ms-installer",
            "application/x-dosexec",
            "application/x-executable",
            "application/x-shellscript"
    );

    public static boolean isExtensionDangerous(String extension) {
        return DANGEROUS_EXTENSIONS.contains(extension.toLowerCase());
    }

    public static boolean isMimeTypeDangerous(String mimeType) {
        if (mimeType == null) return false;
        return DANGEROUS_MIME_TYPES.stream()
                .anyMatch(mimeType.toLowerCase()::contains);
    }
}