package ru.danon.spring.ToDo.mappers;

import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import ru.danon.spring.ToDo.dto.TaskFileDTO;
import ru.danon.spring.ToDo.models.postgre.TaskFile;

import static org.junit.jupiter.api.Assertions.*;

class TaskFileMapperTest {

    private final TaskFileMapper taskFileMapper = new TaskFileMapper(new ModelMapper());

    @Test
    void toDTOShouldMapCoreFields() {
        TaskFile file = new TaskFile();
        file.setId(5L);
        file.setOriginalFileName("solution.pdf");
        file.setFileSize(1024L);
        file.setFileType("application/pdf");

        TaskFileDTO dto = taskFileMapper.toDTO(file);

        assertEquals(5L, dto.getId());
        assertEquals("solution.pdf", dto.getOriginalFileName());
        assertEquals(1024L, dto.getFileSize());
        assertEquals("application/pdf", dto.getFileType());
    }
}
