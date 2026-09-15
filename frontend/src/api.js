export const API_BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8081/api';

export async function api(path, options = {}) {
  const isFormData = options.body instanceof FormData;
  const headers = { ...(options.headers ?? {}) };
  const token = window.localStorage.getItem('tea-session-token');
  if (token) headers.Authorization = `Bearer ${token}`;
  if (!isFormData && !headers['Content-Type']) headers['Content-Type'] = 'application/json';

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });
  } catch {
    throw new Error('Không thể kết nối tới máy chủ. Hãy kiểm tra backend đang chạy rồi thử lại.');
  }
  if (!response.ok) {
    const raw = await response.text().catch(() => '');
    let body = null;
    try {
      body = JSON.parse(raw);
    } catch {
      // Nội dung không phải JSON sẽ được chuyển thành thông báo dễ hiểu bên dưới.
    }
    const fieldDetails = body?.fieldErrors && typeof body.fieldErrors === 'object'
      ? Object.values(body.fieldErrors).filter(Boolean).join(' · ')
      : '';
    const safeText = raw && !raw.trim().startsWith('<') && !raw.trim().startsWith('{') && !raw.trim().startsWith('[')
      ? raw.trim()
      : '';
    const statusMessages = {
      400: 'Thông tin gửi lên chưa hợp lệ.',
      401: 'Phiên đăng nhập không hợp lệ. Bạn hãy đăng nhập lại.',
      403: 'Bạn không có quyền thực hiện thao tác này.',
      404: 'Không tìm thấy dữ liệu cần thao tác.',
      409: 'Thao tác không thể thực hiện do dữ liệu đang được sử dụng.',
      413: 'Ảnh tải lên vượt quá dung lượng cho phép.',
      500: 'Máy chủ gặp lỗi khi xử lý. Bạn hãy thử lại sau.',
    };
    const message = [body?.message || body?.error || safeText || statusMessages[response.status] || `Yêu cầu thất bại (mã ${response.status}).`, fieldDetails]
      .filter(Boolean).join(' Chi tiết: ');
    const error = new Error(message);
    error.status = response.status;
    error.fieldErrors = body?.fieldErrors || {};
    throw error;
  }
  if (response.status === 204) return null;
  return response.json();
}

export function apiMediaUrl(url) {
  if (!url || /^(https?:|blob:|data:)/.test(url)) return url;
  const apiOrigin = API_BASE_URL.replace(/\/api\/?$/, '');
  return `${apiOrigin}${url.startsWith('/') ? url : `/${url}`}`;
}

export const teaApi = {
  login: (payload) => api('/auth/login', { method: 'POST', body: JSON.stringify(payload) }),
  forgotPassword: (payload) => api('/auth/forgot-password', { method: 'POST', body: JSON.stringify(payload) }),
  resetPassword: (payload) => api('/auth/reset-password', { method: 'POST', body: JSON.stringify(payload) }),

  getMembers: (includeInactive = false) => api(`/members${includeInactive ? '?includeInactive=true' : ''}`),
  getMe: () => api('/members/me'),
  updateMe: (payload) => api('/members/me', { method: 'PUT', body: JSON.stringify(payload) }),
  uploadAvatar: (image) => {
    const formData = new FormData();
    formData.append('image', image);
    return api('/members/me/avatar', { method: 'POST', body: formData });
  },
  createMember: (payload) => api('/members', { method: 'POST', body: JSON.stringify(payload) }),
  updateMember: (id, payload) => api(`/members/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  deleteMember: (id) => api(`/members/${id}`, { method: 'DELETE' }),

  getDrinks: (includeInactive = false) => api(`/drinks${includeInactive ? '?includeInactive=true' : ''}`),
  createDrink: (payload) => api('/drinks', { method: 'POST', body: JSON.stringify(payload) }),
  updateDrink: (id, payload) => api(`/drinks/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  deleteDrink: (id) => api(`/drinks/${id}`, { method: 'DELETE' }),

  getToday: (date) => api(`/attendances/daily-summary${date ? `?date=${date}` : ''}`),
  getExpensesRange: (from, to) => api(`/expenses/range?from=${from}&to=${to}`),
  createExpense: (payload) => api('/expenses', { method: 'POST', body: JSON.stringify(payload) }),
  updateExpense: (expenseId, payload) => api(`/expenses/${expenseId}`, { method: 'PUT', body: JSON.stringify(payload) }),
  createAttendance: (payload) => api('/attendances', { method: 'POST', body: JSON.stringify(payload) }),
  deleteQuickAttendance: (expenseId) => api(`/expenses/${expenseId}`, { method: 'DELETE' }),
  getAttendanceSettings: () => api('/attendance-settings'),
  updateAttendanceSettings: (payload) => api('/attendance-settings', { method: 'PUT', body: JSON.stringify(payload) }),
  getFavoriteDrinks: () => api('/members/me/favorite-drinks'),
  addFavoriteDrink: (drinkId) => api(`/members/me/favorite-drinks/${drinkId}`, { method: 'PUT' }),
  removeFavoriteDrink: (drinkId) => api(`/members/me/favorite-drinks/${drinkId}`, { method: 'DELETE' }),

  getWeekSettlement: (weekStart) => api(`/weeks/${weekStart}/settlement`),
  calculateWeekSettlement: (weekStart) => api(`/weeks/${weekStart}/settlement/calculate`, { method: 'POST' }),
  finalizeWeekSettlement: (weekStart) => api(`/weeks/${weekStart}/settlement/finalize`, { method: 'POST' }),
  getPaymentQr: (transferId) => api(`/settlement-transfers/${transferId}/payment-qr`),
  markTransferPaid: (transferId) => api(`/settlement-transfers/${transferId}/mark-paid`, { method: 'POST' }),
  getNotifications: () => api('/notifications'),
  markNotificationRead: (notificationId) => api(`/notifications/${notificationId}/read`, { method: 'PATCH' }),
  deleteNotification: (notificationId) => api(`/notifications/${notificationId}`, { method: 'DELETE' }),

  getMessages: ({ before, limit = 50 } = {}) => {
    const parameters = new URLSearchParams({ limit: String(limit) });
    if (before) parameters.set('before', before);
    return api(`/chat/messages?${parameters}`);
  },
  searchMessages: (query, { before, limit = 50 } = {}) => {
    const parameters = new URLSearchParams({ query, limit: String(limit) });
    if (before) parameters.set('before', before);
    return api(`/chat/messages/search?${parameters}`);
  },
  getChatUnread: () => api('/chat/messages/unread-count'),
  markChatRead: () => api('/chat/messages/read', { method: 'PATCH' }),
  sendMessage: (payload) => api('/chat/messages', { method: 'POST', body: JSON.stringify(payload) }),
  sendMessageWithMedia: ({ senderMemberId, content, replyToMessageId, images }) => {
    const formData = new FormData();
    formData.append('senderMemberId', String(senderMemberId));
    if (content) formData.append('content', content);
    if (replyToMessageId) formData.append('replyToMessageId', String(replyToMessageId));
    images.forEach((image) => formData.append('images', image));
    return api('/chat/messages/with-media', { method: 'POST', body: formData });
  },
  deleteMessage: (messageId) => api(`/chat/messages/${messageId}`, { method: 'DELETE' }),
  clearGroupChat: () => api('/chat/messages', { method: 'DELETE' }),
  getTypingMembers: () => api('/chat/typing'),
  setTyping: (typing) => api('/chat/typing', { method: 'PUT', body: JSON.stringify({ typing }) }),
  toggleReaction: (messageId, emoji) => api(`/chat/messages/${messageId}/reactions`, { method: 'POST', body: JSON.stringify({ emoji }) }),

  getWeeklyNotificationSettings: () => api('/admin/weekly-notification-settings'),
  updateWeeklyNotificationSettings: (payload) => api('/admin/weekly-notification-settings', {
    method: 'PUT', body: JSON.stringify(payload),
  }),
};
