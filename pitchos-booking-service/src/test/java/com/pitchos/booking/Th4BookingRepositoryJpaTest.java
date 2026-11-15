package com.pitchos.booking;

import com.pitchos.booking.entity.Booking;
import com.pitchos.booking.entity.BookingStatus;
import com.pitchos.booking.repository.BookingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TH4 - Tầng JPA trên H2 (chế độ MySQL): mapping Entity, ghi/đọc CSDL, truy vấn trùng lịch, ràng buộc UNIQUE,
 * index và khóa lạc quan. LƯU Ý: đây là H2, KHÔNG phải MySQL thật.
 */
@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class Th4BookingRepositoryJpaTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2030, 1, 10, 18, 0);

    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Lưu và đọc lại đúng mọi cột (logical ID user_id, pitch_id; giá; trạng thái; thời điểm tạo/cập nhật)")
    void saveAndReload_mapsAllColumns() {
        Booking saved = bookingRepository.saveAndFlush(booking(3L, 1L, BASE, BASE.plusMinutes(90), BookingStatus.cho_duyet));
        entityManager.clear();

        Booking reloaded = bookingRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getUserId()).isEqualTo(3L);
        assertThat(reloaded.getPitchId()).isEqualTo(1L);
        assertThat(reloaded.getStartTime()).isEqualTo(BASE);
        assertThat(reloaded.getEndTime()).isEqualTo(BASE.plusMinutes(90));
        assertThat(reloaded.getTotalPrice()).isEqualByComparingTo("450000.00");
        assertThat(reloaded.getPricePerHour()).isEqualByComparingTo("300000.00");
        assertThat(reloaded.getStatus()).isEqualTo(BookingStatus.cho_duyet);
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();
        assertThat(reloaded.getVersion()).isZero();
    }

    @Test
    @DisplayName("Trùng lịch: chồng giờ → tính; liền kề → không tính; tu_choi và da_huy → bỏ qua; sân khác → không tính")
    void overlapQuery_followsBusinessRules() {
        bookingRepository.saveAndFlush(booking(3L, 1L, BASE, BASE.plusHours(1), BookingStatus.da_duyet));
        bookingRepository.saveAndFlush(booking(3L, 1L, BASE.plusHours(2), BASE.plusHours(3), BookingStatus.tu_choi));
        bookingRepository.saveAndFlush(booking(3L, 1L, BASE.plusHours(4), BASE.plusHours(5), BookingStatus.da_huy));

        assertThat(bookingRepository.countOverlappingBookings(1L, BASE.plusMinutes(30), BASE.plusMinutes(90))).isEqualTo(1);
        assertThat(bookingRepository.countOverlappingBookings(1L, BASE.plusHours(1), BASE.plusHours(2))).isZero();
        assertThat(bookingRepository.countOverlappingBookings(1L, BASE.plusHours(2), BASE.plusHours(3))).isZero();
        assertThat(bookingRepository.countOverlappingBookings(1L, BASE.plusHours(4), BASE.plusHours(5))).isZero();
        assertThat(bookingRepository.countOverlappingBookings(2L, BASE, BASE.plusHours(1))).isZero();
        assertThat(bookingRepository.findOverlappingBookings(1L, BASE.minusHours(1), BASE.plusHours(6))).hasSize(1);
    }

    @Test
    @DisplayName("Kiểm tra trùng khi sửa đơn bỏ qua chính đơn đó")
    void overlapExcludingSelf() {
        Booking own = bookingRepository.saveAndFlush(booking(3L, 1L, BASE, BASE.plusHours(1), BookingStatus.cho_duyet));

        assertThat(bookingRepository.countOverlappingBookingsExcluding(1L, BASE, BASE.plusMinutes(90), own.getId()))
                .isZero();
        assertThat(bookingRepository.countOverlappingBookings(1L, BASE, BASE.plusMinutes(90))).isEqualTo(1);
    }

    @Test
    @DisplayName("UNIQUE (user_id, idempotency_key): cùng khách + cùng key → lỗi; khác khách hoặc key NULL → được")
    void idempotencyUniqueConstraint() {
        Booking first = booking(3L, 1L, BASE, BASE.plusHours(1), BookingStatus.cho_duyet);
        first.setIdempotencyKey("key-123");
        bookingRepository.saveAndFlush(first);

        Booking otherUserSameKey = booking(4L, 2L, BASE, BASE.plusHours(1), BookingStatus.cho_duyet);
        otherUserSameKey.setIdempotencyKey("key-123");
        bookingRepository.saveAndFlush(otherUserSameKey);
        bookingRepository.saveAndFlush(booking(3L, 2L, BASE.plusHours(2), BASE.plusHours(3), BookingStatus.cho_duyet));
        bookingRepository.saveAndFlush(booking(3L, 2L, BASE.plusHours(4), BASE.plusHours(5), BookingStatus.cho_duyet));

        Booking duplicate = booking(3L, 2L, BASE.plusHours(6), BASE.plusHours(7), BookingStatus.cho_duyet);
        duplicate.setIdempotencyKey("key-123");
        assertThatThrownBy(() -> bookingRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Schema (đọc qua JDBC DatabaseMetaData, chạy được trên H2 và MySQL): 3 index đúng cột, UNIQUE idempotency, PK pitch_locks, KHÔNG khóa ngoại")
    void schemaHasIndexesAndNoForeignKeys() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String bookings = resolveTable(connection, "bookings");
            String pitchLocks = resolveTable(connection, "pitch_locks");
            Map<String, IndexDef> indexes = indexesOf(connection, bookings);

            // Index thường: đúng tên, đúng cột THEO THỨ TỰ, không phải unique
            assertThat(indexes.get("idx_bookings_pitch_time"))
                    .isEqualTo(new IndexDef(List.of("pitch_id", "start_time", "end_time"), false));
            assertThat(indexes.get("idx_bookings_user_created"))
                    .isEqualTo(new IndexDef(List.of("user_id", "created_at"), false));
            assertThat(indexes.get("idx_bookings_status")).isEqualTo(new IndexDef(List.of("status"), false));

            // UNIQUE (user_id, idempotency_key): MySQL đặt tên index = tên ràng buộc; H2 thêm hậu tố "_INDEX_x"
            assertThat(indexes.entrySet())
                    .filteredOn(e -> e.getKey().startsWith("uk_bookings_user_idempotency"))
                    .extracting(Map.Entry::getValue)
                    .containsExactly(new IndexDef(List.of("user_id", "idempotency_key"), true));

            // Không có khóa ngoại nào (Database-per-Service), theo cả hai chiều, ở cả 2 bảng
            for (String table : List.of(bookings, pitchLocks)) {
                assertThat(countRows(metaData.getImportedKeys(connection.getCatalog(), connection.getSchema(), table)))
                        .as("khóa ngoại trỏ ra từ " + table).isZero();
                assertThat(countRows(metaData.getExportedKeys(connection.getCatalog(), connection.getSchema(), table)))
                        .as("khóa ngoại trỏ vào " + table).isZero();
            }

            // Bảng khóa theo sân: khóa chính duy nhất là pitch_id
            assertThat(primaryKeyColumns(connection, pitchLocks)).containsExactly("pitch_id");
        }
    }

    /** Định nghĩa index đọc từ JDBC DatabaseMetaData: danh sách cột theo thứ tự và tính duy nhất. */
    private record IndexDef(List<String> columns, boolean unique) {
    }

    /** Tên bảng thực tế trong CSDL (H2 viết hoa, MySQL giữ nguyên) để dùng chung một test cho H2 và MySQL. */
    private static String resolveTable(Connection connection, String logicalName) throws SQLException {
        try (ResultSet tables = connection.getMetaData()
                .getTables(connection.getCatalog(), connection.getSchema(), "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String name = tables.getString("TABLE_NAME");
                if (name.equalsIgnoreCase(logicalName)) {
                    return name;
                }
            }
        }
        throw new AssertionError("Không tìm thấy bảng " + logicalName);
    }

    private static Map<String, IndexDef> indexesOf(Connection connection, String table) throws SQLException {
        Map<String, TreeMap<Short, String>> columns = new HashMap<>();
        Map<String, Boolean> unique = new HashMap<>();
        try (ResultSet rs = connection.getMetaData()
                .getIndexInfo(connection.getCatalog(), connection.getSchema(), table, false, false)) {
            while (rs.next()) {
                String name = rs.getString("INDEX_NAME");
                if (name == null) {
                    continue; // dòng thống kê của bảng, không phải index
                }
                String key = name.toLowerCase(Locale.ROOT);
                columns.computeIfAbsent(key, k -> new TreeMap<>())
                        .put(rs.getShort("ORDINAL_POSITION"), rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                unique.put(key, !rs.getBoolean("NON_UNIQUE"));
            }
        }
        Map<String, IndexDef> result = new HashMap<>();
        columns.forEach((name, cols) -> result.put(name, new IndexDef(List.copyOf(cols.values()), unique.get(name))));
        return result;
    }

    private static List<String> primaryKeyColumns(Connection connection, String table) throws SQLException {
        TreeMap<Short, String> columns = new TreeMap<>();
        try (ResultSet rs = connection.getMetaData()
                .getPrimaryKeys(connection.getCatalog(), connection.getSchema(), table)) {
            while (rs.next()) {
                columns.put(rs.getShort("KEY_SEQ"), rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
        }
        return List.copyOf(columns.values());
    }

    private static int countRows(ResultSet resultSet) throws SQLException {
        try (resultSet) {
            int rows = 0;
            while (resultSet.next()) {
                rows++;
            }
            return rows;
        }
    }

    @Test
    @DisplayName("Khóa lạc quan: version tăng sau mỗi lần cập nhật")
    void versionIncrementsOnUpdate() {
        Booking saved = bookingRepository.saveAndFlush(booking(3L, 1L, BASE, BASE.plusHours(1), BookingStatus.cho_duyet));
        saved.setStatus(BookingStatus.da_duyet);
        Booking updated = bookingRepository.saveAndFlush(saved);
        assertThat(updated.getVersion()).isEqualTo(1);
    }

    private static Booking booking(Long userId, Long pitchId, LocalDateTime start, LocalDateTime end,
                                   BookingStatus status) {
        return Booking.builder()
                .userId(userId)
                .pitchId(pitchId)
                .startTime(start)
                .endTime(end)
                .pricePerHour(new BigDecimal("300000.00"))
                .totalPrice(new BigDecimal("450000.00"))
                .status(status)
                .build();
    }
}
