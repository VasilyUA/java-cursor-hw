package com.example.demo.integration;

import com.example.demo.dtos.student.input.CreateStudentDto;
import com.example.demo.dtos.student.output.StudentDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class StudentsControllerIntegrationTest {

    static final MySQLContainer<?> mySQLContainer = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.25"))
            .withDatabaseName("test")
            .withUsername("testuser")
            .withPassword("testpass");

    static {
        mySQLContainer.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    public void testCreateStudent() throws Exception {
        CreateStudentDto createStudentDto = new CreateStudentDto();
        createStudentDto.setEmail("test@example.com");
        createStudentDto.setPassword("Test1234");

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createStudentDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    public void testGetAllStudents() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testGetStudentById() throws Exception {
        CreateStudentDto createStudentDto = new CreateStudentDto();
        createStudentDto.setEmail("test3@example.com");
        createStudentDto.setPassword("Test1234");

        String studentJson = mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createStudentDto)))
                .andReturn().getResponse().getContentAsString();

        StudentDto createdStudent = objectMapper.readValue(studentJson, StudentDto.class);
        Long studentId = createdStudent.getId();

        mockMvc.perform(get("/api/students/{id}", studentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(studentId))
                .andExpect(jsonPath("$.email").value("test3@example.com"));
    }


    @Test
    public void testDeleteStudent() throws Exception {
        CreateStudentDto createStudentDto = new CreateStudentDto();
        createStudentDto.setEmail("test2@example.com");
        createStudentDto.setPassword("Test1234");

        String studentJson = mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createStudentDto)))
                .andReturn().getResponse().getContentAsString();

        StudentDto createdStudent = objectMapper.readValue(studentJson, StudentDto.class);
        Long studentId = createdStudent.getId();

        mockMvc.perform(delete("/api/students/{id}", studentId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/students/{id}", studentId))
                .andExpect(status().isNotFound());
    }
}
