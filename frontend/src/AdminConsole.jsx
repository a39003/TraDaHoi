import { createElement, Fragment, useEffect, useMemo, useRef, useState } from 'react';
import { apiMediaUrl, teaApi } from './api';
import BankSelect from './BankSelect';

const React = { createElement, Fragment };
const money = (value) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value || 0);
const DRINK_ICONS = ['🥤', '🧋', '☕', '🍵', '🫖', '🥛', '💧', '🧃', '🥥', '🍋', '🍊', '🍓', '🍑', '🫐', '🍹', '🧊', '🍺', '🥂'];

export default function AdminConsole({ members, drinks, currentUser, onSaved, flash }) {
  const [tab, setTab] = useState('drinks');
  return <section className="admin-console">
    <div className="admin-tabs" role="tablist">
      <button className={tab === 'drinks' ? 'active' : ''} onClick={() => setTab('drinks')}>🥤 Đồ uống</button>
      <button className={tab === 'members' ? 'active' : ''} onClick={() => setTab('members')}>👥 Thành viên</button>
      <button className={tab === 'attendance' ? 'active' : ''} onClick={() => setTab('attendance')}>⏱️ Giờ điểm danh</button>
      <button className={tab === 'notifications' ? 'active' : ''} onClick={() => setTab('notifications')}>🔔 Thông báo tuần</button>
    </div>
    {tab === 'drinks' && <DrinkManager drinks={drinks} onSaved={onSaved} flash={flash} />}
    {tab === 'members' && <MemberManager members={members} currentUser={currentUser} onSaved={onSaved} flash={flash} />}
    {tab === 'attendance' && <AttendanceSettings flash={flash} />}
    {tab === 'notifications' && <NotificationSettings flash={flash} />}
  </section>;
}

function DrinkManager({ drinks, onSaved, flash }) {
  const [newDrink, setNewDrink] = useState({ name: '', price: '', icon: '🥤' });
  const add = async (event) => {
    event.preventDefault();
    try {
      await teaApi.createDrink({ ...newDrink, price: Number(newDrink.price), active: true });
      setNewDrink({ name: '', price: '', icon: '🥤' });
      flash('Đã thêm đồ uống.');
      await onSaved();
    } catch (error) { flash(error.message || 'Không thể thêm đồ uống.'); }
  };
  return <section className="panel admin-drink-panel">
    <div className="panel-title"><span>🥤</span><div><h2>Danh sách đồ uống</h2><p>Chọn biểu tượng, đặt tên và giá cố định dùng khi thành viên điểm danh.</p></div></div>
    <form className="admin-create-row" onSubmit={add}>
      <IconPicker value={newDrink.icon} onChange={(icon) => setNewDrink({ ...newDrink, icon })} label="Chọn icon cho đồ uống mới" />
      <input required value={newDrink.name} onChange={(event) => setNewDrink({ ...newDrink, name: event.target.value })} placeholder="Tên đồ uống" />
      <input required type="number" min="0" step="1000" value={newDrink.price} onChange={(event) => setNewDrink({ ...newDrink, price: event.target.value })} placeholder="Giá tiền" />
      <button className="primary">Thêm đồ uống</button>
    </form>
    <div className="admin-list">{drinks.map((drink) => <DrinkRow key={drink.id} drink={drink} onSaved={onSaved} flash={flash} />)}</div>
  </section>;
}

function IconPicker({ value, onChange, label }) {
  const [open, setOpen] = useState(false);
  const pickerRef = useRef(null);
  useEffect(() => {
    if (!open) return undefined;
    const close = (event) => { if (!pickerRef.current?.contains(event.target)) setOpen(false); };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, [open]);
  return <div className="drink-icon-picker" ref={pickerRef}>
    <button type="button" className={`icon-picker-trigger ${open ? 'open' : ''}`} onClick={() => setOpen(!open)} aria-label={label} aria-expanded={open}>
      <span>{value || '🥤'}</span><small>⌄</small>
    </button>
    {open && <div className="drink-icon-menu" role="listbox" aria-label="Danh sách biểu tượng đồ uống">
      {DRINK_ICONS.map((icon) => <button type="button" role="option" aria-selected={value === icon} className={value === icon ? 'selected' : ''} key={icon} onClick={() => { onChange(icon); setOpen(false); }}>{icon}</button>)}
    </div>}
  </div>;
}

function DrinkRow({ drink, onSaved, flash }) {
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState(drinkForm(drink));
  useEffect(() => setForm(drinkForm(drink)), [drink]);
  const save = async () => {
    try {
      await teaApi.updateDrink(drink.id, { ...form, price: Number(form.price) });
      setEditing(false); flash('Đã cập nhật đồ uống.'); await onSaved();
    } catch (error) { flash(error.message || 'Không thể sửa đồ uống.'); }
  };
  const remove = async () => {
    if (!window.confirm(`Bạn có chắc muốn xóa "${drink.name}" khỏi danh sách đồ uống không?`)) return;
    try { await teaApi.deleteDrink(drink.id); flash('Đã xóa đồ uống.'); await onSaved(); }
    catch (error) { flash(error.message || 'Không thể xóa đồ uống.'); }
  };
  return <article className={`admin-row drink-admin-row ${!drink.active ? 'inactive' : ''}`}>
    {editing ? <>
      <IconPicker value={form.icon} onChange={(icon) => setForm({ ...form, icon })} label={`Chọn icon cho ${drink.name}`} />
      <input value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
      <input type="number" min="0" step="1000" value={form.price} onChange={(event) => setForm({ ...form, price: event.target.value })} />
      <label className="mini-check"><input type="checkbox" checked={form.active} onChange={(event) => setForm({ ...form, active: event.target.checked })} /> Đang dùng</label>
      <div className="row-actions"><button type="button" className="primary small" onClick={save}>Lưu</button><button type="button" className="outline small" onClick={() => { setForm(drinkForm(drink)); setEditing(false); }}>Hủy</button></div>
    </> : <>
      <span className="row-icon">{drink.icon || '🥤'}</span>
      <div className="row-main"><strong>{drink.name}</strong><small><i className={`status-dot ${drink.active ? '' : 'off'}`} />{drink.active ? 'Đang hiển thị khi điểm danh' : 'Đã ẩn'}</small></div>
      <b>{money(drink.price)}</b>
      <div className="row-actions"><button className="outline small" onClick={() => setEditing(true)}>✎ Sửa</button><button className="danger small" onClick={remove}>Xóa</button></div>
    </>}
  </article>;
}

function MemberManager({ members, currentUser, onSaved, flash }) {
  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState('all');
  const [creating, setCreating] = useState(false);
  const [newAccount, setNewAccount] = useState({ displayName: '', email: '', password: '', role: 'MEMBER' });
  const filtered = useMemo(() => {
    const keyword = query.trim().toLocaleLowerCase('vi');
    return [...members]
      .filter((member) => filter === 'all' || (filter === 'active' ? member.active : !member.active))
      .filter((member) => !keyword || `${member.displayName} ${member.email || ''}`.toLocaleLowerCase('vi').includes(keyword))
      .sort((a, b) => Number(b.active) - Number(a.active) || a.displayName.localeCompare(b.displayName, 'vi'));
  }, [members, query, filter]);
  const activeCount = members.filter((member) => member.active).length;
  const adminCount = members.filter((member) => member.active && member.role === 'ADMIN').length;
  const createAccount = async (event) => {
    event.preventDefault(); setCreating(true);
    try {
      await teaApi.createMember(newAccount);
      setNewAccount({ displayName: '', email: '', password: '', role: 'MEMBER' });
      flash('Đã tạo tài khoản thành viên.');
      await onSaved();
    } catch (error) { flash(error.message || 'Không thể tạo tài khoản.'); }
    finally { setCreating(false); }
  };
  return <section className="panel member-manager-panel">
    <div className="member-manager-heading">
      <div className="panel-title"><span>👥</span><div><h2>Quản lý thành viên</h2><p>Quản lý hồ sơ, thông tin nhận tiền, trạng thái và quyền truy cập.</p></div></div>
      <div className="member-stats"><span><b>{members.length}</b>Tổng</span><span><b>{activeCount}</b>Hoạt động</span><span><b>{adminCount}</b>Admin</span></div>
    </div>
    <form className="member-create-account" onSubmit={createAccount}>
      <div className="member-create-title"><span>＋</span><div><strong>Tạo tài khoản mới</strong><small>Chỉ Admin mới có thể cấp tài khoản đăng nhập cho thành viên.</small></div></div>
      <label>Họ và tên<input required maxLength="100" value={newAccount.displayName} onChange={(event) => setNewAccount({ ...newAccount, displayName: event.target.value })} placeholder="Nguyễn Văn A" /></label>
      <label>Email đăng nhập<input required type="email" maxLength="150" value={newAccount.email} onChange={(event) => setNewAccount({ ...newAccount, email: event.target.value })} placeholder="thanhvien@email.com" /></label>
      <label>Mật khẩu ban đầu<AdminPasswordInput required minLength="8" maxLength="72" value={newAccount.password} onChange={(event) => setNewAccount({ ...newAccount, password: event.target.value })} /></label>
      <label>Vai trò<select value={newAccount.role} onChange={(event) => setNewAccount({ ...newAccount, role: event.target.value })}><option value="MEMBER">Thành viên</option><option value="ADMIN">Admin</option></select></label>
      <button className="primary" disabled={creating}>{creating ? 'Đang tạo...' : 'Tạo tài khoản'}</button>
    </form>
    <div className="member-toolbar">
      <label className="member-search"><span>⌕</span><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm theo tên hoặc email..." /></label>
      <div className="member-filters"><button className={filter === 'all' ? 'active' : ''} onClick={() => setFilter('all')}>Tất cả</button><button className={filter === 'active' ? 'active' : ''} onClick={() => setFilter('active')}>Hoạt động</button><button className={filter === 'inactive' ? 'active' : ''} onClick={() => setFilter('inactive')}>Đã khóa</button></div>
    </div>
    <div className="member-admin-list">
      {filtered.length === 0 && <p className="empty">Không tìm thấy thành viên phù hợp.</p>}
      {filtered.map((member) => <MemberRow key={member.id} member={member} isCurrent={member.id === currentUser.id} onSaved={onSaved} flash={flash} />)}
    </div>
  </section>;
}

function MemberRow({ member, isCurrent, onSaved, flash }) {
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(memberForm(member));
  useEffect(() => setForm(memberForm(member)), [member]);
  const set = (field) => (event) => setForm({ ...form, [field]: event.target.type === 'checkbox' ? event.target.checked : event.target.value });
  const save = async (event) => {
    event.preventDefault();
    try { await teaApi.updateMember(member.id, form); flash('Đã cập nhật thành viên.'); setOpen(false); await onSaved(); }
    catch (error) { flash(error.message || 'Không thể cập nhật thành viên.'); }
  };
  const remove = async () => {
    if (isCurrent) return flash('Bạn không thể xóa chính tài khoản đang đăng nhập.');
    if (!window.confirm(`Bạn có chắc muốn XÓA VĨNH VIỄN tài khoản của ${member.displayName} không? Điểm danh, chat và dữ liệu liên quan của thành viên này cũng sẽ bị xóa và không thể khôi phục.`)) return;
    try { await teaApi.deleteMember(member.id); flash('Đã xóa vĩnh viễn thành viên khỏi hệ thống.'); await onSaved(); }
    catch (error) { flash(error.message || 'Không thể xóa thành viên.'); }
  };
  return <article className={`member-admin-card ${!member.active ? 'inactive' : ''} ${open ? 'editing' : ''}`}>
    <div className="member-card-main">
      <MemberAvatar member={member} />
      <div className="member-identity"><div><strong>{member.displayName}</strong>{isCurrent && <span className="you-badge">Bạn</span>}</div><small>{member.email || 'Chưa có email'}</small></div>
      <div className="member-badges"><span className={`account-status ${member.active ? 'active' : 'locked'}`}><i />{member.active ? 'Hoạt động' : 'Đã khóa'}</span><span className={member.role === 'ADMIN' ? 'role admin' : 'role'}>{member.role === 'ADMIN' ? 'Quản trị viên' : 'Thành viên'}</span></div>
      <button type="button" className={`member-edit-toggle ${open ? 'active' : ''}`} onClick={() => setOpen(!open)}>{open ? 'Thu gọn ↑' : 'Chỉnh sửa ✎'}</button>
    </div>
    <div className="member-bank-preview">
      <span><small>Ngân hàng</small><b>{member.bankName || 'Chưa cập nhật'}</b></span>
      <span><small>Số tài khoản</small><b>{member.accountNumber || '—'}</b></span>
      <span><small>Chủ tài khoản</small><b>{member.accountName || '—'}</b></span>
    </div>
    {open && <form className="member-edit-form" onSubmit={save}>
      <div className="member-form-heading"><strong>Chỉnh sửa thông tin</strong><small>Các thay đổi có hiệu lực ngay sau khi lưu.</small></div>
      <label>Tên hiển thị<input required value={form.displayName} onChange={set('displayName')} /></label>
      <label>Email<input type="email" value={form.email} onChange={set('email')} /></label>
      <label>Vai trò<select value={form.role} onChange={set('role')}><option value="MEMBER">Thành viên</option><option value="ADMIN">Admin</option></select></label>
      <BankSelect value={form.bankName} bankBin={form.bankBin} onChange={({ bankName, bankBin }) => setForm({ ...form, bankName, bankBin })} />
      <label>Số tài khoản<input value={form.accountNumber} onChange={set('accountNumber')} /></label>
      <label>Chủ tài khoản<input value={form.accountName} onChange={set('accountName')} /></label>
      <label className="member-active-switch"><input type="checkbox" checked={form.active} onChange={set('active')} /><span /><b>Tài khoản hoạt động</b></label>
      <div className="form-actions"><button className="primary">Lưu thay đổi</button><button type="button" className="danger" onClick={remove}>Xóa vĩnh viễn tài khoản</button></div>
    </form>}
  </article>;
}

function AdminPasswordInput(props) {
  const [visible, setVisible] = useState(false);
  return <div className="password-field"><input {...props} type={visible ? 'text' : 'password'} autoComplete="new-password" /><button type="button" onClick={() => setVisible(!visible)} aria-label={visible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}>{visible ? '🙈' : '👁️'}</button></div>;
}

function MemberAvatar({ member }) {
  if (member.avatarUrl) return <img className="member-card-avatar" src={apiMediaUrl(member.avatarUrl)} alt={member.displayName} />;
  return <span className="member-card-avatar fallback">{initials(member.displayName)}</span>;
}

function AttendanceSettings({ flash }) {
  const [form, setForm] = useState(null);
  const [saving, setSaving] = useState(false);
  useEffect(() => {
    teaApi.getAttendanceSettings().then((data) => setForm({ ...data, cutoffTime: String(data.cutoffTime).slice(0, 5) }))
      .catch((error) => flash(error.message || 'Không tải được giờ khóa điểm danh.'));
  }, []);
  if (!form) return <section className="panel"><p className="loading">Đang tải giờ khóa...</p></section>;
  const save = async (event) => {
    event.preventDefault(); setSaving(true);
    try {
      const result = await teaApi.updateAttendanceSettings({ cutoffTime: form.cutoffTime });
      setForm({ ...result, cutoffTime: String(result.cutoffTime).slice(0, 5) });
      flash('Đã cập nhật giờ khóa điểm danh mỗi ngày.');
    } catch (error) { flash(error.message || 'Không lưu được giờ khóa điểm danh.'); }
    finally { setSaving(false); }
  };
  return <section className="panel attendance-settings-panel">
    <div className="panel-title"><span>⏱️</span><div><h2>Giờ khóa điểm danh</h2><p>Sau giờ này thành viên không thể tạo, sửa hoặc hủy điểm danh trong ngày. Admin vẫn có thể điều chỉnh khi cần.</p></div></div>
    <form className="attendance-settings-form" onSubmit={save}>
      <label>Khóa điểm danh lúc<input type="time" required value={form.cutoffTime} onChange={(event) => setForm({ ...form, cutoffTime: event.target.value })} /></label>
      <div className={`cutoff-preview ${form.locked ? 'locked' : ''}`}><span>{form.locked ? '🔒' : '✅'}</span><div><strong>{form.locked ? 'Hôm nay đã qua giờ khóa' : 'Hôm nay vẫn đang mở'}</strong><small>Giờ máy chủ: {String(form.serverTime).slice(0, 5)} · Ngày {form.serverDate}</small></div></div>
      <button className="primary" disabled={saving}>{saving ? 'Đang lưu...' : 'Lưu giờ khóa'}</button>
    </form>
  </section>;
}

function NotificationSettings({ flash }) {
  const [form, setForm] = useState(null);
  const [saving, setSaving] = useState(false);
  useEffect(() => {
    teaApi.getWeeklyNotificationSettings().then((data) => setForm({ ...data, sendTime: String(data.sendTime).slice(0, 5) }))
      .catch((error) => flash(error.message || 'Không tải được cấu hình thông báo.'));
  }, []);
  if (!form) return <section className="panel"><p className="loading">Đang tải cấu hình thông báo...</p></section>;
  const set = (field) => (event) => setForm({ ...form, [field]: event.target.value });
  const save = async (event) => {
    event.preventDefault(); setSaving(true);
    try {
      const updated = await teaApi.updateWeeklyNotificationSettings({ ...form, sendDay: Number(form.sendDay), sendTime: form.sendTime });
      setForm({ ...updated, sendTime: String(updated.sendTime).slice(0, 5) });
      flash('Đã lưu lịch và nội dung thông báo tuần.');
    } catch (error) { flash(error.message || 'Không thể lưu cấu hình.'); }
    finally { setSaving(false); }
  };
  return <section className="panel notification-settings">
    <div className="panel-title"><span>🔔</span><div><h2>Thông báo thanh toán hàng tuần</h2><p>Hệ thống tự chốt tuần và gửi thông báo riêng trong mục Chat → Hệ thống.</p></div></div>
    <form className="settings-form" onSubmit={save}>
      <div className="schedule-fields"><label>Ngày gửi<select value={form.sendDay} onChange={set('sendDay')}>{dayOptions.map((day) => <option key={day.value} value={day.value}>{day.label}</option>)}</select></label><label>Giờ gửi<input type="time" required value={form.sendTime} onChange={set('sendTime')} /></label></div>
      <fieldset><legend>Thông báo cho người cần trả tiền</legend><label>Tiêu đề<input required maxLength="200" value={form.debtorTitle} onChange={set('debtorTitle')} /></label><label>Nội dung<textarea required rows="4" maxLength="1000" value={form.debtorBody} onChange={set('debtorBody')} /></label></fieldset>
      <fieldset><legend>Thông báo cho người đã trả trước</legend><label>Tiêu đề<input required maxLength="200" value={form.creditorTitle} onChange={set('creditorTitle')} /></label><label>Nội dung<textarea required rows="4" maxLength="1000" value={form.creditorBody} onChange={set('creditorBody')} /></label></fieldset>
      <p className="template-help">Biến dùng được: <code>{'{amount}'}</code> số tiền, <code>{'{creditor}'}</code> người nhận, <code>{'{weekStart}'}</code> ngày đầu tuần. Hệ thống luôn tự thêm mã người nhận; với người đã ứng tiền, hệ thống tự thêm danh sách từng thành viên và số tiền phải hoàn.</p>
      <button className="primary" disabled={saving}>{saving ? 'Đang lưu...' : 'Lưu cấu hình thông báo'}</button>
    </form>
  </section>;
}

const dayOptions = [
  { value: 1, label: 'Thứ Hai' }, { value: 2, label: 'Thứ Ba' }, { value: 3, label: 'Thứ Tư' },
  { value: 4, label: 'Thứ Năm' }, { value: 5, label: 'Thứ Sáu' }, { value: 6, label: 'Thứ Bảy' },
  { value: 7, label: 'Chủ Nhật' },
];

function drinkForm(drink) {
  return { name: drink.name, price: drink.price, icon: drink.icon || '🥤', active: drink.active };
}

function memberForm(member) {
  return { displayName: member.displayName || '', email: member.email || '', bankName: member.bankName || '', bankBin: member.bankBin || '', accountNumber: member.accountNumber || '', accountName: member.accountName || '', qrCodeUrl: member.qrCodeUrl || '', avatarUrl: member.avatarUrl || '', active: member.active, role: member.role || 'MEMBER' };
}

function initials(name) {
  return String(name || '?').split(' ').filter(Boolean).slice(-2).map((part) => part[0]).join('').toUpperCase();
}
