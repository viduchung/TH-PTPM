package com.football.data.security;

import com.football.data.auth.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("TH5 Integration Test: Chuỗi bảo mật Common Security, Filter JWT & Phân quyền RBAC")
public class CommonSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String customerToken;
    private String fieldOwnerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        customerToken = jwtTokenProvider.generateToken(101L, "nam_customer", Role.ROLE_CUSTOMER);
        fieldOwnerToken = jwtTokenProvider.generateToken(202L, "cuong_owner", Role.ROLE_FIELD_OWNER);
        adminToken = jwtTokenProvider.generateToken(999L, "hung_admin", Role.ROLE_ADMIN);
    }

    @Test
    @DisplayName("TH5 Integration 1: Truy cập API bảo mật không truyền Token -> Trả về 401 Unauthorized chuẩn RFC 7807")
    void testAccessSecuredEndpoint_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/secured/customer/profile")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.errorCode").value("ERR_UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/secured/customer/profile"))
                .andExpect(jsonPath("$.message").value("Yêu cầu xác thực không hợp lệ hoặc thiếu token Bearer hợp lệ"));
    }

    @Test
    @DisplayName("TH5 Integration 2: Truy cập với Token hết hạn (Expired) -> Bị từ chối 401 Unauthorized")
    void testAccessSecuredEndpoint_WithExpiredToken_Returns401() throws Exception {
        String expiredToken = jwtTokenProvider.generateToken(101L, "nam_customer", List.of("ROLE_CUSTOMER"), -10000L);

        mockMvc.perform(get("/api/secured/customer/profile")
                        .header("Authorization", "Bearer " + expiredToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errorCode").value("ERR_UNAUTHORIZED"));
    }

    @Test
    @DisplayName("TH5 Integration 3: Khách hàng (ROLE_CUSTOMER) truy cập thông tin cá nhân với Token hợp lệ -> 200 OK")
    void testCustomerAccessProfile_Success() throws Exception {
        mockMvc.perform(get("/api/secured/customer/profile")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(101))
                .andExpect(jsonPath("$.data.username").value("nam_customer"))
                .andExpect(jsonPath("$.data.accessScope").value("CUSTOMER_DATA"));
    }

    @Test
    @DisplayName("TH5 Integration 4: Khách hàng cố tình truy cập tài nguyên ADMIN -> Bị chặn 403 Forbidden chuẩn RFC 7807")
    void testCustomerAccessAdminEndpoint_Returns403Forbidden() throws Exception {
        mockMvc.perform(get("/api/secured/admin/users")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.errorCode").value("ERR_FORBIDDEN"))
                .andExpect(jsonPath("$.path").value("/api/secured/admin/users"))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền hạn thực hiện hành động này (Quyền truy cập bị từ chối)"));
    }

    @Test
    @DisplayName("TH5 Integration 5: Quản trị viên (ROLE_ADMIN) truy cập tài nguyên ADMIN -> 200 OK")
    void testAdminAccessAdminEndpoint_Success() throws Exception {
        mockMvc.perform(get("/api/secured/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.adminId").value(999))
                .andExpect(jsonPath("$.data.accessScope").value("SYSTEM_ADMINISTRATION"));
    }

    @Test
    @DisplayName("TH5 Integration 6: Chủ sân (ROLE_FIELD_OWNER) truy cập tài nguyên quản lý sân bóng -> 200 OK")
    void testFieldOwnerAccessPitches_Success() throws Exception {
        mockMvc.perform(get("/api/secured/field-owner/pitches")
                        .header("Authorization", "Bearer " + fieldOwnerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ownerId").value(202))
                .andExpect(jsonPath("$.data.accessScope").value("FIELD_OWNER_MANAGEMENT"));
    }

    @Test
    @DisplayName("TH5 Integration 7: Method Security @PreAuthorize('hasRole(\"ADMIN\")') từ chối Chủ sân với 403 Forbidden")
    void testMethodSecurity_DeniesNonAdmin_Returns403() throws Exception {
        mockMvc.perform(get("/api/secured/method-admin")
                        .header("Authorization", "Bearer " + fieldOwnerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.errorCode").value("ERR_FORBIDDEN"));
    }

    @Test
    @DisplayName("TH5 Integration 8: Endpoint /me trích xuất đúng thông tin UserPrincipal từ SecurityContext")
    void testCurrentUserEndpoint_ExtractsPrincipalCorrectly() throws Exception {
        mockMvc.perform(get("/api/secured/me")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(999))
                .andExpect(jsonPath("$.data.username").value("hung_admin"))
                .andExpect(jsonPath("$.data.roles", hasItem("ROLE_ADMIN")));
    }
}
