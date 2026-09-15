import { createElement, useEffect, useState } from 'react';
import { apiMediaUrl, teaApi } from './api';
import BankSelect from './BankSelect';

const React = { createElement };

export default function ProfileView({ user, onUserUpdated, flash }) {
  const [form, setForm] = useState(profileForm(user));
  const [avatarFile, setAvatarFile] = useState(null);
  const [preview, setPreview] = useState(user.avatarUrl ? apiMediaUrl(user.avatarUrl) : '');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setForm(profileForm(user));
    if (!avatarFile) setPreview(user.avatarUrl ? apiMediaUrl(user.avatarUrl) : '');
  }, [user]);

  useEffect(() => () => {
    if (preview?.startsWith('blob:')) URL.revokeObjectURL(preview);
  }, [preview]);

  const update = (field) => (event) => setForm({ ...form, [field]: event.target.value });
  const chooseAvatar = (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) return flash('Ảnh đại diện tối đa 5 MB.');
    setAvatarFile(file);
    setPreview(URL.createObjectURL(file));
  };
  const save = async (event) => {
    event.preventDefault(); setSaving(true);
    try {
      let next = await teaApi.updateMe({ ...form, qrCodeUrl: user.qrCodeUrl || '', avatarUrl: user.avatarUrl || '', active: true, role: user.role });
      if (avatarFile) next = await teaApi.uploadAvatar(avatarFile);
      setAvatarFile(null); onUserUpdated(next);
      setPreview(next.avatarUrl ? apiMediaUrl(next.avatarUrl) : '');
      flash('Đã cập nhật hồ sơ, ảnh đại diện và tài khoản ngân hàng.');
    } catch (error) { flash(error.message || 'Không thể cập nhật hồ sơ.'); }
    finally { setSaving(false); }
  };

  return <section className="panel profile-panel">
    <div className="panel-title">
      {preview ? <img className="profile-avatar" src={preview} alt="Ảnh đại diện" /> : <span className="avatar profile-avatar-fallback">{initials(user.displayName)}</span>}
      <div><h2>Hồ sơ thành viên</h2><p>Tự cập nhật ảnh, tên và thông tin ngân hàng.</p></div>
    </div>
    <form className="stack-form profile-form" onSubmit={save}>
      <label className="avatar-upload">Ảnh đại diện
        <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" onChange={chooseAvatar} />
        <small>Chọn ảnh JPG, PNG, GIF hoặc WEBP từ máy, tối đa 5 MB.</small>
      </label>
      <label>Tên hiển thị<input required value={form.displayName} onChange={update('displayName')} /></label>
      <label>Email<input type="email" value={form.email} disabled /><small>Email đăng nhập không thể tự thay đổi.</small></label>
      <div className="form-divider"><span>Thông tin nhận chuyển khoản</span></div>
      <BankSelect value={form.bankName} bankBin={form.bankBin} onChange={({ bankName, bankBin }) => setForm({ ...form, bankName, bankBin })} />
      <label>Số tài khoản<input value={form.accountNumber} onChange={update('accountNumber')} /></label>
      <label>Chủ tài khoản<input value={form.accountName} onChange={update('accountName')} /></label>
      <button className="primary" disabled={saving}>{saving ? 'Đang lưu...' : 'Lưu thay đổi'}</button>
    </form>
  </section>;
}

function profileForm(user) {
  return { displayName: user.displayName || '', email: user.email || '', bankName: user.bankName || '', bankBin: user.bankBin || '', accountNumber: user.accountNumber || '', accountName: user.accountName || '' };
}

function initials(name) {
  return String(name || '?').split(' ').filter(Boolean).slice(-2).map((part) => part[0]).join('').toUpperCase();
}
