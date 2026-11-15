-- ====================================================================
-- BOOKING SERVICE - NÂNG CẤP SCHEMA CHO TH1–TH7 (NHẬT) — BẢN CHẠY LẠI AN TOÀN
--
-- Áp dụng cho bảng `bookings` đã tạo bằng script chung của nhóm (status ENUM chưa có 'da_huy'),
-- kể cả khi Booking Service đã từng khởi động trước đó (Hibernate ddl-auto=update đã thêm cột / index
-- nhưng KHÔNG thêm 'da_huy' vào ENUM). CSDL mới do Hibernate tạo cũng chạy được (không thay đổi gì).
--
-- CÁCH CHẠY (người chạy TỰ CHỌN tên CSDL, script KHÔNG cố định tên CSDL):
--     mysql -u <user> -p --default-character-set=utf8mb4 <ten_csdl_booking> < V2__booking_th1_th7_mysql.sql
--   - KHÔNG dùng --force: gặp lỗi phải dừng.
--   - Chạy lại nhiều lần an toàn: phần nào đã có thì bỏ qua.
--   - Cần quyền ALTER, INDEX, CREATE trên CSDL Booking.
--
-- NGUYÊN TẮC:
--   - Phần 1 CHỈ KIỂM TRA (đọc information_schema). Cấu trúc không tương thích → dừng NGAY với lỗi
--     "Table '<csdl>.V2_LOI_...' doesn't exist" (mã lỗi ở bảng dưới), CHƯA thay đổi gì.
--   - Phần 2 chỉ THÊM phần còn thiếu. Không DROP / DELETE / TRUNCATE, không đổi tên hay kiểu cột hiện có.
--     ENUM status chỉ được mở rộng khi đang ĐÚNG định nghĩa cũ của script chung.
--
-- MÃ LỖI:
--   V2_LOI_chua_chon_csdl                  chưa chỉ định CSDL khi gọi lệnh mysql (MySQL báo "No database selected")
--   V2_LOI_khong_co_bang_bookings          CSDL không có bảng bookings
--   V2_LOI_bookings_khong_phai_InnoDB      bảng bookings không dùng InnoDB (cần transaction / khóa dòng)
--   V2_LOI_cot_goc_bookings_sai            thiếu hoặc sai kiểu cột gốc (id, user_id, pitch_id, start_time,
--                                          end_time, total_price, status, created_at)
--   V2_LOI_enum_status_khong_tuong_thich   status khác cả định nghĩa cũ lẫn định nghĩa mới
--   V2_LOI_cot_moi_sai_kieu                price_per_hour / idempotency_key / version / updated_at đã có nhưng sai kiểu
--   V2_LOI_index_trung_ten_sai_dinh_nghia  index / unique đã có tên nhưng khác cột hoặc khác tính duy nhất
--   V2_LOI_trung_idempotency_key           dữ liệu đang có 2 đơn cùng (user_id, idempotency_key) → không tạo được UNIQUE
--   V2_LOI_pitch_locks_khong_tuong_thich   bảng pitch_locks đã có nhưng khác cấu trúc (pitch_id BIGINT PK, InnoDB)
--
-- KIỂM THỬ: đã chạy trên MySQL 8.4.3 (instance thử nghiệm riêng, 15/11/2026): lần đầu, lần hai, CSDL do Hibernate tạo,
--   CSDL đã có cột / index nhưng thiếu 'da_huy', các cấu trúc không tương thích. Log:
--   docs/bang-chung-kiem-thu/th4-mysql-8.4.3.txt. CHƯA kiểm thử trên MySQL 8.0 và MySQL của nhóm.
-- ====================================================================

SET @v2_db := DATABASE();
SET @v2_old_status := 'enum(''cho_duyet'',''da_duyet'',''tu_choi'')';
SET @v2_new_status := 'enum(''cho_duyet'',''da_duyet'',''tu_choi'',''da_huy'')';

-- --------------------------------------------------------------------
-- PHẦN 1. KIỂM TRA (không thay đổi gì)
-- --------------------------------------------------------------------

-- 1.1 Phải chỉ định CSDL khi gọi lệnh mysql
SET @v2_sql := IF(@v2_db IS NULL, 'SELECT 1 FROM `V2_LOI_chua_chon_csdl`', 'DO 0');
PREPARE v2_stmt FROM @v2_sql; EXECUTE v2_stmt; DEALLOCATE PREPARE v2_stmt;

-- 1.2 Có bảng bookings và dùng InnoDB
SET @v2_engine := (SELECT engine FROM information_schema.tables
                   WHERE table_schema = @v2_db AND table_name = 'bookings' AND table_type = 'BASE TABLE');
SET @v2_sql := IF(@v2_engine IS NULL, 'SELECT 1 FROM `V2_LOI_khong_co_bang_bookings`', 'DO 0');
PREPARE v2_stmt FROM @v2_sql; EXECUTE v2_stmt; DEALLOCATE PREPARE v2_stmt;
SET @v2_sql := IF(@v2_engine <> 'InnoDB', 'SELECT 1 FROM `V2_LOI_bookings_khong_phai_InnoDB`', 'DO 0');
PREPARE v2_stmt FROM @v2_sql; EXECUTE v2_stmt; DEALLOCATE PREPARE v2_stmt;

-- 1.3 Cột gốc của script chung có đủ và đúng kiểu (chấp nhận cả kiểu do Hibernate sinh)
SET @v2_base_ok := (SELECT COUNT(*) FROM information_schema.columns
                    WHERE table_schema = @v2_db AND table_name = 'bookings' AND (
                          (column_name = 'id'          AND data_type = 'bigint')
                       OR (column_name = 'user_id'     AND data_type = 'bigint')
                       OR (column_name = 'pitch_id'    AND data_type = 'bigint')
                       OR (column_name = 'start_time'  AND data_type = 'datetime')
                       OR (column_name = 'end_time'    AND data_type = 'datetime')
                       OR (column_name = 'total_price' AND column_type = 'decimal(12,2)')
                       OR (column_name = 'status'      AND data_type = 'enum')
                       OR (column_name = 'created_at'  AND data_type IN ('datetime', 'timestamp'))));
SET @v2_sql := IF(@v2_base_ok <> 8, 'SELECT 1 FROM `V2_LOI_cot_goc_bookings_sai`', 'DO 0');
PREPARE v2_stmt FROM @v2_sql; EXECUTE v2_stmt; DEALLOCATE PREPARE v2_stmt;

-- 1.4 ENUM status phải là định nghĩa CŨ (sẽ mở rộng) hoặc MỚI (bỏ qua); khác → dừng, tránh làm mất giá trị lạ
SET @v2_status := (SELECT column_type FROM information_schema.columns
                   WHERE table_schema = @v2_db AND table_name = 'bookings' AND column_name = 'status');
SET @v2_sql := IF(@v2_status NOT IN (@v2_old_status, @v2_new_status),
                  'SELECT 1 FROM `V2_LOI_enum_status_khong_tuong_thich`', 'DO 0');
PREPARE v2_stmt FROM @v2_sql; EXECUTE v2_stmt; DEALLOCATE PREPARE v2_stmt;

-- 1.5 Cột mới nếu ĐÃ có (do lần chạy trước hoặc Hibernate) phải đúng kiểu
SET @v2_bad_cols := (SELECT COUNT(*) FROM information_schema.columns
                     WHERE table_schema = @v2_db AND table_name = 'bookings' AND (
                           (column_name = 'price_per_hour'  AND column_type <> 'decimal(12,2)')
                        OR (column_name = 'idempotency_key' AND column_type <> 'varchar(64)')
                        OR (column_name = 'version'         AND (column_type <> 'bigint' OR is_nullable <> 'NO'))
                        OR (column_name = 'updated_at'      AND data_type <> 'datetime')));
SET @v2_sql := IF(@v2_bad_cols > 0, 'SELECT 1 FROM `V2_LOI_cot_moi_sai_kieu`', 'DO 0');
PREPARE v2_stmt FROM @v2_sql; EXECUTE v2_stmt; DEALLOCATE PREPARE v2_stmt;

-- 1.6 Index / unique nếu ĐÃ có tên phải đúng cột (theo thứ tự) và đúng tính duy nhất
SET @v2_bad_idx := (SELECT COUNT(*) FROM (
        SELECT index_name,
               GROUP_CONCAT(column_name ORDER BY seq_in_index) AS cols,
               MAX(non_unique) AS non_unique
        FROM information_schema.statistics
        WHERE table_schema = @v2_db AND table_name = 'bookings'
          AND index_name IN ('idx_bookings_pitch_time', 'idx_bookings_user_created',
                             'idx_bookings_status', 'uk_bookings_user_idempotency')
        GROUP BY index_name) AS existing
    WHERE NOT ((index_name = 'idx_bookings_pitch_time'      AND cols = 'pitch_id,start_time,end_time' AND non_unique = 1)
            OR (index_name = 'idx_bookings_user_created'    AND cols = 'user_id,created_at'           AND non_unique = 1)
            OR (index_name = 'idx_bookings_status'          AND cols = 'status'                       AND non_unique = 1)
            OR (index_name = 'uk_bookings_user_idempotency' AND cols = 'user_id,idempotency_key'      AND non_unique = 0)));
SET @v2_sql := IF(@v2_bad_idx > 0, 'SELECT 1 FROM `V2_LOI_index_trung_ten_sai_dinh_nghia`', 'DO 0');
PREPARE v2_stmt FROM @v2_sql; EXECUTE v2_stmt; DEALLOCATE PREPARE v2_stmt;

-- 1.7 Nếu sẽ tạo UNIQUE (user_id, idempotency_key): dữ liệu hiện có không được trùng
SET @v2_has_key_col := (SELECT COUNT(*) FROM information_schema.columns
                        WHERE table_schema = @v2_db AND table_name = 'bookings' AND column_name = 'idempotency_key');
SET @v2_has_uk := (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = @v2_db AND table_name = 'bookings'
                     AND index_name = 'uk_bookings_user_idempotency');
SET @v2_dup := 0;
SET @v2_sql := IF(@v2_has_key_col > 0 AND @v2_has_uk = 0,
                  'SELECT COUNT(*) INTO @v2_dup FROM (SELECT user_id, idempotency_key FROM bookings
                     WHERE idempotency_key IS NOT NULL GROUP BY user_id, idempotency_key HAVING COUNT(*) > 1) AS d',
                  'DO 0');
PREPARE v2_stmt FROM @v2_sql; EXECUTE v2_stmt; DEALLOCATE PREPARE v2_stmt;
SET @v2_sql := IF(@v2_dup > 0, 'SELECT 1 FROM `V2_LOI_trung_idempotency_key`', 'DO 0');
PREPARE v2_stmt FROM @v2_sql; EXECUTE v2_stmt; DEALLOCATE PREPARE v2_stmt;

-- 1.8 Bảng pitch_locks nếu ĐÃ có phải đúng cấu trúc
SET @v2_locks_engine := (SELECT engine FROM information_schema.tables
                         WHERE table_schema = @v2_db AND table_name = 'pitch_locks');
SET @v2_locks_ok := (SELECT COUNT(*) FROM information_schema.columns
                     WHERE table_schema = @v2_db AND table_name = 'pitch_locks'
                       AND column_name = 'pitch_id' AND data_type = 'bigint' AND column_key = 'PRI');
SET @v2_locks_cols := (SELECT COUNT(*) FROM information_schema.columns
                       WHERE table_schema = @v2_db AND table_name = 'pitch_locks');
SET @v2_sql := IF(@v2_locks_engine IS NOT NULL
                      AND (@v2_locks_engine <> 'InnoDB' OR @v2_locks_ok <> 1 OR @v2_locks_cols <> 1),
                  'SELECT 1 FROM `V2_LOI_pitch_locks_khong_tuong_thich`', 'DO 0');
PREPARE v2_stmt FROM @v2_sql; EXECUTE v2_stmt; DEALLOCATE PREPARE v2_stmt;

-- --------------------------------------------------------------------
-- PHẦN 2. THÊM PHẦN CÒN THIẾU (mỗi bước tự bỏ qua nếu đã có)
-- --------------------------------------------------------------------

-- 2.1 Trạng thái mới da_huy (khách hủy đơn, hủy mềm). Chỉ khi ENUM đang đúng định nghĩa cũ → không mất giá trị nào.
SET @v2_sql := IF(@v2_status = @v2_old_status,
                  'ALTER TABLE bookings MODIFY status ENUM(''cho_duyet'',''da_duyet'',''tu_choi'',''da_huy'') NOT NULL DEFAULT ''cho_duyet''',
                  'DO 0');
PREPARE v2_stmt FROM @v2_sql; EXECUTE v2_stmt; DEALLOCATE PREPARE v2_stmt;

-- 2.2 Cột mới (nullable hoặc có default nên an toàn với dữ liệu cũ)
SET @v2_sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @v2_db
                     AND table_name = 'bookings' AND column_name = 'price_per_hour') = 0,
                  'ALTER TABLE bookings ADD COLUMN price_per_hour DECIMAL(12, 2) NULL COMMENT ''Don gia san luc dat (ban chup tu Pitch Service)''',
                  'DO 0');
PREPARE v2_stmt FROM @v2_sql; EXECUTE v2_stmt; DEALLOCATE PREPARE v2_stmt;

SET @v2_sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @v2_db
                     AND table_name = 'bookings' AND column_name = 'idempotency_key') = 0,
                  'ALTER TABLE bookings ADD COLUMN idempotency_key VARCHAR(64) NULL COMMENT ''Header Idempotency-Key chong tao don trung''',
                  'DO 0');
PREPARE v2_stmt FROM @v2_sql; EXECUTE v2_stmt; DEALLOCATE PREPARE v2_stmt;

SET @v2_sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @v2_db
                     AND table_name = 'bookings' AND column_name = 'version') = 0,
                  'ALTER TABLE bookings ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT ''Khoa lac quan (JPA @Version)''',
                  'DO 0');
PREPARE v2_stmt FROM @v2_sql; EXECUTE v2_stmt; DEALLOCATE PREPARE v2_stmt;

SET @v2_sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @v2_db
                     AND table_name = 'bookings' AND column_name = 'updated_at') = 0,
                  'ALTER TABLE bookings ADD COLUMN updated_at DATETIME(6) NULL',
                  'DO 0');
PREPARE v2_stmt FROM @v2_sql; EXECUTE v2_stmt; DEALLOCATE PREPARE v2_stmt;

-- 2.3 Index cho truy vấn trùng lịch, danh sách của khách, lọc trạng thái
SET @v2_sql := IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @v2_db
                     AND table_name = 'bookings' AND index_name = 'idx_bookings_pitch_time') = 0,
                  'CREATE INDEX idx_bookings_pitch_time ON bookings (pitch_id, start_time, end_time)', 'DO 0');
PREPARE v2_stmt FROM @v2_sql; EXECUTE v2_stmt; DEALLOCATE PREPARE v2_stmt;

SET @v2_sql := IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @v2_db
                     AND table_name = 'bookings' AND index_name = 'idx_bookings_user_created') = 0,
                  'CREATE INDEX idx_bookings_user_created ON bookings (user_id, created_at)', 'DO 0');
PREPARE v2_stmt FROM @v2_sql; EXECUTE v2_stmt; DEALLOCATE PREPARE v2_stmt;

SET @v2_sql := IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @v2_db
                     AND table_name = 'bookings' AND index_name = 'idx_bookings_status') = 0,
                  'CREATE INDEX idx_bookings_status ON bookings (status)', 'DO 0');
PREPARE v2_stmt FROM @v2_sql; EXECUTE v2_stmt; DEALLOCATE PREPARE v2_stmt;

-- 2.4 Idempotency: một khách không có 2 đơn cùng key (MySQL cho phép nhiều NULL → đơn cũ không bị ảnh hưởng)
SET @v2_sql := IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @v2_db
                     AND table_name = 'bookings' AND index_name = 'uk_bookings_user_idempotency') = 0,
                  'ALTER TABLE bookings ADD CONSTRAINT uk_bookings_user_idempotency UNIQUE (user_id, idempotency_key)',
                  'DO 0');
PREPARE v2_stmt FROM @v2_sql; EXECUTE v2_stmt; DEALLOCATE PREPARE v2_stmt;

-- 2.5 Bảng khóa theo sân (SELECT ... FOR UPDATE). pitch_id là logical ID, KHÔNG có Foreign Key.
CREATE TABLE IF NOT EXISTS pitch_locks (
    pitch_id BIGINT NOT NULL PRIMARY KEY
) ENGINE = InnoDB;

-- --------------------------------------------------------------------
-- PHẦN 3. KẾT QUẢ
-- --------------------------------------------------------------------
SELECT @v2_db AS csdl,
       (SELECT column_type FROM information_schema.columns
         WHERE table_schema = @v2_db AND table_name = 'bookings' AND column_name = 'status') AS status_enum,
       (SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics
         WHERE table_schema = @v2_db AND table_name = 'bookings'
           AND index_name IN ('idx_bookings_pitch_time', 'idx_bookings_user_created',
                              'idx_bookings_status', 'uk_bookings_user_idempotency')) AS so_index_v2,
       (SELECT COUNT(*) FROM information_schema.tables
         WHERE table_schema = @v2_db AND table_name = 'pitch_locks') AS co_pitch_locks,
       (SELECT COUNT(*) FROM bookings) AS so_don;
