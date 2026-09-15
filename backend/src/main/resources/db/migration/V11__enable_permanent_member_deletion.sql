DELETE FROM drink_catalog WHERE active = FALSE;

-- Các phiên bản cũ chỉ khóa thành viên. Xóa dữ liệu phụ thuộc theo đúng thứ tự
-- để dọn vĩnh viễn các tài khoản đó mà vẫn giữ nguyên ràng buộc an toàn của CSDL.
DELETE n FROM notifications n
JOIN members m ON m.id = n.recipient_member_id
WHERE m.active = FALSE;

DELETE st FROM settlement_transfers st
JOIN members m ON m.id = st.debtor_member_id OR m.id = st.creditor_member_id
WHERE m.active = FALSE;

DELETE sb FROM settlement_balances sb
JOIN members m ON m.id = sb.member_id
WHERE m.active = FALSE;

DELETE cm FROM chat_messages cm
JOIN members m ON m.id = cm.sender_member_id
WHERE m.active = FALSE;

DELETE oi FROM order_items oi
JOIN members m ON m.id = oi.consumer_member_id
WHERE m.active = FALSE;

DELETE `as` FROM auth_sessions `as`
JOIN members m ON m.id = `as`.member_id
WHERE m.active = FALSE;

DELETE o FROM drink_orders o
JOIN members m ON m.id = o.payer_member_id
WHERE m.active = FALSE;

DELETE FROM members WHERE active = FALSE;
