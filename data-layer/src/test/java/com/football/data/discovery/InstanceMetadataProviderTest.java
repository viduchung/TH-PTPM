package com.football.data.discovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("TH7 Unit Test: Nhà cung cấp siêu dữ liệu định danh Instance (InstanceMetadataProvider)")
public class InstanceMetadataProviderTest {

    @Autowired
    private InstanceMetadataProvider metadataProvider;

    @Test
    @DisplayName("TH7 Test 1: Khởi tạo Instance ID duy nhất và có ý nghĩa")
    void testInstanceId_NotNullAndValid() {
        String instanceId = metadataProvider.getInstanceId();

        assertNotNull(instanceId);
        assertFalse(instanceId.isBlank());
        assertTrue(instanceId.contains(metadataProvider.getServiceName()));
        assertNotNull(metadataProvider.getHostAddress());
        assertTrue(metadataProvider.getPort() > 0);
    }

    @Test
    @DisplayName("TH7 Test 2: Header X-Served-By-Instance định dạng chuẩn phục vụ kiểm tra Cân bằng tải")
    void testServingHeader_Format() {
        String header = metadataProvider.getServingHeader();

        assertNotNull(header);
        assertTrue(header.contains(metadataProvider.getServiceName()));
        assertTrue(header.contains(String.valueOf(metadataProvider.getPort())));
        assertTrue(header.contains(metadataProvider.getInstanceId()));
    }

    @Test
    @DisplayName("TH7 Test 3: Trích xuất Map siêu dữ liệu chứa đầy đủ thông số quản trị")
    void testGetMetadataMap_ContainsAllKeys() {
        Map<String, Object> map = metadataProvider.getMetadataMap();

        assertNotNull(map);
        assertTrue(map.containsKey("instanceId"));
        assertTrue(map.containsKey("serviceName"));
        assertTrue(map.containsKey("port"));
        assertTrue(map.containsKey("hostAddress"));
        assertTrue(map.containsKey("startedAt"));
        assertTrue(map.containsKey("activeProfiles"));
    }
}

