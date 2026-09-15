-- Các phiên bản cũ chỉ đánh dấu tin nhắn là đã xóa. Từ V9, thao tác xóa là xóa vĩnh viễn.
DELETE FROM chat_messages
WHERE deleted_at IS NOT NULL;
