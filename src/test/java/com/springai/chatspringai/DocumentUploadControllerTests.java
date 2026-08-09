package com.springai.chatspringai;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.springai.chatspringai.controller.DocumentUploadController;
import com.springai.chatspringai.service.RagIngestionService;

class DocumentUploadControllerTests {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DocumentUploadController controller = new DocumentUploadController(mock(RagIngestionService.class));
        ReflectionTestUtils.setField(controller, "maxFileSize", 50L * 1024 * 1024);
        ReflectionTestUtils.setField(controller, "allowedTypes", "pdf,docx,doc,txt");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void rejectsEmptyFiles() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", new byte[0]);

        mockMvc.perform(multipart("/api/documents/upload").file(file))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Le fichier est vide."));
    }

    @Test
    void rejectsUnsupportedFileExtensions() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "archive.exe", "application/octet-stream", "content".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload").file(file))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Ce format de fichier n’est pas accepté."));
    }
}
