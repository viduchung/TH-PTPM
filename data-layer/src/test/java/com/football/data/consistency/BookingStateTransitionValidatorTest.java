package com.football.data.consistency;

import com.football.data.booking.entity.BookingStatus;
import com.football.data.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TH6 Unit Test: Trình kiểm soát nhất quán trạng thái (BookingStateTransitionValidator)")
public class BookingStateTransitionValidatorTest {

    private BookingStateTransitionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new BookingStateTransitionValidator();
    }

    @Test
    @DisplayName("TH6 Test 1: Các bước chuyển đổi trạng thái hợp lệ theo quy trình")
    void testValidTransitions_Success() {
        // CHO_DUYET -> DA_DUYET
        assertTrue(validator.canTransition(BookingStatus.CHO_DUYET, BookingStatus.DA_DUYET));
        assertDoesNotThrow(() -> validator.validateTransition(BookingStatus.CHO_DUYET, BookingStatus.DA_DUYET));

        // CHO_DUYET -> TU_CHOI
        assertTrue(validator.canTransition(BookingStatus.CHO_DUYET, BookingStatus.TU_CHOI));
        assertDoesNotThrow(() -> validator.validateTransition(BookingStatus.CHO_DUYET, BookingStatus.TU_CHOI));

        // CHO_DUYET -> DA_HUY
        assertTrue(validator.canTransition(BookingStatus.CHO_DUYET, BookingStatus.DA_HUY));
        assertDoesNotThrow(() -> validator.validateTransition(BookingStatus.CHO_DUYET, BookingStatus.DA_HUY));

        // DA_DUYET -> DA_HUY (khi có bù trừ hoàn tiền)
        assertTrue(validator.canTransition(BookingStatus.DA_DUYET, BookingStatus.DA_HUY));
        assertDoesNotThrow(() -> validator.validateTransition(BookingStatus.DA_DUYET, BookingStatus.DA_HUY));

        // Giữ nguyên trạng thái (Idempotent state)
        assertTrue(validator.canTransition(BookingStatus.DA_DUYET, BookingStatus.DA_DUYET));
    }

    @Test
    @DisplayName("TH6 Test 2: Các bước chuyển đổi phi lý bị từ chối bằng ConflictException")
    void testInvalidTransitions_ThrowsConflictException() {
        // TU_CHOI -> DA_DUYET (Đã từ chối thì không thể tự ý chuyển sang duyệt)
        assertFalse(validator.canTransition(BookingStatus.TU_CHOI, BookingStatus.DA_DUYET));
        ConflictException ex1 = assertThrows(ConflictException.class, () ->
                validator.validateTransition(BookingStatus.TU_CHOI, BookingStatus.DA_DUYET)
        );
        assertTrue(ex1.getMessage().contains("Vi phạm tính nhất quán"));

        // DA_HUY -> CHO_DUYET (Đã hủy không thể quay lại chờ duyệt)
        assertFalse(validator.canTransition(BookingStatus.DA_HUY, BookingStatus.CHO_DUYET));
        assertThrows(ConflictException.class, () ->
                validator.validateTransition(BookingStatus.DA_HUY, BookingStatus.CHO_DUYET)
        );

        // DA_DUYET -> CHO_DUYET (Đã duyệt không thể tụt lùi về chờ duyệt)
        assertFalse(validator.canTransition(BookingStatus.DA_DUYET, BookingStatus.CHO_DUYET));
        assertThrows(ConflictException.class, () ->
                validator.validateTransition(BookingStatus.DA_DUYET, BookingStatus.CHO_DUYET)
        );
    }

    @Test
    @DisplayName("TH6 Test 3: Nhận diện chính xác trạng thái kết thúc (Terminal States)")
    void testTerminalStates() {
        assertTrue(validator.isTerminalState(BookingStatus.TU_CHOI));
        assertTrue(validator.isTerminalState(BookingStatus.DA_HUY));
        assertFalse(validator.isTerminalState(BookingStatus.CHO_DUYET));
        assertFalse(validator.isTerminalState(BookingStatus.DA_DUYET));
    }
}

