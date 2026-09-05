# ADS-01 — Đặc tả yêu cầu

**TripMind** · v1.0 · 03/09/2026

---

## 1. Sản phẩm

TripMind là ứng dụng web giúp người đi du lịch lập, chỉnh và theo dõi kế hoạch cho một chuyến đi: lịch trình theo ngày, địa điểm, quãng đường, ngân sách và thời tiết.

Hệ thống làm đúng hai việc: **giữ trạng thái thật của chuyến đi**, và **để một trợ lý AI đọc trạng thái đó rồi đề xuất thay đổi có căn cứ**.

> **Nguyên tắc chi phối: AI không tự thay đổi dữ liệu chuyến đi.** Nó đọc, gọi công cụ, phân tích rồi **đề xuất**. Người dùng duyệt thì hệ thống mới ghi — `QĐ-01`. Khi một lịch trình sai, chỉ có hai khả năng: người dùng đã duyệt nhầm, hoặc đề xuất được dựng từ dữ liệu sai. Không có khả năng thứ ba là AI tự ý sửa.

### 1.1 Trả lời được gì

| Trả lời được | Không trả lời |
|---|---|
| Ngày mai lịch của tôi có gì, đi bao xa | Chuyến đi này có đáng tiền không |
| Trời mưa thì đổi được sang chỗ nào, tốn thêm bao nhiêu | Nên đi du lịch ở đâu năm nay |
| Còn bao nhiêu ngân sách, tiêu quá ở khoản nào | Giá vé, giá phòng thực tế hôm đó |
| Sắp lại thứ tự trong ngày thì tiết kiệm được mấy phút | Chỗ nào "đẹp nhất", "đáng đi nhất" |

Sản phẩm cung cấp **trạng thái chuyến đi có thể truy vết** và **đề xuất kèm căn cứ** để người dùng tự quyết. Nó không quyết thay người đi.

### 1.2 Vì sao không thay bằng một trợ lý hội thoại thông thường

Một trợ lý hội thoại sinh được văn bản lịch trình. Nó không có trạng thái chuyến đi, không biết hôm qua người dùng đã đổi gì, không cộng được ngân sách đã tiêu, không lưu được kết quả, và không có ai duyệt trước khi đổi.

TripMind giữ đúng những thứ đó: dữ liệu người dùng, trạng thái chuyến, cơ sở dữ liệu lịch trình, địa điểm đã lưu, ngân sách, chi tiêu, dữ liệu thời tiết, bản đồ, luật nghiệp vụ và bước phê duyệt.

---

## 2. Người dùng

| # | Vai trò | Câu hỏi chính | Màn hình |
|---|---|---|---|
| P1 | Người đi du lịch | Lịch của tôi thế nào, đổi được không, còn bao nhiêu tiền | Bảng điều khiển · Không gian chuyến đi · Trợ lý |
| P2 | Quản trị | Hệ thống đang chạy ra sao, AI gọi công cụ gì | Màn quản trị · Nhật ký chạy công cụ |

P1 chiếm gần như toàn bộ lượt truy cập. P2 chỉ đọc, không chạm dữ liệu chuyến đi của người khác.

| Vai trò | Thấy chuyến đi nào | Sửa được gì |
|---|---|---|
| Người đi du lịch | **Chỉ chuyến của mình** | Toàn bộ chuyến của mình |
| Quản trị | Danh sách chuyến ở mức tổng hợp | **Không sửa nội dung chuyến đi của ai** |

Một tài khoản mang **một** vai trò — `BR-005`.

---

## 3. Quy trình

| Bước | Ai | Việc | Kết quả |
|---|---|---|---|
| 1 | Người dùng | Đăng ký, đăng nhập | Có phiếu truy cập |
| 2 | Người dùng | Đi qua sáu bước tạo chuyến: điểm đến, ngày, số người, ngân sách, sở thích, phong cách | Chuyến đi *đang lập* + đủ số ngày trống |
| 3 | Hệ thống | Sinh lịch trình nháp bằng AI, đối chiếu địa điểm thật | Lịch trình có nội dung |
| 4 | Người dùng | Sửa tay: thêm, xoá, đổi giờ, kéo thả đổi thứ tự | Lịch trình theo ý mình |
| 5 | Người dùng | Hỏi trợ lý một câu về chuyến đi | Trả lời dựa trên dữ liệu thật |
| 6 | Hệ thống | Trợ lý gọi công cụ, dựng **bản đề xuất** | Đề xuất *chờ duyệt* |
| 7 | Người dùng | Xem khác biệt, bấm áp dụng hoặc huỷ | Đề xuất *đã áp dụng* hoặc *bị từ chối* |
| 8 | Hệ thống | Ghi thay đổi trong một giao dịch | Lịch trình mới |
| 9 | Người dùng | Ghi chi tiêu thực tế trong lúc đi | Đối chiếu được với ngân sách |

Bước 7 là điểm phê duyệt bắt buộc — `QĐ-01`. Không có đường nào từ bước 6 sang bước 8 mà không qua bước 7.

---

## 4. Yêu cầu chức năng

### 4.1 FR-0xx — Xác thực và tài khoản

| Mã | Yêu cầu |
|---|---|
| FR-001 | Đăng ký bằng email, mật khẩu và họ tên |
| FR-002 | Đăng nhập nhận phiếu truy cập và phiếu làm mới |
| FR-003 | Phiếu truy cập hết hạn thì làm mới, không cần đăng nhập lại |
| FR-004 | Đăng xuất thu hồi phiếu làm mới |
| FR-005 | Phiếu làm mới đã thu hồi mà bị dùng lại thì thu hồi cả họ phiếu của người đó |
| FR-006 | Xem và sửa hồ sơ: họ tên, ảnh đại diện |
| FR-007 | Đặt sở thích du lịch và phong cách mặc định, dùng lại cho các chuyến sau |
| FR-008 | Một tài khoản mang một vai trò: người dùng hoặc quản trị |

### 4.2 FR-1xx — Chuyến đi

| Mã | Yêu cầu |
|---|---|
| FR-101 | Tạo chuyến đi qua sáu bước, quay lại sửa bước trước được |
| FR-102 | Chuyến đi gồm điểm đến, ngày bắt đầu, ngày kết thúc, số người, ngân sách, mã tiền tệ |
| FR-103 | Từ chối ngày kết thúc trước ngày bắt đầu — `DI-9` |
| FR-104 | Từ chối chuyến dài quá 30 ngày — `BR-201` |
| FR-105 | Tạo chuyến sinh đủ số ngày trống trong khoảng, trong cùng một giao dịch — `BR-202` |
| FR-106 | Đổi khoảng ngày thì sinh thêm hoặc xoá bớt ngày tương ứng — `BR-203` |
| FR-107 | Xem danh sách chuyến sắp tới và đã qua |
| FR-108 | Mỗi chuyến hiển thị tiến độ lập kế hoạch theo phần trăm — `BR-207` |
| FR-109 | Xoá chuyến đi xoá luôn ngày, hoạt động, chi tiêu, hội thoại thuộc chuyến đó |
| FR-110 | **Người dùng chỉ thấy và chỉ sửa chuyến của mình** — `BR-101` |

### 4.3 FR-2xx — Lịch trình

| Mã | Yêu cầu |
|---|---|
| FR-201 | Xem lịch trình chia theo ngày, mỗi ngày sắp theo thứ tự |
| FR-202 | Hoạt động gồm tên, giờ bắt đầu, giờ kết thúc, chi phí ước tính, phương tiện, ghi chú |
| FR-203 | **Hoạt động luôn có tên; địa điểm không bắt buộc** — `DI-4`. "Đến sân bay", "Ăn trưa" là hoạt động hợp lệ không gắn địa điểm |
| FR-204 | Thêm, sửa, xoá hoạt động |
| FR-205 | Kéo thả đổi thứ tự trong ngày |
| FR-206 | Sắp lại thứ tự gửi cả danh sách, không gửi từng phần tử — `BR-204` |
| FR-207 | Từ chối danh sách sắp lại thiếu hoặc thừa so với tập hoạt động của ngày — `BR-205` |
| FR-208 | Từ chối giờ kết thúc trước giờ bắt đầu — `BR-206` |
| FR-209 | Ghi lại hoạt động do người tạo hay do AI tạo |

### 4.4 FR-3xx — Địa điểm

| Mã | Yêu cầu |
|---|---|
| FR-301 | Tìm địa điểm theo từ khoá và vị trí |
| FR-302 | Xem chi tiết: tên, loại, địa chỉ, toạ độ, đánh giá, mức giá |
| FR-303 | Thêm địa điểm tìm được thẳng vào một ngày của lịch trình |
| FR-304 | Lưu địa điểm yêu thích, bỏ lưu |
| FR-305 | **Một người không lưu trùng một địa điểm** — `DI-7` |
| FR-306 | Địa điểm từ nguồn ngoài lưu một bản duy nhất theo *(nhà cung cấp, mã ngoài)* — `DI-8` |
| FR-307 | Bỏ lưu không xoá bản ghi địa điểm — `BR-304` |

### 4.5 FR-4xx — Ngân sách và chi tiêu

| Mã | Yêu cầu |
|---|---|
| FR-401 | Đặt ngân sách tổng và mã tiền tệ cho chuyến đi |
| FR-402 | Xem tổng chi phí ước tính, cộng từ chi phí của các hoạt động |
| FR-403 | Ghi chi tiêu thực tế theo hạng mục, có thể gắn với một hoạt động |
| FR-404 | Xem phân bổ theo hạng mục: lưu trú, ăn uống, đi lại, hoạt động, mua sắm, khác |
| FR-405 | **Chi phí ước tính và chi tiêu thực tế là hai đại lượng tách rời** — `BR-402` |
| FR-406 | Cảnh báo khi vượt 90% và khi vượt 100% ngân sách — `BR-403` |
| FR-407 | Chỉ cộng các khoản cùng mã tiền tệ — `BR-401` |
| FR-408 | Mọi khoản tiền không âm — `DI-10` |

### 4.6 FR-5xx — Thời tiết và dữ liệu ngoài

| Mã | Yêu cầu |
|---|---|
| FR-501 | Hiển thị thời tiết cho từng ngày của chuyến đi |
| FR-502 | **Ngày trong 16 ngày tới dùng dự báo; xa hơn dùng trung bình khí hậu** — `QĐ-07` |
| FR-503 | Mọi số liệu thời tiết hiển thị kèm nhãn nguồn, người dùng phân biệt được hai loại |
| FR-504 | Quy đổi tiền tệ khi ngân sách và chi tiêu khác mã tiền |
| FR-505 | Dịch vụ ngoài lỗi thì trang vẫn dùng được, phần dữ liệu đó để trống kèm lý do — `QĐ-11` |

### 4.7 FR-6xx — Trợ lý AI

| Mã | Yêu cầu |
|---|---|
| FR-601 | Hỏi đáp bằng tiếng Việt trong ngữ cảnh một chuyến đi cụ thể |
| FR-602 | Trợ lý đọc được dữ liệu thật của chuyến: lịch trình, ngân sách, sở thích, địa điểm đã lưu |
| FR-603 | Trợ lý gọi được dịch vụ ngoài: thời tiết, tìm địa điểm, khoảng cách |
| FR-604 | **Trợ lý không truy cập cơ sở dữ liệu; chỉ gọi công cụ** — `QĐ-02` |
| FR-605 | Câu trả lời phát về theo dòng, người dùng thấy tiến trình chứ không chờ màn hình trắng |
| FR-606 | Hiển thị công cụ đang chạy kèm thời gian mỗi bước |
| FR-607 | Lưu lịch sử hội thoại, mở lại thấy nguyên |
| FR-608 | Vòng lặp dừng ở vòng thứ 8 hoặc giây thứ 60 — `BR-502` |
| FR-609 | Sinh lịch trình tự động cho chuyến mới, chạy nền, có báo tiến trình |
| FR-610 | **Địa điểm trong lịch trình do AI sinh phải khớp bản ghi có thật** — `QĐ-08`, `BR-509` |
| FR-611 | Phân tích ngân sách: còn bao nhiêu, tiêu quá ở đâu |
| FR-612 | Đề xuất đổi lịch khi thời tiết bất lợi |
| FR-613 | Tối ưu thứ tự trong ngày để giảm quãng đường — **tính bằng mã, không bằng mô hình** — `QĐ-09` |

### 4.8 FR-7xx — Đề xuất và phê duyệt

| Mã | Yêu cầu |
|---|---|
| FR-701 | Trợ lý dựng bản đề xuất thay vì sửa thẳng — `QĐ-01` |
| FR-702 | Đề xuất gồm danh sách thao tác: thêm, xoá, sửa, sắp lại |
| FR-703 | Đề xuất kèm chênh lệch chi phí và chênh lệch thời gian di chuyển |
| FR-704 | Người dùng xem khác biệt trước khi quyết |
| FR-705 | **Áp dụng chỉ nhận mã đề xuất, không nhận danh sách thay đổi từ máy khách** — `BR-506` |
| FR-706 | Áp dụng kiểm đủ bốn điều: đúng chuyến, đúng người, đang chờ, chưa quá hạn — `BR-507` |
| FR-707 | Đề xuất hết hạn sau 30 phút |
| FR-708 | Áp dụng lần thứ hai là vô hiệu — `BR-508` |
| FR-709 | Toàn bộ việc áp dụng nằm trong một giao dịch; hỏng giữa chừng thì không ghi gì |
| FR-710 | Huỷ đề xuất chuyển sang trạng thái bị từ chối, không xoá bản ghi |

### 4.9 FR-8xx — Bản đồ

| Mã | Yêu cầu |
|---|---|
| FR-801 | Hiển thị các hoạt động của một ngày lên bản đồ |
| FR-802 | Đánh số điểm theo thứ tự trong ngày, nối đường giữa các điểm |
| FR-803 | Hiển thị khoảng cách và thời gian di chuyển ước tính giữa hai điểm liên tiếp |
| FR-804 | Bỏ qua hoạt động không có toạ độ, không báo lỗi |
| FR-805 | Bấm vào điểm thì cuộn tới hoạt động tương ứng trong lịch trình |

### 4.10 FR-9xx — Quản trị và nhật ký

| Mã | Yêu cầu |
|---|---|
| FR-901 | **Ghi nhật ký mọi lần chạy công cụ, kể cả khi lỗi hoặc quá hạn** — `BR-505` |
| FR-902 | Nhật ký gồm tên công cụ, tham số, kết quả, trạng thái, thời gian chạy |
| FR-903 | Quản trị xem nhật ký chạy công cụ, lọc theo người, công cụ, trạng thái, khoảng ngày |
| FR-904 | Quản trị xem thống kê sử dụng AI: số lượt, số token, công cụ hay dùng |
| FR-905 | Quản trị xem danh sách người dùng và danh sách chuyến ở mức tổng hợp |
| FR-906 | **Quản trị không sửa được nội dung chuyến đi của người khác** |

---

### 4.11 FR-10xx — Giai đoạn đang đi *(v2.0)*

| Mã | Yêu cầu |
|---|---|
| FR-1001 | Chuyến đi có ba giai đoạn suy từ ngày: chưa đi · đang đi · đã xong |
| FR-1002 | Người dùng đặt tay giai đoạn được; giao diện **phải ghi rõ là đặt tay** |
| FR-1003 | Đánh dấu từng hoạt động: chưa làm · đang làm · đã xong · đã bỏ |
| FR-1004 | Bỏ một hoạt động phải ghi lý do; **bỏ khác với xoá** |
| FR-1005 | Nhiều nhất một hoạt động đang làm trong một chuyến |
| FR-1006 | Ghi giờ thực tế **cạnh** giờ dự kiến, không thay thế |
| FR-1007 | Tính lệch lịch, phân biệt "đã trễ" (đã xong muộn) với "đang trễ" (chưa xong) |
| FR-1008 | Hệ thống **không tự dời giờ**; trợ lý dựng đề xuất, người dùng duyệt |
| FR-1009 | Không đề xuất sửa hoạt động đã xong hoặc đã bỏ |
| FR-1010 | Dải "Hôm nay": đang làm gì, kế tiếp là gì, còn bao lâu |
| FR-1011 | Ghi nhật ký một dòng cảm nhận cho từng hoạt động **hoặc cho cả ngày** |
| FR-1012 | Đính ảnh vào nhật ký; ảnh được nén trước khi lưu |
| FR-1013 | Không lưu được ảnh thì **nói thẳng**, không nhận rồi làm mất |
| FR-1014 | Nhập chi tiêu bằng giọng nói ở nơi trình duyệt hỗ trợ; không hỗ trợ thì ẩn nút |

### 4.12 FR-11xx — Sau chuyến *(v2.0)*

| Mã | Yêu cầu |
|---|---|
| FR-1101 | Tab "Nhìn lại" chỉ mở được khi chuyến đã kết thúc |
| FR-1102 | Tổng kết: làm bao nhiêu, bỏ bao nhiêu và vì sao, đi bao xa, chi bao nhiêu |
| FR-1103 | So ước tính với thực tế **theo từng hạng mục** |
| FR-1104 | Hạng mục thiếu một trong hai vế → "chưa đủ dữ liệu", **không** hiện 0% hay vô cực |
| FR-1105 | Hoạt động đã bỏ không tính vào phần ước tính đem so |
| FR-1106 | Thói quen ước lượng của người dùng qua **≥2 chuyến** đã kết thúc |
| FR-1107 | Hệ số ước sai **chỉ để gợi ý**, không cộng vào tổng ước tính |
| FR-1108 | Quyết toán chia tiền theo từng loại tiền tệ, rút gọn thành ít lượt chuyển nhất |
| FR-1109 | Người đi cùng **không cần có tài khoản** |
| FR-1110 | Chia lẻ không được làm mất tiền: phần dư rải cho vài người đầu |

### 4.13 FR-12xx — Hoàn tác và giải trình *(v2.0)*

| Mã | Yêu cầu |
|---|---|
| FR-1201 | Hoàn tác một đề xuất đã áp dụng, trong cửa sổ 10 phút |
| FR-1202 | Năm điều kiểm khi hoàn tác — `ADS-30` §10a.1 |
| FR-1203 | Hoạt động đã bị sửa tay thì hoàn tác **giữ nguyên**, không đè |
| FR-1204 | Báo rõ thao tác nào không lùi được và vì sao |
| FR-1205 | Hộp xác nhận liệt kê **đúng** những việc sắp xảy ra |
| FR-1206 | Nói rõ địa điểm đã ghi vào CSDL **không** bị rút lại |
| FR-1207 | Đề xuất đã hoàn tác không áp dụng lại được |
| FR-1208 | Mỗi hoạt động do AI tạo truy được về đề xuất sinh ra nó |
| FR-1209 | Xem được: công cụ nào chạy, ứng viên nào bị loại và vì sao |
| FR-1210 | Không truy được nguồn thì **nói là không biết**, không dựng lời giải thích |
| FR-1211 | Đặt ràng buộc cứng cho chuyến đi |
| FR-1212 | Phương án không giữ được ràng buộc phải **ghi rõ**, không âm thầm bỏ qua |

## 5. Yêu cầu phi chức năng

| Mã | Yêu cầu | Mức |
|---|---|---|
| NFR-01 | Trang lịch trình hiển thị xong | dưới 1,5 giây |
| NFR-02 | Điểm cuối đọc thông thường | dưới 300 ms |
| NFR-03 | Lượt hỏi trợ lý có gọi công cụ | dưới 60 giây, có phát tiến trình từ giây đầu |
| NFR-04 | Truy vấn lịch trình không sinh truy vấn lặp theo số dòng | dùng nạp kèm |
| NFR-05 | Mật khẩu băm bằng BCrypt, không ghi ra nhật ký | `BR-002` |
| NFR-06 | Khoá dịch vụ ngoài chỉ nằm ở biến môi trường, không có trong mã nguồn | |
| NFR-07 | Giới hạn tần suất hai tầng, tầng AI chặt hơn | `BR-601` |
| NFR-08 | Toàn bộ điểm cuối có mô tả OpenAPI | |
| NFR-09 | Mọi thay đổi lược đồ đi qua Flyway | `QĐ-12` |
| NFR-10 | Dịch vụ ngoài lỗi không làm hỏng trang | `QĐ-11` |
| NFR-11 | Giao diện dùng được trên màn hình điện thoại | |
| NFR-12 | Toàn hệ thống dựng lại được bằng một lệnh Docker Compose | |

---

## 6. Ranh giới

### 6.0 Đã đưa vào phạm vi ở v2.0

Ba thứ bản v1.0 xếp ngoài phạm vi, nay đã làm — vì chúng không kéo theo cái giá mà v1.0
lo ngại:

| Việc | Vì sao làm được mà không đổi mô hình quyền |
|---|---|
| Chia tiền nhiều người | Người đi cùng là **tên trong chuyến**, không phải tài khoản. Chuyến vẫn thuộc đúng một chủ — `DI-1` giữ nguyên |
| Ảnh | Chỉ lưu cục bộ theo người dùng, không chia sẻ, không CDN công khai |
| Dùng trong lúc đang đi | Không cần GPS, không cần thông báo đẩy — chỉ là vài cột trạng thái và một cách hiển thị khác |

Vẫn **chưa** làm: mời người khác vào cùng một chuyến, chia sẻ công khai, thanh toán, đặt vé.
Ba cái đó mới thực sự đổi mô hình quyền.

### 6.1 Năm thứ hệ thống không làm

| Không làm | Vì sao |
|---|---|
| Đặt vé máy bay, đặt phòng | Cần hợp đồng với nhà cung cấp và xử lý thanh toán — ngoài phạm vi |
| Thanh toán | Kéo theo toàn bộ nghĩa vụ về bảo mật tài chính |
| Mạng xã hội, chia sẻ chuyến đi giữa người dùng | Đổi mô hình quyền từ một chủ sở hữu sang nhiều người |
| Nhắn tin thời gian thực giữa người dùng | Không phục vụ bài toán lập kế hoạch |
| Ứng dụng di động | Giao diện web đáp ứng đã đủ cho phạm vi này |

### 6.2 Bốn thứ hệ thống không biết

| Không biết | Hệ quả |
|---|---|
| Giá vé, giá phòng thực tế | Mọi con số chi phí là **ước tính do người dùng nhập**, không phải giá thị trường |
| Địa điểm còn mở hay đã đóng cửa | Dữ liệu là ảnh chụp tại thời điểm tra — `BR-302` |
| Thời tiết ngoài 16 ngày | Chỉ có trung bình khí hậu, không phải dự báo — `QĐ-07` |
| Người dùng có thực sự đi hay không | Chi tiêu thực tế chỉ có khi người dùng tự ghi |

---

## 7. Truy vết

| Nhóm yêu cầu | Bảng dữ liệu chính | Màn hình |
|---|---|---|
| FR-0xx | `users` · `refresh_tokens` · `user_preferences` | Đăng nhập · Hồ sơ |
| FR-1xx | `trips` · `destinations` · `itinerary_days` | Bảng điều khiển · Tạo chuyến · Tổng quan |
| FR-2xx | `itinerary_days` · `activities` | Lịch trình |
| FR-3xx | `places` · `saved_places` | Địa điểm · Đã lưu |
| FR-4xx | `expenses` · `activities.estimated_cost` | Ngân sách |
| FR-5xx | `weather_cache` · bộ nhớ đệm | Tổng quan · Lịch trình |
| FR-6xx | `conversations` · `messages` | Trợ lý |
| FR-7xx | `ai_proposals` | Thẻ khác biệt trong Trợ lý |
| FR-8xx | `activities` (toạ độ qua `places`) | Bản đồ |
| FR-9xx | `ai_tool_executions` | Màn quản trị |

---

## 8. Tài liệu liên quan

| Tài liệu | Nội dung |
|---|---|
| [`../PLAN.md`](../PLAN.md) §12 | **Nguồn chân lý** — 12 `QĐ` · 12 `DI` · 28 `BR` |
| [`ADS-02`](ADS-02-Tu-dien-Mo-hinh-mien.md) | Một từ ở đây nghĩa là gì, ánh xạ sang cột nào |
| [`ADS-10`](ADS-10-Kien-truc-phan-mem.md) | Có những thành phần nào, ai gọi ai |
| [`ADS-20`](ADS-20-Thiet-ke-CSDL.md) | Mỗi bảng mỗi cột chứa gì |
| [`ADS-21`](ADS-21-Tich-hop-AI-Agent.md) | Trợ lý gọi công cụ ra sao |
| [`ADS-30`](ADS-30-Hop-dong-API.md) | Điểm cuối, mã lỗi, phân quyền |
