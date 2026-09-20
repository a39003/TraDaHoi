import { createElement, Fragment, useEffect, useMemo, useState } from 'react';
import { apiMediaUrl, teaApi } from './api';

const React = { createElement, Fragment };
const money = (value) => new Intl.NumberFormat('vi-VN').format(Number(value || 0)) + ' đ';
const today = () => {
  const date = new Date();
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
};
const key = () => `${Date.now()}-${Math.random().toString(36).slice(2)}`;
const weekdayLabel = () => {
  const value = new Intl.DateTimeFormat('vi-VN', { weekday: 'long' }).format(new Date());
  return value.charAt(0).toUpperCase() + value.slice(1);
};

export default function AdvancedAttendanceView({ user, members, drinks, todayExpenses, isAdmin, onSaved, flash }) {
  const [mode, setMode] = useState('PERSONAL');
  const [lines, setLines] = useState([]);
  const [payerId, setPayerId] = useState(String(user.id));
  const [sharedMembers, setSharedMembers] = useState([user.id]);
  const [favorites, setFavorites] = useState([]);
  const [settings, setSettings] = useState(null);
  const [editingId, setEditingId] = useState(null);
  const [saving, setSaving] = useState(false);

  const newLine = (consumerId = user.id, drinkId = drinks[0]?.id) => ({ key: key(), drinkId: String(drinkId || ''), quantity: 1, consumerId: String(consumerId) });
  useEffect(() => {
    if (!lines.length && drinks.length) setLines([newLine()]);
  }, [drinks.length]);
  useEffect(() => {
    Promise.all([teaApi.getFavoriteDrinks(), teaApi.getAttendanceSettings()])
      .then(([favoriteData, settingData]) => { setFavorites(favoriteData.map((item) => item.id)); setSettings(settingData); })
      .catch((error) => flash(error.message || 'Không tải được cấu hình điểm danh.'));
  }, []);

  const orderedDrinks = useMemo(() => [...drinks].sort((a, b) => Number(favorites.includes(b.id)) - Number(favorites.includes(a.id)) || a.name.localeCompare(b.name, 'vi')), [drinks, favorites]);
  const selectedTotal = lines.reduce((sum, line) => {
    const drink = drinks.find((item) => String(item.id) === line.drinkId);
    return sum + (drink?.price || 0) * Math.max(1, Number(line.quantity || 1));
  }, 0);
  const locked = Boolean(settings?.locked && !isAdmin);

  const updateLine = (lineKey, field, value) => setLines((current) => current.map((line) => line.key === lineKey ? { ...line, [field]: value } : line));
  const addLine = () => setLines((current) => [...current, newLine(mode === 'PERSONAL' ? user.id : members[0]?.id)]);
  const removeLine = (lineKey) => setLines((current) => current.length === 1 ? current : current.filter((line) => line.key !== lineKey));
  const toggleSharedMember = (id) => setSharedMembers((current) => current.includes(id) ? current.filter((value) => value !== id) : [...current, id]);

  const changeMode = (next) => {
    setMode(next);
    setLines((current) => current.map((line) => ({ ...line, consumerId: String(user.id) })));
    if (next === 'EVEN' && sharedMembers.length === 0) setSharedMembers([user.id]);
  };

  const toggleFavorite = async (drinkId) => {
    try {
      if (favorites.includes(drinkId)) {
        await teaApi.removeFavoriteDrink(drinkId);
        setFavorites((current) => current.filter((id) => id !== drinkId));
      } else {
        await teaApi.addFavoriteDrink(drinkId);
        setFavorites((current) => [...current, drinkId]);
      }
    } catch (error) { flash(error.message || 'Không cập nhật được đồ uống yêu thích.'); }
  };

  const buildItems = () => {
    if (mode === 'EVEN') {
      if (!sharedMembers.length) throw new Error('Hãy chọn ít nhất một người cùng uống.');
      return lines.flatMap((line) => {
        const drink = drinks.find((item) => String(item.id) === line.drinkId);
        if (!drink) throw new Error('Có món chưa chọn đồ uống.');
        const lineTotal = drink.price * Math.max(1, Number(line.quantity));
        const base = Math.floor(lineTotal / sharedMembers.length);
        const remainder = lineTotal - base * sharedMembers.length;
        return sharedMembers.map((memberId, index) => ({
          consumerMemberId: memberId, drinkName: drink.name,
          unitPrice: base + (index === 0 ? remainder : 0), quantity: 1,
        }));
      });
    }
    return lines.map((line) => {
      const drink = drinks.find((item) => String(item.id) === line.drinkId);
      if (!drink) throw new Error('Có món chưa chọn đồ uống.');
      return {
        consumerMemberId: Number(line.consumerId || user.id),
        drinkName: drink.name, unitPrice: drink.price, quantity: Math.max(1, Number(line.quantity)),
      };
    });
  };

  const reset = () => {
    setEditingId(null); setMode('PERSONAL'); setPayerId(String(user.id)); setSharedMembers([user.id]);
    setLines(drinks.length ? [newLine()] : []);
  };

  const save = async (event) => {
    event.preventDefault();
    if (locked) return flash(`Điểm danh đã khóa lúc ${String(settings.cutoffTime).slice(0, 5)}.`);
    setSaving(true);
    try {
      const items = buildItems();
      const payload = {
        orderDate: today(), payerMemberId: Number(payerId), splitMode: mode === 'EVEN' ? 'EVEN' : 'ITEMIZED',
        note: mode === 'PERSONAL' ? 'Điểm danh cá nhân' : mode === 'EVEN' ? `Đơn uống chung chia đều ${sharedMembers.length} người` : 'Đơn uống chung chia theo từng món',
        items,
      };
      const savedExpense = editingId
        ? await teaApi.updateExpense(editingId, payload)
        : await teaApi.createExpense(payload);
      flash(editingId ? 'Đã cập nhật điểm danh.' : 'Đã lưu điểm danh.');
      reset(); onSaved({ type: 'upsert', expense: savedExpense });
      const latest = await teaApi.getAttendanceSettings(); setSettings(latest);
    } catch (error) { flash(error.message || 'Không thể lưu điểm danh.'); }
    finally { setSaving(false); }
  };

  const edit = (expense) => {
    setEditingId(expense.id); setPayerId(String(expense.payer.id));
    if (expense.splitMode === 'EVEN') {
      setMode('EVEN');
      setSharedMembers([...new Set(expense.items.map((item) => item.consumer.id))]);
      const grouped = Object.values(expense.items.reduce((all, item) => {
        all[item.drinkName] ||= { name: item.drinkName, total: 0 };
        all[item.drinkName].total += item.lineTotal;
        return all;
      }, {}));
      setLines(grouped.map((group) => {
        const drink = drinks.find((item) => item.name === group.name);
        return { key: key(), drinkId: String(drink?.id || ''), quantity: Math.max(1, Math.round(group.total / Math.max(1, drink?.price || group.total))), consumerId: String(user.id) };
      }));
    } else {
      setMode('PERSONAL');
      setLines(expense.items.map((item) => ({ key: key(), drinkId: String(drinks.find((drink) => drink.name === item.drinkName)?.id || ''), quantity: item.quantity, consumerId: String(item.consumer.id) })));
    }
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const cancelAttendance = async (expense) => {
    if (!window.confirm('Bạn có chắc muốn hủy điểm danh này không? Dữ liệu sẽ bị xóa khỏi hệ thống.')) return;
    try { await teaApi.deleteQuickAttendance(expense.id); if (editingId === expense.id) reset(); flash('Đã hủy điểm danh.'); onSaved({ type: 'delete', id: expense.id }); }
    catch (error) { flash(error.message || 'Không thể hủy điểm danh.'); }
  };

  return <div className="advanced-attendance">
    <section className="panel attendance-form advanced-attendance-form">
      <div className="panel-title"><span>📝</span><div><h2>{editingId ? `Sửa điểm danh ${weekdayLabel()}` : `Điểm danh ${weekdayLabel()}`}</h2><p>Chọn một hoặc nhiều đồ uống, sau đó chọn người đã trả tiền.</p></div></div>
      {settings && <div className={`cutoff-banner ${settings.locked ? 'locked' : ''}`}><span>{settings.locked ? '🔒' : '⏱️'}</span><div><strong>{settings.locked && !isAdmin ? 'Đã khóa điểm danh' : `Giờ khóa: ${String(settings.cutoffTime).slice(0, 5)}`}</strong><small>{isAdmin && settings.locked ? 'Admin vẫn có thể điều chỉnh.' : 'Có thể sửa hoặc hủy trước giờ khóa.'}</small></div></div>}
      <div className="split-mode-tabs">
        <button type="button" className={mode === 'PERSONAL' ? 'active' : ''} onClick={() => changeMode('PERSONAL')}>Cá nhân</button>
        <button type="button" className={mode === 'EVEN' ? 'active' : ''} onClick={() => changeMode('EVEN')}>Chia đều</button>
      </div>
      {favorites.length > 0 && <div className="favorite-shortcuts"><span>Chọn nhanh:</span>{orderedDrinks.filter((drink) => favorites.includes(drink.id)).map((drink) => <button type="button" key={drink.id} onClick={() => setLines((current) => [...current, newLine(user.id, drink.id)])}>{drink.icon || '🥤'} {drink.name}</button>)}</div>}
      <form onSubmit={save} className="stack-form">
        <div className="drink-line-list">{lines.map((line, index) => <div className="drink-line" key={line.key}>
          <button type="button" className={`favorite-toggle ${favorites.includes(Number(line.drinkId)) ? 'active' : ''}`} onClick={() => line.drinkId && toggleFavorite(Number(line.drinkId))} title="Đồ uống yêu thích">{favorites.includes(Number(line.drinkId)) ? '★' : '☆'}</button>
          <label><span>Món {index + 1}</span><select required value={line.drinkId} onChange={(event) => updateLine(line.key, 'drinkId', event.target.value)}><option value="">Chọn đồ uống</option>{orderedDrinks.map((drink) => <option key={drink.id} value={drink.id}>{favorites.includes(drink.id) ? '★ ' : ''}{drink.icon || '🥤'} {drink.name} — {money(drink.price)}</option>)}</select></label>
          <label className="quantity-field"><span>Số lượng</span><input type="number" min="1" max="20" value={line.quantity} onChange={(event) => updateLine(line.key, 'quantity', event.target.value)} /></label>
          <button type="button" className="remove-line" disabled={lines.length === 1} onClick={() => removeLine(line.key)} aria-label="Bỏ món">×</button>
        </div>)}</div>
        <button type="button" className="outline add-drink-line" onClick={addLine}>＋ Thêm đồ uống</button>
        {mode === 'EVEN' && <fieldset><legend>Chia đều cho những ai?</legend><div className="member-checks">{members.map((member) => <label key={member.id} className="check-row"><input type="checkbox" checked={sharedMembers.includes(member.id)} onChange={() => toggleSharedMember(member.id)} />{member.displayName}</label>)}</div><p className="split-note">Tổng {money(selectedTotal)} · Mỗi người khoảng {money(Math.floor(selectedTotal / Math.max(1, sharedMembers.length)))}</p></fieldset>}
        <label>Người đã trả tiền<select value={payerId} onChange={(event) => setPayerId(event.target.value)}>{members.map((member) => <option key={member.id} value={member.id}>{member.displayName}</option>)}</select></label>
        <div className="attendance-total"><span>Tổng đơn</span><strong>{money(selectedTotal)}</strong></div>
        <div className="attendance-actions"><button className="primary" disabled={saving || locked || !lines.length}>{saving ? 'Đang lưu...' : editingId ? 'Lưu thay đổi' : 'Xác nhận điểm danh'}</button>{editingId && <button type="button" className="outline" onClick={reset}>Hủy sửa</button>}</div>
      </form>
    </section>

    <section className="panel today-orders"><div className="panel-title"><span>☀️</span><div><h2>{isAdmin ? 'Chi tiết hôm nay' : 'Điểm danh của bạn hôm nay'}</h2><p>{todayExpenses.length} đơn đã ghi nhận</p></div></div>
      {todayExpenses.length === 0 ? <p className="empty">Chưa có lượt điểm danh.</p> : <div className="today-order-list">{todayExpenses.map((expense, expenseIndex) => {
        const canChange = isAdmin || expense.createdBy?.id === user.id;
        const canEditShape = expense.splitMode === 'EVEN' || new Set(expense.items.map((item) => item.consumer.id)).size === 1;
        const visibleItems = isAdmin ? expense.items : expense.items.filter((item) => item.consumer.id === user.id);
        return <article className="today-order" key={expense.id}>
          <div className="today-order-head"><div><strong>Điểm danh lần {expenseIndex + 1} · {weekdayLabel()}</strong><span>{expense.splitMode === 'EVEN' ? 'Uống chung chia đều' : expense.items.length > 1 ? 'Nhiều đồ uống' : 'Cá nhân'} · {expense.payer.displayName} trả</span></div><b>{money(visibleItems.reduce((sum, item) => sum + item.lineTotal, 0))}</b></div>
          <div className="today-order-items">{visibleItems.map((item) => <div key={item.id}><Avatar member={item.consumer} /><span><strong>{item.consumer.displayName}</strong><small>{item.drinkName} × {item.quantity}</small></span><b>{money(item.lineTotal)}</b></div>)}</div>
          {canChange && (!locked || isAdmin) && <div className="today-order-actions">{canEditShape && <button type="button" className="outline small" onClick={() => edit(expense)}>✎ Sửa</button>}<button type="button" className="danger small" onClick={() => cancelAttendance(expense)}>Hủy điểm danh</button></div>}
        </article>;
      })}</div>}
    </section>
  </div>;
}

function Avatar({ member }) {
  if (member?.avatarUrl) return <img className="avatar avatar-image" src={apiMediaUrl(member.avatarUrl)} alt={member.displayName} />;
  const letters = String(member?.displayName || '?').split(' ').filter(Boolean).slice(-2).map((part) => part[0]).join('').toUpperCase();
  return <span className="avatar">{letters}</span>;
}
