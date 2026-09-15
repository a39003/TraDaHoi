UPDATE weekly_notification_settings
SET debtor_title = 'Đến lượt thanh toán trà đá'
WHERE debtor_title = 'Den luot thanh toan tra da';

UPDATE weekly_notification_settings
SET debtor_body = 'Bạn cần chuyển {amount} cho {creditor} cho tuần {weekStart}.'
WHERE debtor_body = 'Ban can chuyen {amount} cho {creditor} cho tuan {weekStart}.';

UPDATE weekly_notification_settings
SET creditor_title = 'Tổng hợp tiền trà đá tuần này'
WHERE creditor_title = 'Tong hop tien tra da tuan nay';

UPDATE weekly_notification_settings
SET creditor_body = 'Bạn đã trả trước tiền nước. Các thành viên sẽ hoàn lại tổng {amount} cho bạn trong tuần {weekStart}.'
WHERE creditor_body = 'Ban da tra truoc tien nuoc. Cac thanh vien se hoan lai tong {amount} cho ban trong tuan {weekStart}.';
