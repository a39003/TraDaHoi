import { createElement, Fragment, useEffect, useMemo, useRef, useState } from 'react';
import { apiMediaUrl, teaApi } from './api';

const React = { createElement, Fragment };
const PAGE_SIZE = 50;
const CHAT_REFRESH_INTERVAL_MS = 3_000;
const TYPING_REFRESH_INTERVAL_MS = 4_000;
const emojis = [
  '😀', '😃', '😄', '😁', '😂', '🤣', '😊', '😇', '🙂', '😉', '😍', '🥰',
  '😘', '😋', '😎', '🤩', '🥳', '😅', '😭', '😢', '😤', '😡', '🤔', '🤭',
  '🫡', '😴', '👍', '👎', '👏', '🙌', '🙏', '🤝', '💪', '👌', '✌️', '🤟',
  '❤️', '🧡', '💛', '💚', '💙', '💜', '💯', '🔥', '✨', '🎉', '🎊', '✅',
  '☕', '🍵', '🧋', '🥤', '🧊', '🍋', '🍊', '🍓', '🍻', '💸', '💰', '📷',
];

export default function ChatPanel({ user, members = [], messages, notifications, onSaved, onRefreshChat, onMessageChanged, flash }) {
  const [text, setText] = useState('');
  const [images, setImages] = useState([]);
  const [replyTo, setReplyTo] = useState(null);
  const [emojiOpen, setEmojiOpen] = useState(false);
  const [payment, setPayment] = useState(null);
  const [notificationDetail, setNotificationDetail] = useState(null);
  const [imageViewer, setImageViewer] = useState(null);
  const [sending, setSending] = useState(false);
  const [sendStatus, setSendStatus] = useState('');
  const [olderMessages, setOlderMessages] = useState([]);
  const [loadingOlder, setLoadingOlder] = useState(false);
  const [hasOlder, setHasOlder] = useState(messages.length >= PAGE_SIZE);
  const [searchText, setSearchText] = useState('');
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchResults, setSearchResults] = useState([]);
  const [searchActive, setSearchActive] = useState(false);
  const [searching, setSearching] = useState(false);
  const [searchHasMore, setSearchHasMore] = useState(false);
  const [typingMembers, setTypingMembers] = useState([]);
  const [reactionPickerId, setReactionPickerId] = useState(null);
  const fileRef = useRef(null);
  const scrollRef = useRef(null);
  const lastTypingAt = useRef(0);
  const chatRefreshRunning = useRef(false);

  const allMessages = useMemo(() => {
    const unique = new Map([...olderMessages, ...messages].map((message) => [message.id, message]));
    return [...unique.values()].sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
  }, [olderMessages, messages]);
  const displayedMessages = searchActive ? searchResults : allMessages;
  const mentionMatch = text.match(/@([^@\n]*)$/);
  const mentionSuggestions = mentionMatch ? members.filter((member) => member.id !== user.id && member.displayName.toLocaleLowerCase('vi').includes(mentionMatch[1].trim().toLocaleLowerCase('vi'))).slice(0, 6) : [];

  useEffect(() => {
    const refreshTyping = () => teaApi.getTypingMembers().then(setTypingMembers).catch(() => {});
    refreshTyping();
    const id = window.setInterval(refreshTyping, TYPING_REFRESH_INTERVAL_MS);
    return () => { window.clearInterval(id); teaApi.setTyping(false).catch(() => {}); };
  }, [user.id]);

  useEffect(() => {
    let active = true;
    const refresh = async () => {
      if (!active || document.visibilityState !== 'visible' || chatRefreshRunning.current) return;
      chatRefreshRunning.current = true;
      try { await onRefreshChat(); }
      finally { chatRefreshRunning.current = false; }
    };
    const refreshWhenVisible = () => { if (document.visibilityState === 'visible') refresh(); };
    refresh();
    const id = window.setInterval(refresh, CHAT_REFRESH_INTERVAL_MS);
    window.addEventListener('focus', refresh);
    document.addEventListener('visibilitychange', refreshWhenVisible);
    return () => {
      active = false;
      window.clearInterval(id);
      window.removeEventListener('focus', refresh);
      document.removeEventListener('visibilitychange', refreshWhenVisible);
    };
  }, [onRefreshChat]);

  const changeText = (value) => {
    setText(value);
    const now = Date.now();
    if (value.trim() && now - lastTypingAt.current > 1800) {
      lastTypingAt.current = now;
      teaApi.setTyping(true).catch(() => {});
    } else if (!value.trim()) teaApi.setTyping(false).catch(() => {});
  };

  const chooseMention = (member) => {
    setText((current) => current.replace(/@([^@\n]*)$/, `@${member.displayName} `));
  };

  useEffect(() => {
    if (searchActive) return;
    const box = scrollRef.current;
    if (box) box.scrollTop = box.scrollHeight;
  }, [messages.length, searchActive]);

  const loadOlder = async () => {
    const oldest = allMessages[0];
    if (!oldest || loadingOlder) return;
    const box = scrollRef.current;
    const oldHeight = box?.scrollHeight || 0;
    setLoadingOlder(true);
    try {
      const page = await teaApi.getMessages({ before: oldest.createdAt, limit: PAGE_SIZE });
      setOlderMessages((current) => mergeMessages(page, current));
      setHasOlder(page.length === PAGE_SIZE);
      window.requestAnimationFrame(() => {
        if (box) box.scrollTop = box.scrollHeight - oldHeight;
      });
    } catch (error) { flash(error.message || 'Không tải được tin nhắn cũ.'); }
    finally { setLoadingOlder(false); }
  };

  const runSearch = async (event) => {
    event.preventDefault();
    const query = searchText.trim();
    if (query.length < 2) return flash('Hãy nhập ít nhất 2 ký tự để tìm kiếm.');
    setSearching(true);
    try {
      const page = await teaApi.searchMessages(query, { limit: PAGE_SIZE });
      setSearchResults(page); setSearchActive(true); setSearchHasMore(page.length === PAGE_SIZE);
    } catch (error) { flash(error.message || 'Không tìm được tin nhắn.'); }
    finally { setSearching(false); }
  };

  const loadMoreSearch = async () => {
    const oldest = searchResults[0];
    if (!oldest || searching) return;
    setSearching(true);
    try {
      const page = await teaApi.searchMessages(searchText.trim(), { before: oldest.createdAt, limit: PAGE_SIZE });
      setSearchResults((current) => mergeMessages(page, current));
      setSearchHasMore(page.length === PAGE_SIZE);
    } catch (error) { flash(error.message || 'Không tải được kết quả cũ hơn.'); }
    finally { setSearching(false); }
  };

  const clearSearch = () => {
    setSearchText(''); setSearchResults([]); setSearchActive(false); setSearchHasMore(false);
    window.requestAnimationFrame(() => {
      const box = scrollRef.current;
      if (box) box.scrollTop = box.scrollHeight;
    });
  };

  const send = async (event) => {
    event.preventDefault();
    if (!text.trim() && images.length === 0) return;
    setSending(true);
    const payload = { senderMemberId: user.id, content: text.trim(), replyToMessageId: replyTo?.id || null };
    try {
      const optimizedImages = images.length
        ? await optimizeChatImages(images, (status) => setSendStatus(status))
        : images;
      setSendStatus(images.length ? 'Đang gửi ảnh...' : 'Đang gửi tin nhắn...');
      const sentMessage = images.length
        ? await teaApi.sendMessageWithMedia({ ...payload, images: optimizedImages })
        : await teaApi.sendMessage(payload);
      onMessageChanged(sentMessage);
      await teaApi.setTyping(false).catch(() => {});
      setText(''); setImages([]); setReplyTo(null); setEmojiOpen(false); setImageViewer(null);
      if (fileRef.current) fileRef.current.value = '';
      if (searchActive) clearSearch();
      void onRefreshChat();
    } catch (error) { flash(error.message || 'Không gửi được tin nhắn.'); }
    finally { setSending(false); setSendStatus(''); }
  };

  const remove = async (message) => {
    if (!window.confirm('Bạn có chắc muốn xóa vĩnh viễn tin nhắn này khỏi hệ thống và cơ sở dữ liệu không?')) return;
    try {
      await teaApi.deleteMessage(message.id);
      setSearchResults((current) => current.filter((item) => item.id !== message.id));
      setOlderMessages((current) => current.filter((item) => item.id !== message.id));
      flash('Đã xóa vĩnh viễn tin nhắn.');
      await onRefreshChat();
    } catch (error) { flash(error.message || 'Không thể xóa tin nhắn.'); }
  };

  const react = async (messageId, emoji) => {
    try {
      const changedMessage = await teaApi.toggleReaction(messageId, emoji);
      onMessageChanged(changedMessage);
      setReactionPickerId(null);
      await onRefreshChat();
    } catch (error) { flash(error.message || 'Không thể thả cảm xúc.'); }
  };

  const clearGroupChat = async () => {
    if (!window.confirm('Bạn có chắc muốn xóa VĨNH VIỄN toàn bộ chat nhóm không? Tất cả tin nhắn và ảnh trong cơ sở dữ liệu sẽ không thể khôi phục.')) return;
    try {
      await teaApi.clearGroupChat();
      setOlderMessages([]); setSearchResults([]); setSearchText('');
      setSearchActive(false); setSearchOpen(false); setHasOlder(false); setSearchHasMore(false);
      flash('Đã xóa vĩnh viễn toàn bộ chat nhóm.');
      await onRefreshChat();
    } catch (error) { flash(error.message || 'Không thể xóa chat nhóm.'); }
  };

  const chooseImages = (event) => {
    const selected = Array.from(event.target.files || []);
    const combined = [...images];
    selected.forEach((file) => {
      const duplicate = combined.some((item) => item.name === file.name && item.size === file.size && item.lastModified === file.lastModified);
      if (!duplicate) combined.push(file);
    });
    event.target.value = '';
    if (combined.length > 4) {
      flash(`Mỗi tin nhắn chỉ được tối đa 4 ảnh. Bạn đã có ${images.length} ảnh và đang chọn thêm ${selected.length} ảnh.`);
      return;
    }
    setImages(combined);
  };

  const openQr = async (note) => {
    try {
      setPayment(await teaApi.getPaymentQr(note.transferId));
      if (!note.readAt) await teaApi.markNotificationRead(note.id);
      await onSaved();
    } catch (error) { flash(error.message || 'Không tải được mã QR.'); }
  };

  const openNotificationDetail = async (note) => {
    setNotificationDetail(note);
    try {
      if (!note.readAt) {
        await teaApi.markNotificationRead(note.id);
        await onSaved();
      }
    } catch (error) { flash(error.message || 'Không thể mở chi tiết thông báo.'); }
  };

  const removeNotification = async (note) => {
    if (!window.confirm('Bạn có chắc muốn xóa vĩnh viễn thông báo này không?')) return;
    try {
      await teaApi.deleteNotification(note.id);
      if (notificationDetail?.id === note.id) setNotificationDetail(null);
      setPayment(null);
      flash('Đã xóa thông báo khỏi hệ thống.');
      await onSaved();
    } catch (error) { flash(error.message || 'Không thể xóa thông báo.'); }
  };

  const visibleNotifications = notifications;

  return <>
    <div className="chat-grid">
      <section className="panel system-panel">
        <div className="panel-title"><span>🤖</span><div><h2>Hệ thống</h2><p>Kéo để xem thông báo cũ hoặc xóa những thông báo không cần giữ</p></div></div>
        <div className="system-note-list">{visibleNotifications.length === 0 ? <p className="empty">Chưa có thông báo nào.</p> : visibleNotifications.map((note) => <article className={`system-note ${!note.readAt ? 'unread' : ''}`} key={note.id}>
          <strong>{note.title}</strong><p>{note.body}</p>
          <div className="system-note-actions">
            {note.transferId ? <button type="button" className="system-qr" onClick={() => openQr(note)}>Xem mã QR thanh toán</button> : <button type="button" className="system-qr" onClick={() => openNotificationDetail(note)}>Xem chi tiết</button>}
            <button type="button" className="system-note-delete" onClick={() => removeNotification(note)}>🗑 Xóa</button>
          </div>
          <small>{formatTime(note.createdAt)}</small>
        </article>)}</div>
      </section>

      <section className="panel chat-panel">
        <div className="panel-title chat-heading"><span>💬</span><div><h2>Chat nhóm</h2><p>Tin nhắn được lưu 6 tháng · Ảnh gửi lên được nén tự động.</p></div>
          <div className="chat-heading-actions">
            <button type="button" className={`outline chat-search-toggle ${searchOpen ? 'active' : ''}`} onClick={() => { if (searchOpen) { clearSearch(); setSearchOpen(false); } else setSearchOpen(true); }}>🔎 {searchOpen ? 'Đóng tìm kiếm' : 'Tìm tin nhắn'}</button>
            {user.role === 'ADMIN' && <button type="button" className="danger clear-chat-button" onClick={clearGroupChat}>🗑 Xóa cả đoạn chat</button>}
          </div>
        </div>
        {searchOpen && <form className="chat-searchbar" onSubmit={runSearch}>
          <span>⌕</span><input autoFocus value={searchText} onChange={(event) => setSearchText(event.target.value)} placeholder="Nhập nội dung cần tìm..." />
          {(searchActive || searchText) && <button type="button" className="clear-search" onClick={clearSearch}>×</button>}
          <button className="outline" disabled={searching}>{searching ? 'Đang tìm...' : 'Tìm'}</button>
        </form>}
        {searchActive && <div className="search-summary"><span>Tìm thấy {searchResults.length} tin nhắn phù hợp</span><button onClick={clearSearch}>Quay lại chat</button></div>}

        <div className="message-list" ref={scrollRef}>
          {!searchActive && hasOlder && <button className="load-older-button" onClick={loadOlder} disabled={loadingOlder}>{loadingOlder ? 'Đang tải...' : '↑ Xem tin nhắn cũ'}</button>}
          {searchActive && searchHasMore && <button className="load-older-button" onClick={loadMoreSearch} disabled={searching}>{searching ? 'Đang tải...' : '↑ Xem thêm kết quả cũ'}</button>}
          {displayedMessages.length === 0 && <p className="empty">{searchActive ? 'Không có tin nhắn nào khớp từ khóa.' : 'Chưa có tin nhắn. Hãy bắt đầu cuộc trò chuyện nhé!'}</p>}
          {displayedMessages.map((message) => {
            const mine = message.sender?.id === user.id;
            const canDelete = !message.deleted && (mine || user.role === 'ADMIN');
            const reactionData = groupReactions(message.reactions || [], user.id);
            return <article className={`message ${mine ? 'mine' : ''} ${message.deleted ? 'deleted' : ''}`} key={message.id}>
              <ChatAvatar member={message.sender} />
              <div className="message-body">
                <div className="message-meta"><strong>{message.sender?.displayName}</strong><small>{formatTime(message.createdAt)}</small></div>
                {message.replyTo && <button className="reply-preview" type="button" onClick={() => document.getElementById(`message-${message.replyTo.id}`)?.scrollIntoView({ block: 'center' })}>
                  <b>↪ {message.replyTo.sender?.displayName}</b><span>{message.replyTo.content || 'Tin nhắn đã bị xóa'}</span>
                </button>}
                <div id={`message-${message.id}`} className="message-bubble">
                  {message.deleted ? <p className="deleted-text">Tin nhắn đã bị xóa</p> : <>
                    {message.content && <p>{renderMessageText(message.content, members, searchActive ? searchText.trim() : '')}</p>}
                    {message.attachments?.length > 0 && <div className="chat-images">{message.attachments.map((image) => <button type="button" className="chat-image-button" key={image.id} onClick={() => setImageViewer({ src: apiMediaUrl(image.url), alt: image.filename || 'Ảnh đính kèm' })}><img className="chat-image" src={apiMediaUrl(image.url)} alt={image.filename || 'Ảnh đính kèm'} /></button>)}</div>}
                  </>}
                </div>
                {!message.deleted && reactionData.length > 0 && <div className="message-reactions">{reactionData.map((reaction) => <button type="button" className={reaction.mine ? 'mine' : ''} key={reaction.emoji} title={reaction.names.join(', ')} onClick={() => react(message.id, reaction.emoji)}>{reaction.emoji} <b>{reaction.count}</b></button>)}</div>}
                {!message.deleted && <div className="message-actions"><button type="button" onClick={() => setReactionPickerId(reactionPickerId === message.id ? null : message.id)}>☺ Cảm xúc</button><button type="button" onClick={() => { setReplyTo(message); setEmojiOpen(false); if (searchActive) clearSearch(); }}>↩ Trả lời</button>{canDelete && <button type="button" className="delete-message" onClick={() => remove(message)}>🗑 Xóa</button>}</div>}
                {reactionPickerId === message.id && <div className="reaction-picker">{['👍', '❤️', '😂', '😮', '😢', '🎉'].map((emoji) => <button type="button" key={emoji} onClick={() => react(message.id, emoji)}>{emoji}</button>)}</div>}
              </div>
            </article>;
          })}
          {!searchActive && !hasOlder && allMessages.length > 0 && <p className="history-start">Đã đến tin nhắn đầu tiên còn được lưu</p>}
        </div>

        <form className="chat-compose" onSubmit={send}>
          {typingMembers.length > 0 && <div className="typing-indicator"><span /><b>{typingMembers.map((member) => member.displayName).join(', ')}</b> đang nhập...</div>}
          {replyTo && <div className="compose-reply"><span><b>Đang trả lời {replyTo.sender?.displayName}</b>{replyTo.content || (replyTo.attachments?.length ? 'Ảnh đính kèm' : '')}</span><button type="button" onClick={() => setReplyTo(null)} aria-label="Hủy trả lời">×</button></div>}
          {images.length > 0 && <SelectedImagePreviews images={images} onPreview={(preview) => setImageViewer(preview)} onRemove={(index) => setImages((current) => current.filter((_, itemIndex) => itemIndex !== index))} onClear={() => setImages([])} />}
          {emojiOpen && <div className="emoji-picker" role="dialog" aria-label="Chọn biểu tượng">{emojis.map((emoji) => <button type="button" key={emoji} onClick={() => { changeText(text + emoji); setEmojiOpen(false); }}>{emoji}</button>)}</div>}
          {mentionSuggestions.length > 0 && <div className="mention-suggestions"><small>Gắn thẻ thành viên</small>{mentionSuggestions.map((member) => <button type="button" key={member.id} onClick={() => chooseMention(member)}><ChatAvatar member={member} /><span><b>@{member.displayName}</b><small>Thông báo cho thành viên này</small></span></button>)}</div>}
          <div className="compose-row">
            <button type="button" className="compose-icon" onClick={() => setEmojiOpen(!emojiOpen)} aria-label="Chọn emoji">😊</button>
            <button type="button" className="compose-icon" onClick={() => fileRef.current?.click()} aria-label="Chọn ảnh">📷</button>
            <input ref={fileRef} type="file" accept="image/jpeg,image/png,image/gif,image/webp" multiple hidden onChange={chooseImages} />
            <textarea rows="1" value={text} onChange={(event) => changeText(event.target.value)} onBlur={() => teaApi.setTyping(false).catch(() => {})} onKeyDown={(event) => { if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); event.currentTarget.form?.requestSubmit(); } }} placeholder="Nhắn tin cho cả nhóm... Dùng @ để nhắc tên" />
            <button className="primary send-button" disabled={sending}>{sending ? (sendStatus || 'Đang gửi...') : 'Gửi'}</button>
          </div>
        </form>
      </section>
    </div>
    {payment && <PaymentModal payment={payment} onClose={() => setPayment(null)} />}
    {notificationDetail && <NotificationDetailModal notification={notificationDetail} onClose={() => setNotificationDetail(null)} />}
    {imageViewer && <ImageViewer image={imageViewer} onClose={() => setImageViewer(null)} />}
  </>;
}

const CHAT_IMAGE_MAX_DIMENSION = 1280;
const CHAT_IMAGE_TARGET_BYTES = 1_250_000;

async function optimizeChatImages(images, onProgress) {
  const optimized = [];
  for (let index = 0; index < images.length; index += 1) {
    onProgress(`Đang tối ưu ảnh ${index + 1}/${images.length}...`);
    optimized.push(await optimizeChatImage(images[index]));
  }
  return optimized;
}

async function optimizeChatImage(file) {
  // GIF may be animated. Keep it unchanged instead of only sending its first frame.
  if (file.type === 'image/gif') return file;

  try {
    const source = await loadImageForCompression(file);
    const largestSide = Math.max(source.naturalWidth, source.naturalHeight);
    if (largestSide <= CHAT_IMAGE_MAX_DIMENSION && file.size <= CHAT_IMAGE_TARGET_BYTES) return file;

    const scale = Math.min(1, CHAT_IMAGE_MAX_DIMENSION / largestSide);
    const width = Math.max(1, Math.round(source.naturalWidth * scale));
    const height = Math.max(1, Math.round(source.naturalHeight * scale));
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const context = canvas.getContext('2d');
    context.fillStyle = '#ffffff';
    context.fillRect(0, 0, width, height);
    context.drawImage(source, 0, 0, width, height);

    let quality = 0.78;
    let compressed = await canvasToJpeg(canvas, quality);
    while (compressed.size > CHAT_IMAGE_TARGET_BYTES && quality > 0.44) {
      quality -= 0.12;
      compressed = await canvasToJpeg(canvas, quality);
    }

    const name = (file.name || 'anh-chat').replace(/\.[^.]+$/, '') + '.jpg';
    return new File([compressed], name, { type: 'image/jpeg', lastModified: Date.now() });
  } catch {
    // The backend still validates and compresses an image if a browser cannot
    // optimize it locally, so a client-side optimization failure is harmless.
    return file;
  }
}

function loadImageForCompression(file) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file);
    const image = new Image();
    image.onload = () => { URL.revokeObjectURL(url); resolve(image); };
    image.onerror = () => { URL.revokeObjectURL(url); reject(new Error('Không thể đọc ảnh')); };
    image.src = url;
  });
}

function canvasToJpeg(canvas, quality) {
  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => blob ? resolve(blob) : reject(new Error('Không thể nén ảnh')), 'image/jpeg', quality);
  });
}

function SelectedImagePreviews({ images, onPreview, onRemove, onClear }) {
  const previews = useMemo(() => images.map((file) => ({ file, src: URL.createObjectURL(file), alt: file.name || 'Ảnh đã chọn' })), [images]);
  useEffect(() => () => previews.forEach((preview) => URL.revokeObjectURL(preview.src)), [previews]);
  return <div className="selected-image-panel">
    <div className="selected-image-heading"><span>📷 Đã chọn {images.length}/4 ảnh · Bạn có thể bấm 📷 để chọn thêm</span><button type="button" onClick={onClear}>Bỏ tất cả</button></div>
    <div className="selected-image-grid">{previews.map((preview, index) => <div className="selected-image-item" key={`${preview.file.name}-${preview.file.lastModified}-${index}`}><button type="button" className="selected-image-preview" onClick={() => onPreview({ src: preview.src, alt: preview.alt })}><img src={preview.src} alt={preview.alt} /></button><button type="button" className="selected-image-remove" onClick={() => onRemove(index)} aria-label={`Bỏ ảnh ${preview.alt}`}>×</button><small>{preview.file.name}</small></div>)}</div>
  </div>;
}

function ImageViewer({ image, onClose }) {
  useEffect(() => {
    const close = (event) => { if (event.key === 'Escape') onClose(); };
    document.addEventListener('keydown', close);
    return () => document.removeEventListener('keydown', close);
  }, [onClose]);
  return <div className="image-viewer-backdrop" onMouseDown={onClose}><section className="image-viewer" role="dialog" aria-modal="true" aria-label="Xem ảnh" onMouseDown={(event) => event.stopPropagation()}><button type="button" className="image-viewer-close" onClick={onClose} aria-label="Đóng ảnh">×</button><img src={image.src} alt={image.alt} /><p>{image.alt}</p></section></div>;
}

function mergeMessages(first, second) {
  const unique = new Map([...first, ...second].map((message) => [message.id, message]));
  return [...unique.values()].sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
}

function highlight(content, query) {
  if (!query) return content;
  const index = content.toLocaleLowerCase('vi').indexOf(query.toLocaleLowerCase('vi'));
  if (index < 0) return content;
  return <>{content.slice(0, index)}<mark>{content.slice(index, index + query.length)}</mark>{content.slice(index + query.length)}</>;
}

function renderMessageText(content, members, query) {
  const source = String(content || '');
  const lower = source.toLocaleLowerCase('vi');
  const targets = members.map((member) => ({ type: 'mention', text: `@${member.displayName}`, lower: `@${member.displayName}`.toLocaleLowerCase('vi') }));
  if (query) targets.push({ type: 'search', text: query, lower: query.toLocaleLowerCase('vi') });
  const result = [];
  let cursor = 0;
  while (cursor < source.length) {
    let match = null;
    targets.forEach((target) => {
      const index = lower.indexOf(target.lower, cursor);
      if (index >= 0 && (!match || index < match.index || (index === match.index && target.type === 'mention'))) match = { ...target, index };
    });
    if (!match) { result.push(source.slice(cursor)); break; }
    if (match.index > cursor) result.push(source.slice(cursor, match.index));
    const value = source.slice(match.index, match.index + match.text.length);
    result.push(match.type === 'mention' ? <span className="chat-mention" key={`${match.index}-${value}`}>{value}</span> : <mark key={`${match.index}-${value}`}>{value}</mark>);
    cursor = match.index + match.text.length;
  }
  return result;
}

function groupReactions(reactions, userId) {
  const grouped = new Map();
  reactions.forEach((reaction) => {
    const value = grouped.get(reaction.emoji) || { emoji: reaction.emoji, count: 0, mine: false, names: [] };
    value.count += 1;
    value.mine ||= reaction.member?.id === userId;
    if (reaction.member?.displayName) value.names.push(reaction.member.displayName);
    grouped.set(reaction.emoji, value);
  });
  return [...grouped.values()];
}

function ChatAvatar({ member }) {
  if (member?.avatarUrl) return <img className="chat-avatar" src={apiMediaUrl(member.avatarUrl)} alt={member.displayName || 'Thành viên'} />;
  const letters = String(member?.displayName || '?').split(' ').filter(Boolean).slice(-2).map((part) => part[0]).join('').toUpperCase();
  return <span className="avatar chat-avatar">{letters}</span>;
}

function PaymentModal({ payment, onClose }) {
  return <div className="modal-backdrop" onMouseDown={onClose}><section className="payment-modal" role="dialog" aria-modal="true" onMouseDown={(event) => event.stopPropagation()}>
    <button className="modal-close" onClick={onClose}>×</button><p className="eyebrow">CHUYỂN KHOẢN</p><h2>Quét mã QR</h2>
    {payment.qrImageUrl ? <img src={payment.qrImageUrl} alt="Mã QR thanh toán" /> : <div className="qr-unavailable"><span>⚠️</span><strong>Chưa tạo được mã QR</strong><p>Người nhận cần chọn lại ngân hàng trong Hồ sơ tài khoản.</p></div>}<strong>{money(payment.amount)}</strong>
    <dl><dt>Ngân hàng</dt><dd>{payment.bankName}</dd><dt>Chủ tài khoản</dt><dd>{payment.accountName}</dd><dt>Số tài khoản</dt><dd>{payment.accountNumber}</dd><dt>Nội dung</dt><dd>{payment.transferContent}</dd></dl>
  </section></div>;
}

function NotificationDetailModal({ notification, onClose }) {
  return <div className="modal-backdrop" onMouseDown={onClose}><section className="notification-detail-modal" role="dialog" aria-modal="true" aria-label="Chi tiết thông báo hệ thống" onMouseDown={(event) => event.stopPropagation()}>
    <button className="modal-close" onClick={onClose} aria-label="Đóng">×</button>
    <p className="eyebrow">THÔNG BÁO HỆ THỐNG</p>
    <h2>{notification.title}</h2>
    <p className="notification-detail-body">{notification.body}</p>
    <small>Gửi lúc {formatTime(notification.createdAt)}</small>
    <button type="button" className="primary notification-detail-close" onClick={onClose}>Đóng</button>
  </section></div>;
}

function formatTime(value) {
  if (!value) return '';
  return new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' }).format(new Date(value));
}

function money(value) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value || 0);
}
