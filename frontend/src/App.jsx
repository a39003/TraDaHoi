import { Fragment, createElement, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { apiMediaUrl, teaApi } from './api';
import AdminConsole from './AdminConsole';
import ChatPanel from './ChatPanel';
import ProfileView from './ProfileView';
import AdvancedAttendanceView from './AdvancedAttendanceView';

const React = { createElement, Fragment };
const currency = (value) => new Intl.NumberFormat('vi-VN').format(Number(value || 0)) + ' đ';
const isoDate = (date = new Date()) => new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
const monday = () => {
  const date = new Date();
  const day = date.getDay() || 7;
  date.setDate(date.getDate() - day + 1);
  return isoDate(date);
};
const monthValue = (date = new Date()) => isoDate(date).slice(0, 7);
const calendarRange = (month) => {
  const first = new Date(`${month}-01T00:00:00`);
  const last = new Date(first.getFullYear(), first.getMonth() + 1, 0);
  const start = new Date(first);
  start.setDate(start.getDate() - ((start.getDay() + 6) % 7));
  const end = new Date(last);
  end.setDate(end.getDate() + (6 - ((end.getDay() + 6) % 7)));
  return { start: isoDate(start), end: isoDate(end) };
};
const initials = (name) => String(name || '?').split(/\s+/).filter(Boolean).slice(-2).map((part) => part[0]).join('').toUpperCase();
let notificationAudioContext = null;

function unlockNotificationSound() {
  try {
    const AudioApi = window.AudioContext || window.webkitAudioContext;
    if (!AudioApi) return;
    notificationAudioContext ||= new AudioApi();
    if (notificationAudioContext.state === 'suspended') void notificationAudioContext.resume();
  } catch {
    // A device/browser without Web Audio simply keeps visual notifications.
  }
}

function playNotificationSound() {
  if (!notificationAudioContext || notificationAudioContext.state !== 'running') return;
  const now = notificationAudioContext.currentTime;
  [880, 1_175].forEach((frequency, index) => {
    const oscillator = notificationAudioContext.createOscillator();
    const gain = notificationAudioContext.createGain();
    oscillator.type = 'sine'; oscillator.frequency.value = frequency;
    gain.gain.setValueAtTime(0.0001, now + index * 0.13);
    gain.gain.exponentialRampToValueAtTime(0.09, now + index * 0.13 + 0.012);
    gain.gain.exponentialRampToValueAtTime(0.0001, now + index * 0.13 + 0.16);
    oscillator.connect(gain).connect(notificationAudioContext.destination);
    oscillator.start(now + index * 0.13); oscillator.stop(now + index * 0.13 + 0.18);
  });
}

function readStoredUser() {
  try { return JSON.parse(window.localStorage.getItem('tea-session-user') || 'null'); }
  catch { return null; }
}

export default function App() {
  const [user, setUser] = useState(readStoredUser);
  const updateUser = (next) => {
    window.localStorage.setItem('tea-session-user', JSON.stringify(next));
    setUser(next);
  };
  const logout = () => {
    if (!window.confirm('Bạn có chắc muốn đăng xuất khỏi tài khoản này không?')) return;
    window.localStorage.removeItem('tea-session-token');
    window.localStorage.removeItem('tea-session-user');
    setUser(null);
  };
  if (!user) return <AuthScreen onAuthenticated={updateUser} />;
  return <TeaApp user={user} onUserUpdated={updateUser} onLogout={logout} />;
}

function AuthScreen({ onAuthenticated }) {
  const [mode, setMode] = useState('login');
  const [form, setForm] = useState({ email: '', password: '', confirm: '', code: '' });
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const update = (field) => (event) => setForm((current) => ({ ...current, [field]: event.target.value }));
  const changeMode = (next) => { setMode(next); setMessage(''); };
  const submit = async (event) => {
    event.preventDefault(); setMessage('');
    if (mode === 'reset' && form.password !== form.confirm) {
      setMessage('Mật khẩu nhập lại chưa khớp.'); return;
    }
    setBusy(true);
    try {
      if (mode === 'login') {
        const result = await teaApi.login({ email: form.email, password: form.password });
        window.localStorage.setItem('tea-session-token', result.token);
        onAuthenticated(result.member);
      } else if (mode === 'forgot') {
        const result = await teaApi.forgotPassword({ email: form.email });
        setForm((current) => ({ ...current, code: result.code }));
        setMessage(`Mã đặt lại của bạn: ${result.code}. Mã có hiệu lực 15 phút.`);
        setMode('reset');
      } else {
        await teaApi.resetPassword({ email: form.email, code: form.code, password: form.password });
        setMessage('Đặt lại mật khẩu thành công. Hãy đăng nhập.'); setMode('login');
      }
    } catch (error) { setMessage(error.message || 'Không thể xử lý yêu cầu.'); }
    finally { setBusy(false); }
  };
  const labels = { login: 'Đăng nhập', forgot: 'Quên mật khẩu', reset: 'Đặt lại mật khẩu' };
  return <main className="auth-shell">
    <section className="auth-brand"><span>🥤</span><h1>Trà Đá Hội</h1><p>Điểm danh, chia tiền và trò chuyện cùng nhóm mỗi ngày.</p></section>
    <section className="auth-card"><h2>{labels[mode]}</h2><p className="muted">Tài khoản do Admin của nhóm tạo và quản lý.</p>
      <form onSubmit={submit} className="stack-form">
        <label>Email<input required type="email" value={form.email} onChange={update('email')} placeholder="ban@email.com" /></label>
        {mode === 'reset' && <label>Mã đặt lại<input required value={form.code} onChange={update('code')} inputMode="numeric" /></label>}
        {mode !== 'forgot' && <label>Mật khẩu<PasswordInput required minLength="8" value={form.password} onChange={update('password')} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} /></label>}
        {mode === 'reset' && <label>Nhập lại mật khẩu<PasswordInput required minLength="8" value={form.confirm} onChange={update('confirm')} autoComplete="new-password" /></label>}
        {message && <p className="form-message">{message}</p>}
        <button className="primary" disabled={busy}>{busy ? 'Đang xử lý...' : labels[mode]}</button>
      </form>
      <div className="auth-links">
        {mode === 'login' && <button onClick={() => changeMode('forgot')}>Quên mật khẩu?</button>}
        {mode !== 'login' && <button onClick={() => changeMode('login')}>Quay lại đăng nhập</button>}
      </div>
    </section>
  </main>;
}

function PasswordInput(props) {
  const [visible, setVisible] = useState(false);
  return <div className="password-field"><input {...props} type={visible ? 'text' : 'password'} /><button type="button" onClick={() => setVisible(!visible)} aria-label={visible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'} title={visible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}>{visible ? '🙈' : '👁️'}</button></div>;
}

function TeaApp({ user, onUserUpdated, onLogout }) {
  const [view, setView] = useState('attendance');
  const [members, setMembers] = useState([]);
  const [drinks, setDrinks] = useState([]);
  const [expenses, setExpenses] = useState([]);
  const [messages, setMessages] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [settlement, setSettlement] = useState(null);
  const [chatUnreadCount, setChatUnreadCount] = useState(0);
  const [attendanceEdit, setAttendanceEdit] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState('');
  const lastAlertUnreadCount = useRef(0);
  const lastChatMessageId = useRef(0);
  const lastFullChatSyncAt = useRef(0);
  const knownNotificationIds = useRef(null);
  const staticDataLoaded = useRef(false);
  const secondaryDataLoaded = useRef(false);
  const [calendarMonth, setCalendarMonth] = useState(monthValue);
  const weekStart = monday();
  const weekEnd = useMemo(() => {
    const date = new Date(`${weekStart}T00:00:00`); date.setDate(date.getDate() + 6); return isoDate(date);
  }, [weekStart]);
  const calendarBounds = useMemo(() => calendarRange(calendarMonth), [calendarMonth]);
  const expenseRangeStart = calendarBounds.start < weekStart ? calendarBounds.start : weekStart;
  const expenseRangeEnd = calendarBounds.end > weekEnd ? calendarBounds.end : weekEnd;
  const isAdmin = user.role === 'ADMIN';

  const load = useCallback(async () => {
    const shouldLoadStaticData = !staticDataLoaded.current;
    if (shouldLoadStaticData) setLoading(true);
    try {
      if (shouldLoadStaticData) {
        const [memberData, drinkData, expenseData] = await Promise.all([
          teaApi.getMembers(isAdmin), teaApi.getDrinks(isAdmin), teaApi.getExpensesRange(expenseRangeStart, expenseRangeEnd),
        ]);
        setMembers(memberData); setDrinks(drinkData); setExpenses(expenseData);
        staticDataLoaded.current = true;
      } else {
        // Khi chỉ đổi tháng, chỉ tải lại lịch điểm danh cần hiển thị.
        // Danh sách thành viên/đồ uống, chat và thông báo được giữ lại.
        const expenseData = await teaApi.getExpensesRange(expenseRangeStart, expenseRangeEnd);
        setExpenses(expenseData);
      }
    } catch (error) { setNotice(error.message || 'Không tải được dữ liệu từ máy chủ.'); }
    finally { if (shouldLoadStaticData) setLoading(false); }

    if (secondaryDataLoaded.current) return;
    secondaryDataLoaded.current = true;

    // Chat, thông báo và tổng kết không chặn giao diện chính. Điều này đặc biệt
    // quan trọng khi Render/Aiven vừa thức dậy hoặc kết nối mạng chậm.
    try {
      const [chatData, notificationData, chatUnread, settlementData] = await Promise.all([
        teaApi.getMessages(), teaApi.getNotifications(), teaApi.getChatUnread(), teaApi.calculateWeekSettlement(weekStart),
      ]);
      setMessages(chatData); setNotifications(notificationData); setChatUnreadCount(chatUnread.unreadCount || 0);
      lastAlertUnreadCount.current = chatUnread.unreadCount || 0;
      knownNotificationIds.current = new Set(notificationData.map((notification) => notification.id));
      lastChatMessageId.current = Math.max(0, ...chatData.map((message) => message.id || 0));
      lastFullChatSyncAt.current = Date.now();
      setSettlement(settlementData);
    } catch {
      // Giao diện chính vẫn dùng được; các lần đồng bộ nền sau sẽ thử lại.
      secondaryDataLoaded.current = false;
    }
  }, [isAdmin, user.id, expenseRangeStart, expenseRangeEnd]);

  const refreshAlerts = useCallback(async () => {
    try {
      const [notificationData, chatUnread] = await Promise.all([
        teaApi.getNotifications(), teaApi.getChatUnread(),
      ]);
      const unreadCount = chatUnread.unreadCount || 0;
      const nextNotificationIds = new Set(notificationData.map((notification) => notification.id));
      const hasNewSystemNotification = knownNotificationIds.current !== null
        && notificationData.some((notification) => !knownNotificationIds.current.has(notification.id));
      const hasNewChatMessage = unreadCount > lastAlertUnreadCount.current;
      setNotifications(notificationData); setChatUnreadCount(unreadCount);

      // Only fetch message previews for the notification bell when a new chat
      // notification actually arrives. Previously this downloaded 50 messages
      // every five seconds even while the chat screen was closed.
      if (hasNewChatMessage) {
        const latestMessages = await teaApi.getMessages({ limit: 6 });
        setMessages((current) => [...new Map([...current, ...latestMessages].map((message) => [message.id, message])).values()]
          .sort((first, second) => new Date(first.createdAt) - new Date(second.createdAt))
          .slice(-50));
      }
      if (hasNewChatMessage || hasNewSystemNotification) playNotificationSound();
      lastAlertUnreadCount.current = unreadCount;
      knownNotificationIds.current = nextNotificationIds;
    } catch {
      // Giữ dữ liệu hiện tại nếu lần kiểm tra thông báo nền bị gián đoạn.
    }
  }, [user.id]);

  const refreshChat = useCallback(async () => {
    try {
      const now = Date.now();
      const fullSync = !lastChatMessageId.current || now - lastFullChatSyncAt.current >= 60_000;
      const chatData = fullSync
        ? await teaApi.getMessages()
        : await teaApi.getMessages({ afterId: lastChatMessageId.current });

      if (fullSync) {
        setMessages(chatData);
        lastChatMessageId.current = Math.max(0, ...chatData.map((message) => message.id || 0));
        lastFullChatSyncAt.current = now;
      } else if (chatData.length) {
        lastChatMessageId.current = Math.max(lastChatMessageId.current, ...chatData.map((message) => message.id || 0));
        setMessages((current) => [...new Map([...current, ...chatData].map((message) => [message.id, message])).values()]
          .sort((first, second) => new Date(first.createdAt) - new Date(second.createdAt))
          .slice(-50));
      }
    } catch {
      // Keep the current messages when a background refresh is interrupted.
    }
  }, [user.id]);

  const refreshSettlement = useCallback(async () => {
    try {
      setSettlement(await teaApi.calculateWeekSettlement(weekStart));
    } catch {
      // Keep the current settlement when a background refresh is interrupted.
    }
  }, [weekStart]);

  const applyAttendanceChange = useCallback((change) => {
    if (change?.type === 'delete') {
      setExpenses((current) => current.filter((expense) => expense.id !== change.id));
    } else if (change?.expense) {
      setExpenses((current) => [...current.filter((expense) => expense.id !== change.expense.id), change.expense]
        .sort((first, second) => String(first.orderDate).localeCompare(String(second.orderDate))));
    }
    void refreshSettlement();
  }, [refreshSettlement]);

  const deleteAttendanceFromCalendar = useCallback(async (expense) => {
    if (!window.confirm('Bạn có chắc muốn xóa điểm danh này không? Dữ liệu sẽ bị xóa khỏi hệ thống.')) return false;
    try {
      await teaApi.deleteQuickAttendance(expense.id);
      applyAttendanceChange({ type: 'delete', id: expense.id });
      flash('Đã xóa điểm danh.');
      return true;
    } catch (error) {
      flash(error.message || 'Không thể xóa điểm danh.');
      return false;
    }
  }, [applyAttendanceChange]);

  const editAttendanceFromCalendar = useCallback((expense) => {
    setAttendanceEdit({ expense, token: Date.now() });
    setView('attendance');
  }, []);

  const applyAdminChange = useCallback((change) => {
    if (!change) return;
    const updateList = (setter) => {
      setter((current) => change.type === 'delete'
        ? current.filter((item) => item.id !== change.id)
        : [...current.filter((item) => item.id !== change.item.id), change.item]);
    };
    if (change.kind === 'drink') updateList(setDrinks);
    if (change.kind === 'member') updateList(setMembers);
  }, []);

  const mergeLiveMessage = useCallback((message) => {
    if (!message?.id) return;
    lastChatMessageId.current = Math.max(lastChatMessageId.current, message.id);
    setMessages((current) => [...current.filter((item) => item.id !== message.id), message]
      .sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
      .slice(-50));
  }, []);

  useEffect(() => { load(); }, [load]);
  useEffect(() => {
    const unlock = () => unlockNotificationSound();
    window.addEventListener('pointerdown', unlock, { once: true });
    window.addEventListener('keydown', unlock, { once: true });
    return () => { window.removeEventListener('pointerdown', unlock); window.removeEventListener('keydown', unlock); };
  }, []);
  useEffect(() => {
    const id = window.setInterval(refreshAlerts, 5000);
    return () => window.clearInterval(id);
  }, [refreshAlerts]);

  const flash = (message) => {
    setNotice(message);
    window.setTimeout(() => setNotice(''), 3800);
  };
  const records = useMemo(() => expenses.flatMap((expense) => expense.items.map((item) => ({
    id: item.id, expenseId: expense.id, date: expense.orderDate, payer: expense.payer,
    member: item.consumer, drink: item.drinkName, amount: item.lineTotal, note: expense.note,
  }))), [expenses]);
  const visibleRecords = isAdmin ? records : records.filter((item) => item.member.id === user.id);
  const todayRecords = visibleRecords.filter((item) => item.date === isoDate());
  const todayExpenses = expenses.filter((item) => item.orderDate === isoDate());
  const weekRecords = records.filter((item) => item.date >= weekStart && item.date <= weekEnd);
  const activeMembers = members.filter((member) => member.active);
  const balances = useMemo(() => {
    const values = Object.fromEntries(members.map((member) => [member.id, { member, paid: 0, consumed: 0 }]));
    weekRecords.forEach((item) => {
      if (values[item.member.id]) values[item.member.id].consumed += item.amount;
      if (values[item.payer.id]) values[item.payer.id].paid += item.amount;
    });
    return Object.values(values).map((item) => ({ ...item, net: item.paid - item.consumed }));
  }, [members, weekRecords]);
  const titles = { attendance: 'Điểm danh hôm nay', week: 'Lịch uống nước', settlement: 'Tổng kết tuần', chat: 'Trò chuyện', profile: 'Tài khoản', admin: 'Quản trị nhóm' };

  return <div className="tea-app">
    <aside className="app-sidebar">
      <div className="logo">🥤 <span>Trà Đá Hội<small>{isAdmin ? 'Admin' : 'Thành viên'}</small></span></div>
      <nav>
        <NavButton icon="✓" active={view === 'attendance'} onClick={() => setView('attendance')}>Điểm danh</NavButton>
        <NavButton icon="▦" active={view === 'week'} onClick={() => setView('week')}>Lịch tháng</NavButton>
        <NavButton icon="₫" active={view === 'settlement'} onClick={() => setView('settlement')}>Tổng kết</NavButton>
        <NavButton icon="💬" active={view === 'chat'} onClick={() => setView('chat')}>Chat</NavButton>
        <NavButton icon="⚙" active={view === 'profile'} onClick={() => setView('profile')}>Tài khoản</NavButton>
        {isAdmin && <NavButton icon="🛡" active={view === 'admin'} onClick={() => setView('admin')}>Quản trị</NavButton>}
      </nav>
      <div className="side-user"><Avatar member={user} /><span>{user.displayName}<small>{isAdmin ? 'Quản trị viên' : 'Thành viên'}</small></span><button onClick={onLogout}>Đăng xuất</button></div>
    </aside>
    <main className="app-main">
      <header className="app-header"><div><p className="eyebrow">NHÓM TRÀ ĐÁ</p><h1>{titles[view]}</h1></div><NotificationBell user={user} messages={messages} notifications={notifications} chatUnreadCount={chatUnreadCount} chatOpen={view === 'chat'} onOpenChat={() => setView('chat')} onRefresh={refreshAlerts} /></header>
      {loading ? <p className="loading">Đang tải dữ liệu...</p> : <>
        {view === 'attendance' && <AdvancedAttendanceView user={user} members={activeMembers} drinks={drinks.filter((drink) => drink.active)} todayExpenses={todayExpenses} isAdmin={isAdmin} onSaved={applyAttendanceChange} editRequest={attendanceEdit} onEditHandled={() => setAttendanceEdit(null)} flash={flash} />}
        {view === 'week' && <MonthCalendar records={visibleRecords} expenses={expenses} user={user} month={calendarMonth} onMonthChange={setCalendarMonth} isAdmin={isAdmin} onEditExpense={editAttendanceFromCalendar} onDeleteExpense={deleteAttendanceFromCalendar} />}
        {view === 'settlement' && <SettlementView balances={balances} settlement={settlement} user={user} weekStart={weekStart} weekEnd={settlement?.weekEnd || weekEnd} isAdmin={isAdmin} />}
        {view === 'chat' && <ChatPanel user={user} members={activeMembers} messages={messages} notifications={notifications} onSaved={refreshAlerts} onRefreshChat={refreshChat} onMessageChanged={mergeLiveMessage} flash={flash} />}
        {view === 'profile' && <ProfileView user={user} onUserUpdated={(next) => {
          onUserUpdated(next);
          setMembers((current) => [...current.filter((member) => member.id !== next.id), next]);
        }} flash={flash} />}
        {view === 'admin' && isAdmin && <AdminConsole members={members} drinks={drinks} currentUser={user} onSaved={applyAdminChange} flash={flash} />}
      </>}
    </main>
    {notice && <div className="toast">{notice}</div>}
  </div>;
}

function NavButton({ icon, active, onClick, children }) {
  return <button className={active ? 'active' : ''} onClick={onClick}><span>{icon}</span>{children}</button>;
}

function Avatar({ member }) {
  return member?.avatarUrl
    ? <img className="avatar avatar-image" src={apiMediaUrl(member.avatarUrl)} alt={member.displayName || 'Thành viên'} />
    : <span className="avatar">{initials(member?.displayName)}</span>;
}

function NotificationBell({ user, messages, notifications, chatUnreadCount, chatOpen, onOpenChat, onRefresh }) {
  const [open, setOpen] = useState(false);
  const bellRef = useRef(null);
  useEffect(() => {
    if (!open) return undefined;
    const close = (event) => { if (!bellRef.current?.contains(event.target)) setOpen(false); };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, [open]);

  const markChatSeen = async () => {
    if (!chatUnreadCount) return;
    try {
      await teaApi.markChatRead();
      await onRefresh();
    } catch {
      // Lần kiểm tra nền tiếp theo sẽ thử đồng bộ lại trạng thái đã đọc.
    }
  };
  useEffect(() => { if (chatOpen && chatUnreadCount) markChatSeen(); }, [chatOpen, chatUnreadCount]);

  const unreadSystem = notifications.filter((item) => !item.readAt);
  const unreadCount = unreadSystem.length + chatUnreadCount;
  const recentChats = [...messages].filter((item) => item.sender?.id !== user.id).reverse().slice(0, Math.min(chatUnreadCount, 6));
  const recentSystem = notifications.slice(0, 6);

  const toggle = () => {
    const next = !open;
    setOpen(next);
  };
  const openSystemNotification = async (notification) => {
    try {
      if (!notification.readAt) await teaApi.markNotificationRead(notification.id);
      await onRefresh();
      setOpen(false);
      onOpenChat();
    } catch {
      // Giữ thông báo lại để người dùng có thể thử đánh dấu đã xem lần nữa.
    }
  };

  return <div className="notification-bell" ref={bellRef}>
    <button type="button" className="bell-button" onClick={toggle} aria-label={`Thông báo${unreadCount ? `, ${unreadCount} chưa đọc` : ''}`} aria-expanded={open}>🔔{unreadCount > 0 && <b>{unreadCount > 99 ? '99+' : unreadCount}</b>}</button>
    {open && <section className="notification-popover">
      <div className="notification-popover-head"><div><strong>Thông báo</strong><small>{unreadCount ? `${unreadCount} thông báo mới` : 'Bạn đã xem hết thông báo'}</small></div><button type="button" onClick={() => setOpen(false)}>×</button></div>
      <div className="notification-feed">
        {recentSystem.map((item) => <button type="button" className={`notification-feed-item system ${!item.readAt ? 'unread' : ''}`} key={`system-${item.id}`} onClick={() => openSystemNotification(item)}><span>🤖</span><div><strong>Hệ thống · {item.title}</strong><p>{item.body}</p><small>{formatNotificationTime(item.createdAt)}</small></div></button>)}
        {recentChats.map((item) => <button type="button" className="notification-feed-item chat unread" key={`chat-${item.id}`} onClick={() => { markChatSeen(); setOpen(false); onOpenChat(); }}><Avatar member={item.sender} /><div><strong>{item.sender?.displayName} nhắn trong nhóm</strong><p>{item.content || (item.attachments?.length ? `Đã gửi ${item.attachments.length} ảnh` : 'Tin nhắn')}</p><small>{formatNotificationTime(item.createdAt)}</small></div></button>)}
        {recentSystem.length === 0 && recentChats.length === 0 && <p className="empty">Chưa có thông báo nào.</p>}
      </div>
      <button type="button" className="notification-view-chat" onClick={() => { markChatSeen(); setOpen(false); onOpenChat(); }}>Mở Chat và thông báo hệ thống →</button>
    </section>}
  </div>;
}

function formatNotificationTime(value) {
  if (!value) return '';
  return new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' }).format(new Date(value));
}

function AttendanceView({ user, members, drinks, todayRecords, isAdmin, onSaved, flash }) {
  const [drinkId, setDrinkId] = useState('');
  const [payerId, setPayerId] = useState(String(user.id));
  const [shared, setShared] = useState(false);
  const [selected, setSelected] = useState([user.id]);
  const [saving, setSaving] = useState(false);
  useEffect(() => { if (!drinkId && drinks[0]) setDrinkId(String(drinks[0].id)); }, [drinks, drinkId]);
  const drink = drinks.find((item) => String(item.id) === drinkId);
  const toggle = (id) => setSelected((current) => current.includes(id) ? current.filter((item) => item !== id) : [...current, id]);
  const save = async (event) => {
    event.preventDefault();
    const consumers = shared ? selected : [user.id];
    if (!drink || consumers.length === 0) return flash('Hãy chọn đồ uống và người uống.');
    setSaving(true);
    try {
      const base = Math.floor(drink.price / consumers.length);
      const remainder = drink.price - base * consumers.length;
      await teaApi.createExpense({
        orderDate: isoDate(), payerMemberId: Number(payerId),
        note: shared ? `Uống chung ${consumers.length} người` : 'Điểm danh cá nhân',
        items: consumers.map((id, index) => ({ consumerMemberId: id, drinkName: drink.name, unitPrice: base + (index === 0 ? remainder : 0), quantity: 1 })),
      });
      flash('Đã lưu điểm danh và người trả tiền.'); await onSaved();
    } catch (error) { flash(error.message || 'Không thể lưu điểm danh.'); }
    finally { setSaving(false); }
  };
  return <div className="two-column">
    <section className="panel attendance-form"><div className="panel-title"><span>📝</span><div><h2>Hôm nay bạn có đi uống không?</h2><p>Bạn chỉ thấy lịch điểm danh của mình; Admin xem được cả nhóm.</p></div></div>
      <form onSubmit={save} className="stack-form">
        <label>Đồ uống<select value={drinkId} onChange={(event) => setDrinkId(event.target.value)}>{drinks.map((item) => <option key={item.id} value={item.id}>{item.icon || '🥤'} {item.name} - {currency(item.price)}</option>)}</select></label>
        <label>Người đã trả tiền<select value={payerId} onChange={(event) => setPayerId(event.target.value)}>{members.map((member) => <option key={member.id} value={member.id}>{member.displayName}</option>)}</select></label>
        <label className="check-row"><input type="checkbox" checked={shared} onChange={(event) => { setShared(event.target.checked); if (!event.target.checked) setSelected([user.id]); }} /> Uống chung, chia đều giá đồ uống</label>
        {shared && <fieldset><legend>Uống chung với ai?</legend><div className="member-checks">{members.map((member) => <label key={member.id} className="check-row"><input type="checkbox" checked={selected.includes(member.id)} onChange={() => toggle(member.id)} />{member.displayName}</label>)}</div><p className="split-note">Mỗi người khoảng: {currency(Math.floor((drink?.price || 0) / Math.max(selected.length, 1)))}</p></fieldset>}
        <button className="primary" disabled={saving || !drink}>{saving ? 'Đang lưu...' : 'Xác nhận điểm danh'}</button>
      </form>
    </section>
    <section className="panel"><div className="panel-title"><span>☀️</span><div><h2>{isAdmin ? 'Chi tiết hôm nay' : 'Điểm danh của bạn hôm nay'}</h2><p>{todayRecords.length} lượt uống</p></div></div><RecordList records={todayRecords} /></section>
  </div>;
}

function RecordList({ records }) {
  if (records.length === 0) return <p className="empty">Chưa có lượt điểm danh.</p>;
  return <div className="record-list">{records.map((record) => <article className="record" key={record.id}>
    <Avatar member={record.member} /><div><strong>{record.member.displayName}</strong><span>{record.drink}{record.note?.startsWith('Uống chung') ? ` · ${record.note}` : ''}</span></div>
    <div><strong>{currency(record.amount)}</strong><small>{record.payer.displayName} trả</small></div>
  </article>)}</div>;
}

function MonthCalendar({ records, expenses, user, month, onMonthChange, isAdmin, onEditExpense, onDeleteExpense }) {
  const bounds = useMemo(() => calendarRange(month), [month]);
  const grouped = useMemo(() => records.reduce((all, record) => {
    if (record.date >= bounds.start && record.date <= bounds.end) {
      all[record.date] = [...(all[record.date] || []), record];
    }
    return all;
  }, {}), [records, bounds.start, bounds.end]);
  const days = useMemo(() => {
    const start = new Date(`${bounds.start}T00:00:00`);
    return Array.from({ length: 42 }, (_, index) => {
      const date = new Date(start); date.setDate(date.getDate() + index); return isoDate(date);
    });
  }, [bounds.start]);
  const [selectedDate, setSelectedDate] = useState(null);
  const moveMonth = (amount) => {
    const date = new Date(`${month}-01T00:00:00`); date.setMonth(date.getMonth() + amount); onMonthChange(monthValue(date));
  };
  const monthLabel = new Intl.DateTimeFormat('vi-VN', { month: 'long', year: 'numeric' }).format(new Date(`${month}-01T00:00:00`));
  return <>
  <section className="panel calendar-panel">
    <div className="calendar-header">
      <div className="panel-title"><span>📅</span><div><h2>{isAdmin ? 'Lịch uống nước của cả nhóm' : 'Lịch uống nước của bạn'}</h2><p>Chọn tháng và bấm vào một ngày để xem đồ uống, giá tiền và người đã trả.</p></div></div>
      <div className="month-controls"><button className="outline" onClick={() => moveMonth(-1)} aria-label="Tháng trước">‹</button><label><span>Tháng đang xem</span><input type="month" value={month} onChange={(event) => onMonthChange(event.target.value)} /></label><button className="outline" onClick={() => moveMonth(1)} aria-label="Tháng sau">›</button></div>
    </div>
    <div className="calendar-month-title"><strong>{monthLabel}</strong><span>{Object.values(grouped).flat().filter((item) => item.date.startsWith(month)).length} lượt điểm danh</span></div>
    <div className="calendar-weekdays">{['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'].map((day) => <span key={day}>{day}</span>)}</div>
    <div className="month-grid">{days.map((date) => {
      const daily = grouped[date] || [];
      const inMonth = date.startsWith(month);
      const dayTotal = daily.reduce((sum, item) => sum + item.amount, 0);
      return <button key={date} type="button" onClick={() => setSelectedDate(date)} aria-label={`Xem chi tiết ngày ${date}`} className={`calendar-day ${inMonth ? '' : 'outside-month'} ${date === isoDate() ? 'today' : ''} ${date === selectedDate ? 'selected' : ''} ${daily.length ? 'has-records' : ''}`}>
        <span className="day-number">{Number(date.slice(-2))}</span>
        {daily.length > 0 ? <><span className="day-count">{daily.length} lượt</span><strong>{currency(dayTotal)}</strong><span className="day-preview">{daily.slice(0, 2).map((item) => item.drink).join(', ')}</span></> : <span className="day-empty">—</span>}
      </button>;
    })}</div>
  </section>
  {selectedDate && <DayDetailModal date={selectedDate} records={grouped[selectedDate] || []} expenses={expenses.filter((expense) => expense.orderDate === selectedDate)} user={user} isAdmin={isAdmin} onEditExpense={onEditExpense} onDeleteExpense={onDeleteExpense} onClose={() => setSelectedDate(null)} />}
  </>;
}

function DayDetailModal({ date, records, expenses, user, isAdmin, onEditExpense, onDeleteExpense, onClose }) {
  const [deletingExpenseId, setDeletingExpenseId] = useState(null);
  useEffect(() => {
    const close = (event) => { if (event.key === 'Escape') onClose(); };
    document.addEventListener('keydown', close);
    return () => document.removeEventListener('keydown', close);
  }, [onClose]);
  const label = new Intl.DateTimeFormat('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' }).format(new Date(`${date}T00:00:00`));
  const total = records.reduce((sum, item) => sum + item.amount, 0);
  return <div className="modal-backdrop" onMouseDown={onClose}><section className="day-detail-modal" role="dialog" aria-modal="true" aria-label={`Chi tiết ngày ${label}`} onMouseDown={(event) => event.stopPropagation()}>
    <button className="modal-close" onClick={onClose} aria-label="Đóng chi tiết">×</button>
    <div className="day-detail-title"><span>📅</span><div><p className="eyebrow">CHI TIẾT ĐIỂM DANH</p><h2>{label}</h2><p>{isAdmin ? 'Danh sách thành viên đi uống, đồ uống, giá tiền và người đã trả.' : 'Đồ uống, số tiền và người đã trả cho lượt uống của bạn.'}</p></div></div>
    <div className="day-detail-summary"><span><small>Số lượt</small><b>{records.length}</b></span><span><small>Tổng tiền</small><b>{currency(total)}</b></span></div>
    <div className="day-detail-list">{records.length ? <RecordList records={records} /> : <p className="empty">Không có lượt uống nước trong ngày này.</p>}</div>
    {expenses.length > 0 && <div className="day-detail-actions">{expenses.map((expense) => {
      const canManage = isAdmin || expense.createdBy?.id === user.id;
      if (!canManage) return null;
      return <div className="day-detail-order-actions" key={expense.id}>
        <span>Đơn #{expense.id}{expense.createdBy?.id === user.id ? ' · Bạn đã tạo' : ` · ${expense.createdBy?.displayName || 'Admin'} đã tạo`}</span>
        <div><button type="button" className="outline small" disabled={deletingExpenseId !== null} onClick={() => { onEditExpense(expense); onClose(); }}>✎ Sửa</button><button type="button" className="danger small" disabled={deletingExpenseId !== null} onClick={async () => { setDeletingExpenseId(expense.id); try { if (await onDeleteExpense(expense)) onClose(); } finally { setDeletingExpenseId(null); } }}>{deletingExpenseId === expense.id ? 'Đang xóa...' : 'Xóa'}</button></div>
      </div>;
    })}</div>}
  </section></div>;
}

function SettlementView({ balances, settlement, user, weekStart, weekEnd, isAdmin }) {
  const calculatedBalances = settlement?.balances?.map((item) => ({
    member: item.member, consumed: item.consumedAmount, paid: item.paidAmount, net: item.netAmount,
  })) || balances;
  const transfers = settlement?.transfers || buildSettlementTransfers(calculatedBalances);
  const visibleBalances = isAdmin ? calculatedBalances : calculatedBalances.filter((item) => item.member.id === user.id);
  const visibleTransfers = isAdmin ? transfers : transfers.filter((item) => item.debtor.id === user.id || item.creditor.id === user.id);
  const totalConsumed = isAdmin
    ? calculatedBalances.reduce((sum, item) => sum + item.consumed, 0)
    : visibleBalances.reduce((sum, item) => sum + item.consumed, 0);
  const activeMemberCount = calculatedBalances.filter((item) => item.consumed > 0 || item.paid > 0).length;
  const totalReceivable = visibleTransfers.reduce((sum, item) => sum + item.amount, 0);
  const dateLabel = `${formatShortDate(weekStart)} – ${formatShortDate(weekEnd)}`;
  return <div className="settlement-layout">
    <section className="settlement-hero">
      <div><p className="eyebrow">TUẦN HIỆN TẠI · {dateLabel}</p><h2>{isAdmin ? 'Bức tranh chi tiêu của cả nhóm' : 'Chi tiêu trà đá của bạn'}</h2><p>Các khoản hoàn tiền sẽ được hệ thống gửi riêng trong mục Chat sau khi tuần được chốt.</p></div>
      <span>🧾</span>
    </section>
    <div className="settlement-stats">
      <article><span>🥤</span><div><small>Tổng đã uống</small><strong>{currency(totalConsumed)}</strong></div></article>
      <article><span>👥</span><div><small>{isAdmin ? 'Thành viên phát sinh' : 'Khoản cần thanh toán'}</small><strong>{isAdmin ? `${activeMemberCount} người` : `${visibleTransfers.length} khoản`}</strong></div></article>
      <article><span>↔</span><div><small>Cần hoàn lại</small><strong>{currency(totalReceivable)}</strong></div></article>
    </div>
    <div className="settlement-columns">
      <section className="panel settlement-members"><div className="panel-title"><span>👥</span><div><h2>{isAdmin ? 'Thanh toán của từng thành viên' : 'Thanh toán của bạn'}</h2><p>Hiển thị rõ người nhận, người trả và số tiền cần chuyển.</p></div></div>
        <div className="balance-list">{visibleBalances.length === 0 ? <p className="empty">Chưa có dữ liệu trong tuần này.</p> : visibleBalances.map((item) => {
          const related = transfers.filter((transfer) => transfer.debtor.id === item.member.id || transfer.creditor.id === item.member.id);
          return <article key={item.member.id}><Avatar member={item.member} /><span><strong>{item.member.displayName}</strong><small>Đã uống {currency(item.consumed)}</small><span className="settlement-transfer-lines">{related.length === 0
            ? <em>Không cần thanh toán thêm</em>
            : related.map((transfer, index) => transfer.creditor.id === item.member.id
              ? <em className="receive-line" key={`${transfer.debtor.id}-${transfer.creditor.id}-${index}`}>Nhận {currency(transfer.amount)} từ {transfer.debtor.displayName}</em>
              : <em className="owe-line" key={`${transfer.debtor.id}-${transfer.creditor.id}-${index}`}>Trả {currency(transfer.amount)} cho {transfer.creditor.displayName}</em>)}</span></span><b className={item.net >= 0 ? 'receive' : 'owe'}>{item.net > 0 ? `Tổng nhận ${currency(item.net)}` : item.net < 0 ? `Tổng trả ${currency(-item.net)}` : 'Đã cân bằng'}</b></article>;
        })}</div>
      </section>
      <section className="panel settlement-note"><span>🤖</span><p className="eyebrow">TRỢ LÝ HỆ THỐNG</p><h2>Tự động nhắc thanh toán</h2><p>Người cần trả sẽ thấy số tiền, mã thành viên nhận tiền và mã QR. Người đã ứng tiền sẽ thấy danh sách từng thành viên phải hoàn cùng số tiền tương ứng.</p><div className="settlement-note-badge">✓ Không cần tổng hợp thủ công</div></section>
    </div>
  </div>;
}

function buildSettlementTransfers(balances) {
  const debtors = balances.filter((item) => item.net < 0).map((item) => ({ member: item.member, remaining: -item.net }));
  const creditors = balances.filter((item) => item.net > 0).map((item) => ({ member: item.member, remaining: item.net }));
  const transfers = [];
  let debtorIndex = 0;
  let creditorIndex = 0;
  while (debtorIndex < debtors.length && creditorIndex < creditors.length) {
    const debtor = debtors[debtorIndex];
    const creditor = creditors[creditorIndex];
    const amount = Math.min(debtor.remaining, creditor.remaining);
    transfers.push({ debtor: debtor.member, creditor: creditor.member, amount });
    debtor.remaining -= amount;
    creditor.remaining -= amount;
    if (debtor.remaining === 0) debtorIndex += 1;
    if (creditor.remaining === 0) creditorIndex += 1;
  }
  return transfers;
}

function formatShortDate(value) {
  return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit' }).format(new Date(`${value}T00:00:00`));
}
